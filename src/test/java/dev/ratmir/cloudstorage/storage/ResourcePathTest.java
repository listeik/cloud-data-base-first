package dev.ratmir.cloudstorage.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ResourcePathTest {

	@Test
	void acceptsRootDirectoryWhenAllowed() {
		var path = ResourcePath.directory("", true);

		assertEquals("", path.value());
	}

	@Test
	void requiresDirectoryTrailingSlash() {
		var exception = assertThrows(IllegalArgumentException.class, () -> ResourcePath.directory("docs", false));

		assertEquals("Directory path must end with slash", exception.getMessage());
	}

	@Test
	void rejectsTraversal() {
		assertThrows(IllegalArgumentException.class, () -> ResourcePath.resource("docs/../secret.txt"));
	}

	@Test
	void acceptsNestedFile() {
		var path = ResourcePath.file("docs/report.txt");

		assertEquals("docs/report.txt", path.value());
		assertTrue(!path.directory());
	}
}
