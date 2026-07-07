package dev.ratmir.cloudstorage.storage;

import java.util.regex.Pattern;

public record ResourcePath(String value) {

	private static final Pattern FORBIDDEN_SEGMENT = Pattern.compile("(^|/)\\.\\.?(/|$)");

	public ResourcePath {
		if (value == null) {
			throw new IllegalArgumentException("Path must not be null");
		}
		if (!value.isEmpty()
				&& (value.startsWith("/")
						|| value.contains("\\")
						|| value.contains("//")
						|| FORBIDDEN_SEGMENT.matcher(value).find())) {
			throw new IllegalArgumentException("Path must be relative and normalized");
		}
	}

	public static ResourcePath resource(String value) {
		if (value == null || value.isBlank()) {
			throw new IllegalArgumentException("Path must not be blank");
		}
		return new ResourcePath(value);
	}

	public static ResourcePath file(String value) {
		var path = resource(value);
		if (path.directory()) {
			throw new IllegalArgumentException("File path must not end with slash");
		}
		return path;
	}

	public static ResourcePath directory(String value, boolean allowRoot) {
		if (value == null || value.isEmpty()) {
			if (allowRoot) {
				return new ResourcePath("");
			}
			throw new IllegalArgumentException("Directory path must not be blank");
		}
		var path = new ResourcePath(value);
		if (!path.directory()) {
			throw new IllegalArgumentException("Directory path must end with slash");
		}
		return path;
	}

	public boolean directory() {
		return value.endsWith("/");
	}
}
