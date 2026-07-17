package dev.ratmir.cloudstorage.storage.api;

import dev.ratmir.cloudstorage.config.OpenApiConfig;
import dev.ratmir.cloudstorage.storage.service.ResourceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/storage")
@Tag(name = "Storage")
@SecurityRequirement(name = OpenApiConfig.SESSION_COOKIE)
public class StorageController {

	private final ResourceService resourceService;

	public StorageController(ResourceService resourceService) {
		this.resourceService = resourceService;
	}

	@GetMapping("/usage")
	@Operation(summary = "Get current storage usage and limits")
	StorageUsageResponse usage() {
		return resourceService.usage();
	}
}
