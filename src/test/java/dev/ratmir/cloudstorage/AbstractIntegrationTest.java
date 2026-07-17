package dev.ratmir.cloudstorage;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

import dev.ratmir.cloudstorage.auth.api.AuthRequest;
import dev.ratmir.cloudstorage.auth.api.UserResponse;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MinIOContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.lifecycle.Startables;
import org.testcontainers.utility.DockerImageName;
import tools.jackson.databind.ObjectMapper;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest(
		webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
		properties = "spring.docker.compose.enabled=false")
public abstract class AbstractIntegrationTest {

	protected static final String PASSWORD = "password123";

	private final HttpClient http = HttpClient.newHttpClient();

	private static final PostgreSQLContainer<?> POSTGRES =
			new PostgreSQLContainer<>(DockerImageName.parse("postgres:18.4-alpine"));

	private static final GenericContainer<?> REDIS =
			new GenericContainer<>(DockerImageName.parse("redis:8.6.4")).withExposedPorts(6379);

	private static final MinIOContainer MINIO =
			new MinIOContainer(DockerImageName.parse("minio/minio:RELEASE.2025-09-07T16-13-09Z"))
					.withUserName("minioadmin")
					.withPassword("minioadmin");

	static {
		Startables.deepStart(POSTGRES, REDIS, MINIO).join();
	}

	@Autowired
	private ObjectMapper objectMapper;

	@LocalServerPort
	private int port;

	@DynamicPropertySource
	static void registerInfrastructureProperties(DynamicPropertyRegistry registry) {
		registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
		registry.add("spring.datasource.username", POSTGRES::getUsername);
		registry.add("spring.datasource.password", POSTGRES::getPassword);
		registry.add("spring.data.redis.host", REDIS::getHost);
		registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
		registry.add("app.storage.endpoint", MINIO::getS3URL);
		registry.add("app.storage.access-key", MINIO::getUserName);
		registry.add("app.storage.secret-key", MINIO::getPassword);
		registry.add("app.storage.bucket", () -> "integration-user-files");
		registry.add("app.storage.user-root-prefix-pattern", () -> "it-user-%d-files");
		registry.add("app.storage.user-quota", () -> "64B");
		registry.add("app.storage.max-file-size", () -> "32B");
	}

	protected String signUp(String username) throws IOException, InterruptedException {
		var response = postJson("/api/auth/sign-up", new AuthRequest(username, PASSWORD), null);

		assertStatus(response, HttpStatus.CREATED);
		org.junit.jupiter.api.Assertions.assertEquals(username, read(response, UserResponse.class).username());
		return sessionHeaders(response);
	}

	protected HttpResponse<String> get(String path, String sessionCookie) throws IOException, InterruptedException {
		return http.send(request(path, sessionCookie).GET().build(), HttpResponse.BodyHandlers.ofString());
	}

	protected HttpResponse<byte[]> getBytes(String path, String sessionCookie)
			throws IOException, InterruptedException {
		return http.send(request(path, sessionCookie).GET().build(), HttpResponse.BodyHandlers.ofByteArray());
	}

	protected HttpResponse<String> postEmpty(String path, String sessionCookie)
			throws IOException, InterruptedException {
		return http.send(
				request(path, sessionCookie).POST(HttpRequest.BodyPublishers.noBody()).build(),
				HttpResponse.BodyHandlers.ofString());
	}

	protected HttpResponse<String> postJson(String path, Object body, String sessionCookie)
			throws IOException, InterruptedException {
		return http.send(
				request(path, sessionCookie)
						.header("Content-Type", "application/json")
						.POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
						.build(),
				HttpResponse.BodyHandlers.ofString());
	}

	protected HttpResponse<String> postMultipart(
			String path,
			String sessionCookie,
			String partName,
			String fileName,
			byte[] content) throws IOException, InterruptedException {
		var boundary = "cloud-storage-boundary-" + System.nanoTime();
		return http.send(
				request(path, sessionCookie)
						.header("Content-Type", "multipart/form-data; boundary=" + boundary)
						.POST(HttpRequest.BodyPublishers.ofByteArray(multipartBody(
								boundary,
								partName,
								fileName,
								content)))
						.build(),
				HttpResponse.BodyHandlers.ofString());
	}

	protected HttpResponse<String> delete(String path, String sessionCookie) throws IOException, InterruptedException {
		return http.send(request(path, sessionCookie).DELETE().build(), HttpResponse.BodyHandlers.ofString());
	}

	protected <T> T read(HttpResponse<String> response, Class<T> type) throws IOException {
		return objectMapper.readValue(response.body(), type);
	}

	protected <T> java.util.List<T> readList(HttpResponse<String> response, Class<T> elementType) throws IOException {
		return objectMapper.readValue(
				response.body(),
				objectMapper.getTypeFactory().constructCollectionType(java.util.List.class, elementType));
	}

	protected static void assertStatus(HttpResponse<?> response, HttpStatus expected) {
		org.junit.jupiter.api.Assertions.assertEquals(expected.value(), response.statusCode(), () -> "body: "
				+ response.body());
	}

	protected static String uniqueUsername(String prefix) {
		return prefix + "_" + System.nanoTime();
	}

	private HttpRequest.Builder request(String path, String sessionCookie) {
		var builder = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
				.header("Accept", "application/json");
		if (sessionCookie != null) {
			builder.header("Cookie", sessionCookie);
		}
		return builder;
	}

	private static String sessionHeaders(HttpResponse<?> response) {
		var cookies = response.headers().allValues("set-cookie");
		assertNotNull(cookies, "session cookie must be returned");
		assertFalse(cookies.isEmpty(), "session cookie must be returned");
		return cookies.stream()
				.map(cookie -> cookie.split(";", 2)[0])
				.collect(Collectors.joining("; "));
	}

	private static byte[] multipartBody(String boundary, String partName, String fileName, byte[] content)
			throws IOException {
		var output = new ByteArrayOutputStream();
		output.write(("--" + boundary + "\r\n").getBytes(StandardCharsets.UTF_8));
		output.write(("Content-Disposition: form-data; name=\"" + partName + "\"; filename=\"" + fileName + "\"\r\n")
				.getBytes(StandardCharsets.UTF_8));
		output.write("Content-Type: application/octet-stream\r\n\r\n".getBytes(StandardCharsets.UTF_8));
		output.write(content);
		output.write(("\r\n--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));
		return output.toByteArray();
	}
}
