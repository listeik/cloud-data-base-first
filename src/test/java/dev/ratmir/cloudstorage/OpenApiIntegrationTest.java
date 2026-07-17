package dev.ratmir.cloudstorage;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import org.springframework.http.HttpStatus;

class OpenApiIntegrationTest extends AbstractIntegrationTest {

	@Test
	void openApiContractIsAvailableWithoutAuthentication() throws Exception {
		var response = get("/v3/api-docs", null);

		assertStatus(response, HttpStatus.OK);
		assertTrue(response.body().contains("\"/api/resource\""));
		assertTrue(response.body().contains("\"sessionCookie\""));
	}
}
