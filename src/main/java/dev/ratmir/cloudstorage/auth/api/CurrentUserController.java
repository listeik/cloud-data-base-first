package dev.ratmir.cloudstorage.auth.api;

import dev.ratmir.cloudstorage.auth.service.CurrentUserProvider;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/user")
public class CurrentUserController {

	private final CurrentUserProvider currentUserProvider;

	public CurrentUserController(CurrentUserProvider currentUserProvider) {
		this.currentUserProvider = currentUserProvider;
	}

	@GetMapping("/me")
	UserResponse currentUser() {
		return new UserResponse(currentUserProvider.currentUser().getUsername());
	}
}
