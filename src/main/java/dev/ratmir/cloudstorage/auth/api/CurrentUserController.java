package dev.ratmir.cloudstorage.auth.api;

import dev.ratmir.cloudstorage.auth.service.CurrentUserProvider;
import dev.ratmir.cloudstorage.config.OpenApiConfig;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/user")
@Tag(name = "Users")
@SecurityRequirement(name = OpenApiConfig.SESSION_COOKIE)
public class CurrentUserController {

	private final CurrentUserProvider currentUserProvider;

	public CurrentUserController(CurrentUserProvider currentUserProvider) {
		this.currentUserProvider = currentUserProvider;
	}

	@GetMapping("/me")
	@Operation(summary = "Get the current user")
	UserResponse currentUser() {
		return new UserResponse(currentUserProvider.currentUser().getUsername());
	}
}
