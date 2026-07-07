package dev.ratmir.cloudstorage.storage.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import dev.ratmir.cloudstorage.api.FeatureNotImplementedException;
import dev.ratmir.cloudstorage.auth.service.CurrentUserProvider;
import dev.ratmir.cloudstorage.storage.ResourcePath;
import dev.ratmir.cloudstorage.storage.StorageOperationException;
import dev.ratmir.cloudstorage.storage.StorageProperties;
import dev.ratmir.cloudstorage.storage.api.ResourceResponse;
import dev.ratmir.cloudstorage.storage.api.ResourceType;
import io.minio.CopyObjectArgs;
import io.minio.GetObjectArgs;
import io.minio.ListObjectsArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.Result;
import io.minio.SourceObject;
import io.minio.StatObjectArgs;
import io.minio.StatObjectResponse;
import io.minio.errors.ErrorResponseException;
import io.minio.messages.Item;

import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
class MinioResourceService implements ResourceService, DirectoryService {

	private final MinioClient minioClient;
	private final StorageProperties properties;
	private final CurrentUserProvider currentUserProvider;

	MinioResourceService(
			MinioClient minioClient,
			StorageProperties properties,
			CurrentUserProvider currentUserProvider) {
		this.minioClient = minioClient;
		this.properties = properties;
		this.currentUserProvider = currentUserProvider;
	}

	@Override
	public ResourceResponse get(String path) {
		var resource = ResourcePath.resource(path);
		var storage = currentStorage();

		if (resource.directory()) {
			if (!directoryExists(storage, resource.value())) {
				throw notFound("Resource not found");
			}
			return toResponse(resource.value(), 0L, true);
		}

		var stat = statObject(storage.absolute(resource.value()))
				.orElseThrow(() -> notFound("Resource not found"));
		return toResponse(resource.value(), stat.size(), false);
	}

	@Override
	public void delete(String path) {
		var resource = ResourcePath.resource(path);
		var storage = currentStorage();

		if (resource.directory()) {
			if (!directoryExists(storage, resource.value())) {
				throw notFound("Resource not found");
			}
			removeDirectory(storage, resource.value());
			return;
		}

		var objectName = storage.absolute(resource.value());
		if (statObject(objectName).isEmpty()) {
			throw notFound("Resource not found");
		}
		removeObject(objectName);
	}

	@Override
	public DownloadedResource download(String path) {
		var resource = ResourcePath.resource(path);
		if (resource.directory()) {
			throw new FeatureNotImplementedException("Directory download as zip is not implemented yet");
		}

		var storage = currentStorage();
		var objectName = storage.absolute(resource.value());
		var stat = statObject(objectName).orElseThrow(() -> notFound("Resource not found"));
		try {
			var response = minioClient.getObject(GetObjectArgs.builder()
					.bucket(properties.bucket())
					.object(objectName)
					.build());
			return new DownloadedResource(
					resourceName(resource.value()),
					MediaType.APPLICATION_OCTET_STREAM,
					new InputStreamResource(response),
					stat.size());
		}
		catch (Exception exception) {
			if (isNotFound(exception)) {
				throw notFound("Resource not found");
			}
			throw new StorageOperationException("Failed to download resource", exception);
		}
	}

	@Override
	public ResourceResponse move(String from, String to) {
		var source = ResourcePath.resource(from);
		var storage = currentStorage();
		if (source.directory()) {
			var target = ResourcePath.directory(to, false);
			return moveDirectory(storage, source, target);
		}
		var target = ResourcePath.file(to);
		return moveFile(storage, source, target);
	}

	@Override
	public List<ResourceResponse> search(String query) {
		if (query == null || query.isBlank()) {
			throw new IllegalArgumentException("Search query must not be blank");
		}
		var storage = currentStorage();
		var normalizedQuery = query.toLowerCase();
		var resources = new ArrayList<ResourceResponse>();
		try {
			var objects = minioClient.listObjects(ListObjectsArgs.builder()
					.bucket(properties.bucket())
					.prefix(storage.rootPrefix())
					.recursive(true)
					.build());
			for (var result : objects) {
				var item = result.get();
				var relative = storage.relative(item.objectName());
				if (relative.isEmpty()) {
					continue;
				}
				var name = resourceName(relative).toLowerCase();
				if (name.contains(normalizedQuery)) {
					resources.add(toResponse(relative, item.size(), item.isDir() || relative.endsWith("/")));
				}
			}
		}
		catch (Exception exception) {
			throw new StorageOperationException("Failed to search resources", exception);
		}
		resources.sort(Comparator
				.comparing((ResourceResponse resource) -> resource.path())
				.thenComparing(ResourceResponse::name));
		return resources;
	}

	@Override
	public List<ResourceResponse> upload(String path, List<MultipartFile> files) {
		if (files == null || files.isEmpty()) {
			throw new IllegalArgumentException("Upload request must contain at least one file");
		}

		var directory = ResourcePath.directory(path, true);
		var storage = currentStorage();
		if (!directoryExists(storage, directory.value())) {
			throw notFound("Upload directory not found");
		}

		var uploaded = new ArrayList<ResourceResponse>();
		for (var file : files) {
			uploaded.add(uploadFile(storage, directory.value(), file));
		}
		return uploaded;
	}

