package dev.ratmir.cloudstorage.storage.api;

import java.util.List;

import jakarta.validation.constraints.NotBlank;

import dev.ratmir.cloudstorage.storage.service.DirectoryService;
import dev.ratmir.cloudstorage.config.OpenApiConfig;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/directory")
@Tag(name = "Directories")
@SecurityRequirement(name = OpenApiConfig.SESSION_COOKIE)
public class DirectoryController {

	private final DirectoryService directoryService;

	public DirectoryController(DirectoryService directoryService) {
		this.directoryService = directoryService;
	}

	@GetMapping
	@Operation(summary = "List directory contents")
	List<ResourceResponse> list(@RequestParam(defaultValue = "") String path) {
		return directoryService.list(path);
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@Operation(summary = "Create an empty directory")
	ResourceResponse create(@RequestParam @NotBlank String path) {
		return directoryService.create(path);
	}
}
