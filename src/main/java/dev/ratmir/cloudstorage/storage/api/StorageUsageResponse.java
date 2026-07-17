package dev.ratmir.cloudstorage.storage.api;

public record StorageUsageResponse(
		long usedBytes,
		long quotaBytes,
		long remainingBytes,
		long maxFileSizeBytes) {
}
