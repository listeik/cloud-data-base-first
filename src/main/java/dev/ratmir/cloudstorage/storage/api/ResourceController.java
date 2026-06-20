package dev.ratmir.cloudstorage.storage.api;

import java.nio.charset.StandardCharsets;
import java.util.List;

import jakarta.validation.constraints.NotBlank;

import dev.ratmir.cloudstorage.storage.service.ResourceService;

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
public class ResourceController {

	private final ResourceService resourceService;

	public ResourceController(ResourceService resourceService) {
		this.resourceService = resourceService;
	}

	@GetMapping
	ResourceResponse get(@RequestParam @NotBlank String path) {
		return resourceService.get(path);
	}

	@DeleteMapping
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void delete(@RequestParam @NotBlank String path) {
		resourceService.delete(path);
	}

	@GetMapping("/download")
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
	ResourceResponse move(@RequestParam @NotBlank String from, @RequestParam @NotBlank String to) {
		return resourceService.move(from, to);
	}

	@GetMapping("/search")
	List<ResourceResponse> search(@RequestParam @NotBlank String query) {
		return resourceService.search(query);
	}

	@PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	@ResponseStatus(HttpStatus.CREATED)
	List<ResourceResponse> upload(
			@RequestParam(defaultValue = "") String path,
			@RequestPart("files") List<MultipartFile> files) {
		return resourceService.upload(path, files);
	}
}
