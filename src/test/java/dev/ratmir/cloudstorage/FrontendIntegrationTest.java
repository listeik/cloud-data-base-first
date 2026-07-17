package dev.ratmir.cloudstorage;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import org.springframework.http.HttpStatus;

class FrontendIntegrationTest extends AbstractIntegrationTest {

	@Test
	void frontendEntryPointIsAvailableWithoutAuthentication() throws Exception {
		var response = get("/", null, "text/html");

		assertStatus(response, HttpStatus.OK);
		assertTrue(response.body().contains("Cloud Data Base"));
		assertTrue(response.body().contains("/assets/app.js"));
	}

	@Test
	void frontendAssetsAreAvailableWithoutAuthentication() throws Exception {
		var script = get("/assets/app.js", null, "application/javascript");

		assertStatus(script, HttpStatus.OK);
		assertTrue(script.body().contains("/api/directory/page"));
	}
}
