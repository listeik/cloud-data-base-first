package dev.ratmir.cloudstorage.auth.api;

import java.security.Principal;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/user")
public class CurrentUserController {

	@GetMapping("/me")
	UserResponse currentUser(Principal principal) {
		return new UserResponse(principal.getName());
	}
}