	@Override
	public List<ResourceResponse> list(String path) {
		var directory = ResourcePath.directory(path, true);
		var storage = currentStorage();
		if (!directoryExists(storage, directory.value())) {
			throw notFound("Directory not found");
		}

		var prefix = storage.absolute(directory.value());
		var resources = new ArrayList<ResourceResponse>();
		try {
			var objects = minioClient.listObjects(ListObjectsArgs.builder()
					.bucket(properties.bucket())
					.prefix(prefix)
					.recursive(false)
					.build());
			for (var result : objects) {
				var item = result.get();
				if (item.objectName().equals(prefix)) {
					continue;
				}
				var relative = storage.relative(item.objectName());
				var remainder = relative.substring(directory.value().length());
				if (remainder.isEmpty() || (!item.isDir() && remainder.contains("/"))) {
					continue;
				}
				resources.add(toResponse(relative, item.size(), item.isDir() || relative.endsWith("/")));
			}
		}
		catch (Exception exception) {
			throw new StorageOperationException("Failed to list directory", exception);
		}

		resources.sort(Comparator
				.comparing((ResourceResponse resource) -> resource.type() == ResourceType.DIRECTORY ? 0 : 1)
				.thenComparing(ResourceResponse::name));
		return resources;
	}

	@Override
	public ResourceResponse create(String path) {
		var directory = ResourcePath.directory(path, false);
		var storage = currentStorage();

		if (directoryExists(storage, directory.value())) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "Directory already exists");
		}

		var parent = parentDirectory(directory.value());
		if (!directoryExists(storage, parent)) {
			throw notFound("Parent directory not found");
		}

		putDirectoryMarker(storage.absolute(directory.value()));
		return toResponse(directory.value(), 0L, true);
	}

	private ResourceResponse moveFile(UserStorage storage, ResourcePath source, ResourcePath target) {
		var sourceObjectName = storage.absolute(source.value());
		var targetObjectName = storage.absolute(target.value());
		var sourceStat = statObject(sourceObjectName).orElseThrow(() -> notFound("Resource not found"));
		if (statObject(targetObjectName).isPresent()) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "Target resource already exists");
		}
		if (!directoryExists(storage, parentDirectory(target.value()))) {
			throw notFound("Target parent directory not found");
		}

		copyObject(sourceObjectName, targetObjectName);
		removeObject(sourceObjectName);
		return toResponse(target.value(), sourceStat.size(), false);
	}

	private ResourceResponse moveDirectory(UserStorage storage, ResourcePath source, ResourcePath target) {
		if (!directoryExists(storage, source.value())) {
			throw notFound("Resource not found");
		}
		if (directoryExists(storage, target.value())) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "Target resource already exists");
		}
		if (target.value().startsWith(source.value())) {
			throw new IllegalArgumentException("Directory cannot be moved inside itself");
		}
		if (!directoryExists(storage, parentDirectory(target.value()))) {
			throw notFound("Target parent directory not found");
		}

		copyDirectory(storage, source.value(), target.value());
		removeDirectory(storage, source.value());
		return toResponse(target.value(), 0L, true);
	}

	private void copyDirectory(UserStorage storage, String sourceDirectory, String targetDirectory) {
		var sourcePrefix = storage.absolute(sourceDirectory);
		try {
			var objects = minioClient.listObjects(ListObjectsArgs.builder()
					.bucket(properties.bucket())
					.prefix(sourcePrefix)
					.recursive(true)
					.build());
			for (var result : objects) {
				var sourceObjectName = result.get().objectName();
				var sourceRelative = storage.relative(sourceObjectName);
				var targetRelative = targetDirectory + sourceRelative.substring(sourceDirectory.length());
				copyObject(sourceObjectName, storage.absolute(targetRelative));
			}
		}
		catch (Exception exception) {
			throw new StorageOperationException("Failed to move directory", exception);
		}
	}

	private void copyObject(String sourceObjectName, String targetObjectName) {
		try {
			minioClient.copyObject(CopyObjectArgs.builder()
					.bucket(properties.bucket())
					.object(targetObjectName)
					.source(SourceObject.builder()
							.bucket(properties.bucket())
							.object(sourceObjectName)
							.build())
					.build());
		}
		catch (Exception exception) {
			throw new StorageOperationException("Failed to copy resource", exception);
		}
	}

	private ResourceResponse uploadFile(UserStorage storage, String directory, MultipartFile file) {
		var originalName = file.getOriginalFilename();
		if (originalName == null || originalName.isBlank()) {
			throw new IllegalArgumentException("Uploaded file name must not be blank");
		}

		var target = ResourcePath.file(directory + originalName);
		var objectName = storage.absolute(target.value());
		if (statObject(objectName).isPresent()) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "File already exists");
		}

		try (var input = file.getInputStream()) {
			minioClient.putObject(PutObjectArgs.builder()
					.bucket(properties.bucket())
					.object(objectName)
					.stream(input, Long.valueOf(file.getSize()), -1L)
					.contentType(contentType(file))
					.build());
			return toResponse(target.value(), file.getSize(), false);
		}
		catch (IOException exception) {
			throw new IllegalArgumentException("Failed to read uploaded file");
		}
		catch (Exception exception) {
			throw new StorageOperationException("Failed to upload file", exception);
		}
	}

	private void removeDirectory(UserStorage storage, String relativeDirectory) {
		var prefix = storage.absolute(relativeDirectory);
		var removedAny = false;
		try {
			var objects = minioClient.listObjects(ListObjectsArgs.builder()
					.bucket(properties.bucket())
					.prefix(prefix)
					.recursive(true)
					.build());
			for (Result<Item> result : objects) {
				removeObject(result.get().objectName());
				removedAny = true;
			}
		}
		catch (Exception exception) {
			throw new StorageOperationException("Failed to delete directory", exception);
		}
		if (!removedAny) {
			removeObject(prefix);
		}
	}

	private boolean directoryExists(UserStorage storage, String relativeDirectory) {
		if (relativeDirectory.isEmpty()) {
			return true;
		}

		var prefix = storage.absolute(relativeDirectory);
		if (statObject(prefix).isPresent()) {
			return true;
		}

		try {
			var objects = minioClient.listObjects(ListObjectsArgs.builder()
					.bucket(properties.bucket())
					.prefix(prefix)
					.recursive(true)
					.maxKeys(1)
					.build());
			return objects.iterator().hasNext();
		}
		catch (Exception exception) {
			throw new StorageOperationException("Failed to check directory", exception);
		}
	}

	private Optional<StatObjectResponse> statObject(String objectName) {
		try {
			return Optional.of(minioClient.statObject(StatObjectArgs.builder()
					.bucket(properties.bucket())
					.object(objectName)
					.build()));
		}
		catch (Exception exception) {
			if (isNotFound(exception)) {
				return Optional.empty();
			}
			throw new StorageOperationException("Failed to inspect resource", exception);
		}
	}

	private void putDirectoryMarker(String objectName) {
		try (var input = new java.io.ByteArrayInputStream(new byte[0])) {
			minioClient.putObject(PutObjectArgs.builder()
					.bucket(properties.bucket())
					.object(objectName)
					.stream(input, 0L, -1L)
					.build());
		}
		catch (Exception exception) {
			throw new StorageOperationException("Failed to create directory", exception);
		}
	}

	private void removeObject(String objectName) {
		try {
			minioClient.removeObject(RemoveObjectArgs.builder()
					.bucket(properties.bucket())
					.object(objectName)
					.build());
		}
		catch (Exception exception) {
			throw new StorageOperationException("Failed to delete resource", exception);
		}
	}

	private UserStorage currentStorage() {
		var user = currentUserProvider.currentUser();
		if (user.getId() == null) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User is not authenticated");
		}
		return new UserStorage(properties.userRootPrefix(user.getId()) + "/");
	}

	private static ResourceResponse toResponse(String relativePath, long size, boolean directory) {
		return new ResourceResponse(
				parentDirectory(relativePath),
				resourceName(relativePath),
				directory ? null : size,
				directory ? ResourceType.DIRECTORY : ResourceType.FILE);
	}

	private static String parentDirectory(String relativePath) {
		var path = relativePath.endsWith("/")
				? relativePath.substring(0, relativePath.length() - 1)
				: relativePath;
		var index = path.lastIndexOf('/');
		return index < 0 ? "" : path.substring(0, index + 1);
	}

	private static String resourceName(String relativePath) {
		var path = relativePath.endsWith("/")
				? relativePath.substring(0, relativePath.length() - 1)
				: relativePath;
		var index = path.lastIndexOf('/');
		return index < 0 ? path : path.substring(index + 1);
	}

	private static String contentType(MultipartFile file) {
		var contentType = file.getContentType();
		return contentType == null || contentType.isBlank()
				? MediaType.APPLICATION_OCTET_STREAM_VALUE
				: contentType;
	}

	private static boolean isNotFound(Exception exception) {
		return exception instanceof ErrorResponseException responseException
				&& ("NoSuchKey".equals(responseException.errorResponse().code())
						|| "NoSuchObject".equals(responseException.errorResponse().code())
						|| "NoSuchBucket".equals(responseException.errorResponse().code()));
	}

	private static ResponseStatusException notFound(String message) {
		return new ResponseStatusException(HttpStatus.NOT_FOUND, message);
	}

	private record UserStorage(String rootPrefix) {

		String absolute(String relativePath) {
			return rootPrefix + relativePath;
		}

		String relative(String objectName) {
			if (!objectName.startsWith(rootPrefix)) {
				throw new IllegalArgumentException("Object is outside current user root");
			}
			return objectName.substring(rootPrefix.length());
		}
	}
}
