package dev.ratmir.cloudstorage;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import org.springframework.boot.micrometer.metrics.test.autoconfigure.AutoConfigureMetrics;
import org.springframework.http.HttpStatus;

@AutoConfigureMetrics(export = true)
class ObservabilityIntegrationTest extends AbstractIntegrationTest {

	@Test
	void prometheusEndpointExposesApplicationAndStorageMetrics() throws Exception {
		var session = signUp(uniqueUsername("metrics"));
		var content = "observed payload".getBytes(UTF_8);

		assertStatus(
				postMultipart("/api/resource?path=", session, "files", "observed.txt", content),
				HttpStatus.CREATED);
		assertStatus(getBytes("/api/resource/download?path=observed.txt", session), HttpStatus.OK);

		var response = get("/actuator/prometheus", null, "text/plain");

		assertStatus(response, HttpStatus.OK);
		assertTrue(response.body().contains("jvm_memory_used_bytes"));
		assertTrue(response.body().contains("cloud_storage_files_uploaded_total"));
		assertTrue(response.body().contains("cloud_storage_resources_downloaded_total"));
	}
}
