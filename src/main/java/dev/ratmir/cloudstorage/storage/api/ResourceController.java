package dev.ratmir.cloudstorage.storage.api;

import java.nio.charset.StandardCharsets;
import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import dev.ratmir.cloudstorage.storage.service.ResourceService;
import dev.ratmir.cloudstorage.config.OpenApiConfig;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Validated
@RestController
@RequestMapping("/api/resource")
@Tag(name = "Resources")
@SecurityRequirement(name = OpenApiConfig.SESSION_COOKIE)
public class ResourceController {

	private final ResourceService resourceService;

	public ResourceController(ResourceService resourceService) {
		this.resourceService = resourceService;
	}

	@GetMapping
	@Operation(summary = "Get resource metadata")
	ResourceResponse get(@RequestParam @NotBlank String path) {
		return resourceService.get(path);
	}

	@DeleteMapping
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@Operation(summary = "Delete a file or directory")
	void delete(@RequestParam @NotBlank String path) {
		resourceService.delete(path);
	}

	@GetMapping("/download")
	@Operation(summary = "Download a file or a directory as ZIP")
	ResponseEntity<InputStreamResource> download(@RequestParam @NotBlank String path) {
		var download = resourceService.download(path);
		var disposition = ContentDisposition.attachment()
				.filename(download.fileName(), StandardCharsets.UTF_8)
				.build();
		return ResponseEntity.ok()
				.contentType(download.contentType())
				.contentLength(download.contentLength())
				.header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
				.body(download.body());
	}

	@GetMapping("/move")
	@Operation(summary = "Move or rename a resource")
	ResourceResponse move(@RequestParam @NotBlank String from, @RequestParam @NotBlank String to) {
		return resourceService.move(from, to);
	}

	@GetMapping("/search")
	@Operation(summary = "Search resources by name")
	List<ResourceResponse> search(@RequestParam @NotBlank String query) {
		return resourceService.search(query);
	}

	@GetMapping("/search/page")
	@Operation(summary = "Search resources by name with pagination")
	ResourcePageResponse searchPage(
			@RequestParam @NotBlank String query,
			@RequestParam(defaultValue = "0") @Min(0) int page,
			@RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
		return ResourcePageResponse.from(resourceService.search(query), page, size);
	}

	@PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	@ResponseStatus(HttpStatus.CREATED)
	@Operation(summary = "Upload one or more files")
	List<ResourceResponse> upload(
			@RequestParam(defaultValue = "") String path,
			@RequestPart("files") List<MultipartFile> files) {
		return resourceService.upload(path, files);
	}
}
