package dev.ratmir.cloudstorage.auth.service;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import dev.ratmir.cloudstorage.auth.api.AuthRequest;
import dev.ratmir.cloudstorage.auth.api.UserResponse;
import dev.ratmir.cloudstorage.storage.service.ObjectStorageService;
import dev.ratmir.cloudstorage.user.UserAccount;
import dev.ratmir.cloudstorage.user.UserAccountRepository;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextHolderStrategy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
class DefaultAuthService implements AuthService {

	private final UserAccountRepository users;
	private final PasswordEncoder passwordEncoder;
	private final AuthenticationManager authenticationManager;
	private final SecurityContextRepository securityContextRepository;
	private final ObjectStorageService objectStorageService;
	private final SecurityContextHolderStrategy securityContextHolderStrategy =
			SecurityContextHolder.getContextHolderStrategy();

	DefaultAuthService(
			UserAccountRepository users,
			PasswordEncoder passwordEncoder,
			AuthenticationManager authenticationManager,
			SecurityContextRepository securityContextRepository,
			ObjectStorageService objectStorageService) {
		this.users = users;
		this.passwordEncoder = passwordEncoder;
		this.authenticationManager = authenticationManager;
		this.securityContextRepository = securityContextRepository;
		this.objectStorageService = objectStorageService;
	}

	@Override
	@Transactional
	public UserResponse signUp(
			AuthRequest request,
			HttpServletRequest httpRequest,
			HttpServletResponse httpResponse) {
		if (users.existsByUsername(request.username())) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "Username is already taken");
		}

		var account = new UserAccount(request.username(), passwordEncoder.encode(request.password()));
		try {
			users.saveAndFlush(account);
		}
		catch (DataIntegrityViolationException exception) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "Username is already taken", exception);
		}
		if (account.getId() == null) {
			throw new IllegalStateException("User id must be available after save");
		}
		objectStorageService.ensureUserRoot(account.getId());

		return authenticate(request, httpRequest, httpResponse);
	}

	@Override
	public UserResponse signIn(
			AuthRequest request,
			HttpServletRequest httpRequest,
			HttpServletResponse httpResponse) {
		return authenticate(request, httpRequest, httpResponse);
	}

	private UserResponse authenticate(
			AuthRequest request,
			HttpServletRequest httpRequest,
			HttpServletResponse httpResponse) {
		try {
			var token = UsernamePasswordAuthenticationToken.unauthenticated(request.username(), request.password());
			var authentication = authenticationManager.authenticate(token);
			var context = securityContextHolderStrategy.createEmptyContext();
			context.setAuthentication(authentication);
			securityContextHolderStrategy.setContext(context);
			securityContextRepository.saveContext(context, httpRequest, httpResponse);
			return new UserResponse(authentication.getName());
		}
		catch (AuthenticationException exception) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid username or password", exception);
		}
	}
}
