package dev.ratmir.cloudstorage.storage.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;

import org.springframework.stereotype.Component;

@Component
class StorageMetrics {

	private final MeterRegistry registry;
	private final Counter uploadedFiles;
	private final DistributionSummary uploadedBytes;
	private final Counter downloadedResources;
	private final DistributionSummary downloadedBytes;

	StorageMetrics(MeterRegistry registry) {
		this.registry = registry;
		this.uploadedFiles = Counter.builder("cloud.storage.files.uploaded")
				.description("Number of files uploaded successfully")
				.register(registry);
		this.uploadedBytes = DistributionSummary.builder("cloud.storage.file.upload.size")
				.baseUnit("bytes")
				.description("Size of successfully uploaded files")
				.register(registry);
		this.downloadedResources = Counter.builder("cloud.storage.resources.downloaded")
				.description("Number of resources prepared for download")
				.register(registry);
		this.downloadedBytes = DistributionSummary.builder("cloud.storage.resource.download.size")
				.baseUnit("bytes")
				.description("Size of resources prepared for download")
				.register(registry);
	}

	void recordUpload(long bytes) {
		uploadedFiles.increment();
		uploadedBytes.record(bytes);
	}

	void recordDownload(long bytes) {
		downloadedResources.increment();
		downloadedBytes.record(bytes);
	}

	void recordUploadRejected(String reason) {
		Counter.builder("cloud.storage.uploads.rejected")
				.description("Number of uploads rejected by storage limits")
				.tag("reason", reason)
				.register(registry)
				.increment();
	}
}
