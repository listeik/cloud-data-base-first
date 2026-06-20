package dev.ratmir.cloudstorage.auth.service;

import dev.ratmir.cloudstorage.auth.api.AuthRequest;
import dev.ratmir.cloudstorage.auth.api.UserResponse;

public interface AuthService {

	UserResponse signUp(AuthRequest request);

	UserResponse signIn(AuthRequest request);
}
