package dev.ratmir.cloudstorage.storage;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.unit.DataSize;

@ConfigurationProperties(prefix = "app.storage")
public record StorageProperties(
		String endpoint,
		String bucket,
		String accessKey,
		String secretKey,
		String userRootPrefixPattern,
		DataSize userQuota,
		DataSize maxFileSize) {

	public StorageProperties {
		if (userQuota == null || userQuota.toBytes() <= 0) {
			throw new IllegalArgumentException("User quota must be positive");
		}
		if (maxFileSize == null || maxFileSize.toBytes() <= 0) {
			throw new IllegalArgumentException("Maximum file size must be positive");
		}
		if (maxFileSize.toBytes() > userQuota.toBytes()) {
			throw new IllegalArgumentException("Maximum file size must not exceed user quota");
		}
	}

	public String userRootPrefix(long userId) {
		return userRootPrefixPattern.formatted(userId);
	}
}
