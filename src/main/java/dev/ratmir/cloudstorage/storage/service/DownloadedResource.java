package dev.ratmir.cloudstorage.storage.service;

import org.springframework.core.io.InputStreamResource;
import org.springframework.http.MediaType;

public record DownloadedResource(
		String fileName,
		MediaType contentType,
		InputStreamResource body,
		long contentLength) {
}
