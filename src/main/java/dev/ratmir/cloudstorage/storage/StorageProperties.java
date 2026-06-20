package dev.ratmir.cloudstorage.storage;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.storage")
public record StorageProperties(
		String endpoint,
		String bucket,
		String accessKey,
		String secretKey,
		String userRootPrefixPattern) {

	public String userRootPrefix(long userId) {
		return userRootPrefixPattern.formatted(userId);
	}
}
