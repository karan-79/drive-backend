package com.project.mydrive.api.v1.model;

public record CreateDirectoryRequest(String name, Long parentDirectoryId) {
}