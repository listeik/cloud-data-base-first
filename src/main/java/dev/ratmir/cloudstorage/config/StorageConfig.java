package dev.ratmir.cloudstorage.config;

import dev.ratmir.cloudstorage.storage.StorageProperties;
import io.minio.MinioClient;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class StorageConfig {

	@Bean
	MinioClient minioClient(StorageProperties properties) {
		return MinioClient.builder()
				.endpoint(properties.endpoint())
				.credentials(properties.accessKey(), properties.secretKey())
				.build();
	}
}
