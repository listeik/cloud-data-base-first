package dev.ratmir.cloudstorage.auth.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import dev.ratmir.cloudstorage.auth.api.AuthRequest;
import dev.ratmir.cloudstorage.user.UserAccount;
import dev.ratmir.cloudstorage.user.UserAccountRepository;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.server.ResponseStatusException;

class DefaultAuthServiceTest {

	private final UserAccountRepository users = org.mockito.Mockito.mock(UserAccountRepository.class);
	private final PasswordEncoder passwordEncoder = org.mockito.Mockito.mock(PasswordEncoder.class);
	private final AuthenticationManager authenticationManager = org.mockito.Mockito.mock(AuthenticationManager.class);
	private final SecurityContextRepository securityContextRepository =
			org.mockito.Mockito.mock(SecurityContextRepository.class);
	private final HttpServletRequest httpRequest = org.mockito.Mockito.mock(HttpServletRequest.class);
	private final HttpServletResponse httpResponse = org.mockito.Mockito.mock(HttpServletResponse.class);
	private final DefaultAuthService authService =
			new DefaultAuthService(users, passwordEncoder, authenticationManager, securityContextRepository);

	@AfterEach
	void clearSecurityContext() {
		SecurityContextHolder.clearContext();
	}

	@Test
	void signUpStoresEncodedPasswordAndAuthenticatesUser() {
		var request = new AuthRequest("user_1", "password123");
		var authentication = new UsernamePasswordAuthenticationToken(
				"user_1",
				"password123",
				List.of(new SimpleGrantedAuthority("ROLE_USER")));

		when(users.existsByUsername("user_1")).thenReturn(false);
		when(passwordEncoder.encode("password123")).thenReturn("encoded-password");
		when(users.saveAndFlush(any(UserAccount.class))).thenAnswer(invocation -> invocation.getArgument(0));
		when(authenticationManager.authenticate(any())).thenReturn(authentication);

		var response = authService.signUp(request, httpRequest, httpResponse);

		assertEquals("user_1", response.username());

		var account = ArgumentCaptor.forClass(UserAccount.class);
		verify(users).saveAndFlush(account.capture());
		assertEquals("user_1", account.getValue().getUsername());
		assertEquals("encoded-password", account.getValue().getPasswordHash());
		verify(securityContextRepository).saveContext(any(SecurityContext.class), eq(httpRequest), eq(httpResponse));
	}

	@Test
	void signUpRejectsDuplicateUsername() {
		var request = new AuthRequest("user_1", "password123");
		when(users.existsByUsername("user_1")).thenReturn(true);

		var exception = assertThrows(
				ResponseStatusException.class,
				() -> authService.signUp(request, httpRequest, httpResponse));

		assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
		verify(users, never()).saveAndFlush(any());
	}

	@Test
	void signInRejectsInvalidCredentials() {
		var request = new AuthRequest("user_1", "password123");
		when(authenticationManager.authenticate(any()))
				.thenThrow(new BadCredentialsException("Bad credentials"));

		var exception = assertThrows(
				ResponseStatusException.class,
				() -> authService.signIn(request, httpRequest, httpResponse));

		assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
	}
}
