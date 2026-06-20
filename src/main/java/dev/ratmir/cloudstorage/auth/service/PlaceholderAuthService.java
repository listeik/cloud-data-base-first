package dev.ratmir.cloudstorage.auth.service;

import dev.ratmir.cloudstorage.api.FeatureNotImplementedException;
import dev.ratmir.cloudstorage.auth.api.AuthRequest;
import dev.ratmir.cloudstorage.auth.api.UserResponse;

import org.springframework.stereotype.Service;

@Service
class PlaceholderAuthService implements AuthService {

	@Override
	public UserResponse signUp(AuthRequest request) {
		throw new FeatureNotImplementedException("Registration is not implemented yet");
	}

	@Override
	public UserResponse signIn(AuthRequest request) {
		throw new FeatureNotImplementedException("Sign in is not implemented yet");
	}
}
