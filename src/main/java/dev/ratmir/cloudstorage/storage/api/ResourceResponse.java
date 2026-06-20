package dev.ratmir.cloudstorage.storage.api;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ResourceResponse(String path, String name, Long size, ResourceType type) {
}
