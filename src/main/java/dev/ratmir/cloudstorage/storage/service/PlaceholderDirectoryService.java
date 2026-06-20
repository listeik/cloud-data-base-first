package dev.ratmir.cloudstorage.storage.service;

import java.util.List;

import dev.ratmir.cloudstorage.api.FeatureNotImplementedException;
import dev.ratmir.cloudstorage.storage.api.ResourceResponse;

import org.springframework.stereotype.Service;

@Service
class PlaceholderDirectoryService implements DirectoryService {

	@Override
	public List<ResourceResponse> list(String path) {
		throw new FeatureNotImplementedException("Directory listing is not implemented yet");
	}

	@Override
	public ResourceResponse create(String path) {
		throw new FeatureNotImplementedException("Directory creation is not implemented yet");
	}
}
