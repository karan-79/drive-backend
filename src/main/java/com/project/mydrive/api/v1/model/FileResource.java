package com.project.mydrive.api.v1.model;

import org.springframework.core.io.Resource;

public record FileResource(String filename, String mimeType, Resource resource) {
}
