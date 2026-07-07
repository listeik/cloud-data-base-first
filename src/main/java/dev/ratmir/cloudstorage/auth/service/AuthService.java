package dev.ratmir.cloudstorage.auth.service;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import dev.ratmir.cloudstorage.auth.api.AuthRequest;
import dev.ratmir.cloudstorage.auth.api.UserResponse;

public interface AuthService {

	UserResponse signUp(AuthRequest request, HttpServletRequest httpRequest, HttpServletResponse httpResponse);

	UserResponse signIn(AuthRequest request, HttpServletRequest httpRequest, HttpServletResponse httpResponse);
}
