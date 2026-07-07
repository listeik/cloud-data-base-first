package dev.ratmir.cloudstorage.auth.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.util.Optional;

import dev.ratmir.cloudstorage.user.UserAccount;
import dev.ratmir.cloudstorage.user.UserAccountRepository;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.test.util.ReflectionTestUtils;

class JpaUserDetailsServiceTest {

	private final UserAccountRepository users = org.mockito.Mockito.mock(UserAccountRepository.class);
	private final JpaUserDetailsService service = new JpaUserDetailsService(users);

	@Test
	void loadsAuthenticatedUserWithId() {
		var account = new UserAccount("user_1", "encoded-password");
		ReflectionTestUtils.setField(account, "id", 42L);
		when(users.findByUsername("user_1")).thenReturn(Optional.of(account));

		var details = service.loadUserByUsername("user_1");

		var user = assertInstanceOf(AuthenticatedUser.class, details);
		assertEquals(42L, user.getId());
		assertEquals("user_1", user.getUsername());
		assertEquals("encoded-password", user.getPassword());
	}

	@Test
	void rejectsMissingUser() {
		when(users.findByUsername("missing")).thenReturn(Optional.empty());

		assertThrows(UsernameNotFoundException.class, () -> service.loadUserByUsername("missing"));
	}
}
