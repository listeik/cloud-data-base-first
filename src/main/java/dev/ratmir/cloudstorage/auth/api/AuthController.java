package dev.ratmir.cloudstorage.auth.api;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

import dev.ratmir.cloudstorage.auth.service.AuthService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

	private final AuthService authService;

	public AuthController(AuthService authService) {
		this.authService = authService;
	}

	@PostMapping("/sign-up")
	@ResponseStatus(HttpStatus.CREATED)
	UserResponse signUp(
			@Valid @RequestBody AuthRequest request,
			HttpServletRequest httpRequest,
			HttpServletResponse httpResponse) {
		return authService.signUp(request, httpRequest, httpResponse);
	}

	@PostMapping("/sign-in")
	UserResponse signIn(
			@Valid @RequestBody AuthRequest request,
			HttpServletRequest httpRequest,
			HttpServletResponse httpResponse) {
		return authService.signIn(request, httpRequest, httpResponse);
	}

	@PostMapping("/sign-out")
	ResponseEntity<Void> signOut(HttpServletRequest request) {
		SecurityContextHolder.clearContext();
		var session = request.getSession(false);
		if (session != null) {
			session.invalidate();
		}
		return ResponseEntity.noContent().build();
	}
}
