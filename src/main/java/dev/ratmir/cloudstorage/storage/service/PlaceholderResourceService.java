package dev.ratmir.cloudstorage.storage.service;

import java.util.List;

import dev.ratmir.cloudstorage.api.FeatureNotImplementedException;
import dev.ratmir.cloudstorage.storage.api.ResourceResponse;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
class PlaceholderResourceService implements ResourceService {

	@Override
	public ResourceResponse get(String path) {
		throw new FeatureNotImplementedException("Resource lookup is not implemented yet");
	}

	@Override
	public void delete(String path) {
		throw new FeatureNotImplementedException("Resource deletion is not implemented yet");
	}

	@Override
	public DownloadedResource download(String path) {
		throw new FeatureNotImplementedException("Resource download is not implemented yet");
	}

	@Override
	public ResourceResponse move(String from, String to) {
		throw new FeatureNotImplementedException("Resource move is not implemented yet");
	}

	@Override
	public List<ResourceResponse> search(String query) {
		throw new FeatureNotImplementedException("Resource search is not implemented yet");
	}

	@Override
	public List<ResourceResponse> upload(String path, List<MultipartFile> files) {
		throw new FeatureNotImplementedException("Resource upload is not implemented yet");
	}
}
