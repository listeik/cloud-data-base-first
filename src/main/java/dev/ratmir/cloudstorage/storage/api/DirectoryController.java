package dev.ratmir.cloudstorage.storage.api;

import java.util.List;

import jakarta.validation.constraints.NotBlank;

import dev.ratmir.cloudstorage.storage.service.DirectoryService;

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
public class DirectoryController {

	private final DirectoryService directoryService;

	public DirectoryController(DirectoryService directoryService) {
		this.directoryService = directoryService;
	}

	@GetMapping
	List<ResourceResponse> list(@RequestParam @NotBlank String path) {
		return directoryService.list(path);
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	ResourceResponse create(@RequestParam @NotBlank String path) {
		return directoryService.create(path);
	}
}
