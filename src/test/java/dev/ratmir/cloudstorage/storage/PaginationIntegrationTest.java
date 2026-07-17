package dev.ratmir.cloudstorage.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.ratmir.cloudstorage.AbstractIntegrationTest;
import dev.ratmir.cloudstorage.storage.api.ResourcePageResponse;

import org.junit.jupiter.api.Test;

import org.springframework.http.HttpStatus;

class PaginationIntegrationTest extends AbstractIntegrationTest {

	@Test
	void directoryContentsCanBeReadPageByPage() throws Exception {
		var session = signUp(uniqueUsername("directory_page"));
		assertStatus(postEmpty("/api/directory?path=alpha/", session), HttpStatus.CREATED);
		assertStatus(postEmpty("/api/directory?path=beta/", session), HttpStatus.CREATED);
		assertStatus(postEmpty("/api/directory?path=gamma/", session), HttpStatus.CREATED);

		var firstResponse = get("/api/directory/page?path=&page=0&size=2", session);
		assertStatus(firstResponse, HttpStatus.OK);
		var first = read(firstResponse, ResourcePageResponse.class);
		assertEquals(2, first.content().size());
		assertEquals("alpha", first.content().get(0).name());
		assertEquals("beta", first.content().get(1).name());
		assertEquals(3, first.totalElements());
		assertEquals(2, first.totalPages());

		var secondResponse = get("/api/directory/page?path=&page=1&size=2", session);
		assertStatus(secondResponse, HttpStatus.OK);
		var second = read(secondResponse, ResourcePageResponse.class);
		assertEquals(1, second.content().size());
		assertEquals("gamma", second.content().getFirst().name());
	}

	@Test
	void searchResultsCanBeReadPageByPage() throws Exception {
		var session = signUp(uniqueUsername("search_page"));
		assertStatus(postEmpty("/api/directory?path=report-a/", session), HttpStatus.CREATED);
		assertStatus(postEmpty("/api/directory?path=report-b/", session), HttpStatus.CREATED);
		assertStatus(postEmpty("/api/directory?path=report-c/", session), HttpStatus.CREATED);

		var response = get("/api/resource/search/page?query=report&page=1&size=2", session);

		assertStatus(response, HttpStatus.OK);
		var page = read(response, ResourcePageResponse.class);
		assertEquals(1, page.content().size());
		assertEquals("report-c", page.content().getFirst().name());
		assertEquals(3, page.totalElements());
		assertEquals(2, page.totalPages());
	}

	@Test
	void invalidPageSizeIsRejected() throws Exception {
		var session = signUp(uniqueUsername("invalid_page"));

		var response = get("/api/directory/page?path=&page=0&size=101", session);

		assertStatus(response, HttpStatus.BAD_REQUEST);
	}
}
