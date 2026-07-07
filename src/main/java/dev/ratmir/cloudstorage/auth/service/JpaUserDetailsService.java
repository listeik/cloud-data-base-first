package dev.ratmir.cloudstorage.auth.service;

import dev.ratmir.cloudstorage.user.UserAccount;
import dev.ratmir.cloudstorage.user.UserAccountRepository;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class JpaUserDetailsService implements UserDetailsService {

	private final UserAccountRepository users;

	JpaUserDetailsService(UserAccountRepository users) {
		this.users = users;
	}

	@Override
	@Transactional(readOnly = true)
	public UserDetails loadUserByUsername(String username) {
		return users.findByUsername(username)
				.map(this::toUserDetails)
				.orElseThrow(() -> new UsernameNotFoundException("User not found"));
	}

	private UserDetails toUserDetails(UserAccount account) {
		return AuthenticatedUser.from(account);
	}
}
