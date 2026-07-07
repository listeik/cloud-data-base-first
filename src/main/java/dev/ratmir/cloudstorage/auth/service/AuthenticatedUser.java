package dev.ratmir.cloudstorage.auth.service;

import java.util.Collection;
import java.util.List;

import dev.ratmir.cloudstorage.user.UserAccount;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

public final class AuthenticatedUser implements UserDetails {

	private final Long id;
	private final String username;
	private final String password;
	private final boolean enabled;
	private final List<GrantedAuthority> authorities;

	private AuthenticatedUser(
			Long id,
			String username,
			String password,
			boolean enabled,
			List<GrantedAuthority> authorities) {
		this.id = id;
		this.username = username;
		this.password = password;
		this.enabled = enabled;
		this.authorities = List.copyOf(authorities);
	}

	public static AuthenticatedUser from(UserAccount account) {
		return new AuthenticatedUser(
				account.getId(),
				account.getUsername(),
				account.getPasswordHash(),
				account.isEnabled(),
				List.of(new SimpleGrantedAuthority("ROLE_USER")));
	}

	public Long getId() {
		return id;
	}

	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		return authorities;
	}

	@Override
	public String getPassword() {
		return password;
	}

	@Override
	public String getUsername() {
		return username;
	}

	@Override
	public boolean isEnabled() {
		return enabled;
	}
}
