package dev.ratmir.cloudstorage.storage;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.List;

import dev.ratmir.cloudstorage.AbstractIntegrationTest;
import dev.ratmir.cloudstorage.storage.api.ResourceResponse;
import dev.ratmir.cloudstorage.storage.api.ResourceType;

import org.junit.jupiter.api.Test;

import org.springframework.http.HttpStatus;

class ResourceFlowIntegrationTest extends AbstractIntegrationTest {

	@Test
	void userCanManageFileResourcesInMinioStorage() throws Exception {
		var session = signUp(uniqueUsername("resources"));
		var content = "hello from integration test".getBytes(UTF_8);

		var directory = postEmpty("/api/directory?path=docs/", session);
		assertStatus(directory, HttpStatus.CREATED);
		assertResource(read(directory, ResourceResponse.class), "", "docs", null, ResourceType.DIRECTORY);

		var upload = postMultipart("/api/resource?path=docs/", session, "files", "report.txt", content);
		assertStatus(upload, HttpStatus.CREATED);
		assertResource(
				only(readList(upload, ResourceResponse.class)),
				"docs/",
				"report.txt",
				content.length,
				ResourceType.FILE);

		var list = get("/api/directory?path=docs/", session);
		assertStatus(list, HttpStatus.OK);
		assertResource(
				only(readList(list, ResourceResponse.class)),
				"docs/",
				"report.txt",
				content.length,
				ResourceType.FILE);

		var search = get("/api/resource/search?query=report", session);
		assertStatus(search, HttpStatus.OK);
		assertResource(
				only(readList(search, ResourceResponse.class)),
				"docs/",
				"report.txt",
				content.length,
				ResourceType.FILE);

		var moved = get("/api/resource/move?from=docs/report.txt&to=docs/archive.txt", session);
		assertStatus(moved, HttpStatus.OK);
		assertResource(read(moved, ResourceResponse.class), "docs/", "archive.txt", content.length, ResourceType.FILE);

		var download = getBytes("/api/resource/download?path=docs/archive.txt", session);
		assertStatus(download, HttpStatus.OK);
		assertArrayEquals(content, download.body());

		var delete = delete("/api/resource?path=docs/archive.txt", session);
		assertStatus(delete, HttpStatus.NO_CONTENT);

		var deleted = get("/api/resource?path=docs/archive.txt", session);
		assertStatus(deleted, HttpStatus.NOT_FOUND);
	}

	@Test
	void usersDoNotSeeEachOthersFiles() throws Exception {
		var firstUser = signUp(uniqueUsername("first"));
		var secondUser = signUp(uniqueUsername("second"));

		postEmpty("/api/directory?path=private/", firstUser);
		postMultipart(
				"/api/resource?path=private/",
				firstUser,
				"files",
				"secret.txt",
				"first user secret".getBytes(UTF_8));

		var secondUserRoot = get("/api/directory?path=", secondUser);

		assertStatus(secondUserRoot, HttpStatus.OK);
		assertEquals(List.of(), readList(secondUserRoot, ResourceResponse.class));
	}

	private static ResourceResponse only(List<ResourceResponse> resources) {
		assertNotNull(resources);
		assertEquals(1, resources.size());
		return resources.getFirst();
	}

	private static void assertResource(
			ResourceResponse response,
			String path,
			String name,
			Integer size,
			ResourceType type) {
		assertNotNull(response);
		assertEquals(path, response.path());
		assertEquals(name, response.name());
		assertEquals(size == null ? null : size.longValue(), response.size());
		assertEquals(type, response.type());
	}
}
