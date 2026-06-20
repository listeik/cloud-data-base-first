package dev.ratmir.cloudstorage.storage;

import java.util.regex.Pattern;

public record ResourcePath(String value) {

	private static final Pattern FORBIDDEN_SEGMENT = Pattern.compile("(^|/)\\.\\.?(/|$)");

	public ResourcePath {
		if (value == null || value.isBlank()) {
			throw new IllegalArgumentException("Path must not be blank");
		}
		if (value.startsWith("/") || value.contains("\\") || value.contains("//") || FORBIDDEN_SEGMENT.matcher(value).find()) {
			throw new IllegalArgumentException("Path must be relative and normalized");
		}
	}

	public boolean directory() {
		return value.endsWith("/");
	}
}
