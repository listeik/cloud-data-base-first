package dev.ratmir.cloudstorage.storage.service;

import java.io.ByteArrayInputStream;

import jakarta.annotation.PostConstruct;

import dev.ratmir.cloudstorage.storage.StorageOperationException;
import dev.ratmir.cloudstorage.storage.StorageProperties;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;

import org.springframework.stereotype.Service;

@Service
class MinioObjectStorageService implements ObjectStorageService {

	private static final byte[] EMPTY_DIRECTORY_MARKER = new byte[0];

	private final MinioClient minioClient;
	private final StorageProperties properties;

	MinioObjectStorageService(MinioClient minioClient, StorageProperties properties) {
		this.minioClient = minioClient;
		this.properties = properties;
	}

	@PostConstruct
	void initializeBucket() {
		ensureBucketExists();
	}

	@Override
	public void ensureUserRoot(long userId) {
		ensureBucketExists();
		var objectName = properties.userRootPrefix(userId) + "/";
		try (var input = new ByteArrayInputStream(EMPTY_DIRECTORY_MARKER)) {
			minioClient.putObject(PutObjectArgs.builder()
					.bucket(properties.bucket())
					.object(objectName)
					.stream(input, Long.valueOf(EMPTY_DIRECTORY_MARKER.length), -1L)
					.build());
		}
		catch (Exception exception) {
			throw new StorageOperationException("Failed to create user storage root", exception);
		}
	}

	private void ensureBucketExists() {
		try {
			var exists = minioClient.bucketExists(BucketExistsArgs.builder()
					.bucket(properties.bucket())
					.build());
			if (!exists) {
				minioClient.makeBucket(MakeBucketArgs.builder()
						.bucket(properties.bucket())
						.build());
			}
		}
		catch (Exception exception) {
			throw new StorageOperationException("Failed to initialize storage bucket", exception);
		}
	}
}
