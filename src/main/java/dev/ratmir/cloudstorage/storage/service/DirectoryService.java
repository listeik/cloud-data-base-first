package dev.ratmir.cloudstorage.storage.service;

import java.util.List;

import dev.ratmir.cloudstorage.storage.api.ResourceResponse;

public interface DirectoryService {

	List<ResourceResponse> list(String path);

	ResourceResponse create(String path);
}
