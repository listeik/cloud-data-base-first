package dev.ratmir.cloudstorage.auth.service;

import dev.ratmir.cloudstorage.user.UserAccountRepository;

import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class CurrentUserProvider {

	private final UserAccountRepository users;

	public CurrentUserProvider(UserAccountRepository users) {
		this.users = users;
	}

	@Transactional(readOnly = true)
	public AuthenticatedUser currentUser() {
		var authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null
				|| !authentication.isAuthenticated()
				|| authentication instanceof AnonymousAuthenticationToken) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User is not authenticated");
		}

		var principal = authentication.getPrincipal();
		if (principal instanceof AuthenticatedUser user) {
			return user;
		}
		if (principal instanceof UserDetails details) {
			return findByUsername(details.getUsername());
		}
		if (principal instanceof String username) {
			return findByUsername(username);
		}

		throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User is not authenticated");
	}

	private AuthenticatedUser findByUsername(String username) {
		return users.findByUsername(username)
				.map(AuthenticatedUser::from)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User is not authenticated"));
	}
}
