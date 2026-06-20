package dev.ratmir.cloudstorage.storage.service;

import java.util.List;

import dev.ratmir.cloudstorage.storage.api.ResourceResponse;

import org.springframework.web.multipart.MultipartFile;

public interface ResourceService {

	ResourceResponse get(String path);

	void delete(String path);

	DownloadedResource download(String path);

	ResourceResponse move(String from, String to);

	List<ResourceResponse> search(String query);

	List<ResourceResponse> upload(String path, List<MultipartFile> files);
}
