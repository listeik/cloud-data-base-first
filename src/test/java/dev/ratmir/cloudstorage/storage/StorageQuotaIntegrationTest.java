package dev.ratmir.cloudstorage.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.ratmir.cloudstorage.AbstractIntegrationTest;
import dev.ratmir.cloudstorage.storage.api.StorageUsageResponse;

import org.junit.jupiter.api.Test;

import org.springframework.http.HttpStatus;

class StorageQuotaIntegrationTest extends AbstractIntegrationTest {

	@Test
	void usageReflectsUploadedAndDeletedFiles() throws Exception {
		var session = signUp(uniqueUsername("usage"));
		var content = new byte[20];

		var initial = get("/api/storage/usage", session);
		assertStatus(initial, HttpStatus.OK);
		assertUsage(read(initial, StorageUsageResponse.class), 0, 64, 64, 32);

		var upload = postMultipart("/api/resource?path=", session, "files", "usage.bin", content);
		assertStatus(upload, HttpStatus.CREATED);

		var afterUpload = get("/api/storage/usage", session);
		assertStatus(afterUpload, HttpStatus.OK);
		assertUsage(read(afterUpload, StorageUsageResponse.class), 20, 64, 44, 32);

		assertStatus(delete("/api/resource?path=usage.bin", session), HttpStatus.NO_CONTENT);

		var afterDelete = get("/api/storage/usage", session);
		assertStatus(afterDelete, HttpStatus.OK);
		assertUsage(read(afterDelete, StorageUsageResponse.class), 0, 64, 64, 32);
	}

	@Test
	void uploadRejectsFilesLargerThanPerFileLimit() throws Exception {
		var session = signUp(uniqueUsername("file_limit"));

		var response = postMultipart("/api/resource?path=", session, "files", "large.bin", new byte[33]);

		assertStatus(response, HttpStatus.PAYLOAD_TOO_LARGE);
	}

	@Test
	void uploadRejectsDataThatExceedsUserQuota() throws Exception {
		var session = signUp(uniqueUsername("quota"));
		assertStatus(
				postMultipart("/api/resource?path=", session, "files", "first.bin", new byte[32]),
				HttpStatus.CREATED);
		assertStatus(
				postMultipart("/api/resource?path=", session, "files", "second.bin", new byte[32]),
				HttpStatus.CREATED);

		var response = postMultipart("/api/resource?path=", session, "files", "overflow.bin", new byte[1]);

		assertStatus(response, HttpStatus.PAYLOAD_TOO_LARGE);
	}

	private static void assertUsage(
			StorageUsageResponse usage,
			long used,
			long quota,
			long remaining,
			long maxFileSize) {
		assertEquals(used, usage.usedBytes());
		assertEquals(quota, usage.quotaBytes());
		assertEquals(remaining, usage.remainingBytes());
		assertEquals(maxFileSize, usage.maxFileSizeBytes());
	}
}
