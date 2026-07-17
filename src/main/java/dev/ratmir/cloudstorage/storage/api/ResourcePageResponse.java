package dev.ratmir.cloudstorage.storage.api;

import java.util.List;

public record ResourcePageResponse(
		List<ResourceResponse> content,
		int page,
		int size,
		long totalElements,
		int totalPages) {

	public static ResourcePageResponse from(List<ResourceResponse> resources, int page, int size) {
		var totalElements = resources.size();
		var totalPages = totalElements == 0 ? 0 : (int) Math.ceil((double) totalElements / size);
		var fromIndex = (int) Math.min((long) page * size, totalElements);
		var toIndex = Math.min(fromIndex + size, totalElements);
		return new ResourcePageResponse(
				List.copyOf(resources.subList(fromIndex, toIndex)),
				page,
				size,
				totalElements,
				totalPages);
	}
}
