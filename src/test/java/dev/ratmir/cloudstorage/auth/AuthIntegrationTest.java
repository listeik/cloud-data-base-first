package dev.ratmir.cloudstorage.auth;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import dev.ratmir.cloudstorage.AbstractIntegrationTest;
import dev.ratmir.cloudstorage.auth.api.AuthRequest;
import dev.ratmir.cloudstorage.auth.api.UserResponse;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;

class AuthIntegrationTest extends AbstractIntegrationTest {

	@Autowired
	private StringRedisTemplate redis;

	@BeforeEach
	void clearRedis() {
		var connectionFactory = redis.getConnectionFactory();
		assertNotNull(connectionFactory);
		try (var connection = connectionFactory.getConnection()) {
			connection.serverCommands().flushDb();
		}
	}

	@Test
	void signUpCreatesAuthenticatedRedisBackedSession() throws Exception {
		var username = uniqueUsername("auth");
		var session = signUp(username);

		var currentUser = get("/api/user/me", session);

		assertStatus(currentUser, HttpStatus.OK);
		org.junit.jupiter.api.Assertions.assertEquals(username, read(currentUser, UserResponse.class).username());
		var sessionKeys = redis.keys("cloud-storage:sessions:*");
		assertNotNull(sessionKeys);
		assertFalse(sessionKeys.isEmpty());
	}

	@Test
	void duplicateUsernamesAreRejected() throws Exception {
		var username = uniqueUsername("duplicate");
		signUp(username);

		var duplicate = postJson("/api/auth/sign-up", new AuthRequest(username, PASSWORD), null);

		assertStatus(duplicate, HttpStatus.CONFLICT);
	}

	@Test
	void signOutInvalidatesSession() throws Exception {
		var session = signUp(uniqueUsername("signout"));

		var signOut = postEmpty("/api/auth/sign-out", session);
		assertStatus(signOut, HttpStatus.NO_CONTENT);

		var currentUser = get("/api/user/me", session);

		assertStatus(currentUser, HttpStatus.UNAUTHORIZED);
	}
}
