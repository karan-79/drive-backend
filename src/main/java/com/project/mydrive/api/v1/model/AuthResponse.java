package com.project.mydrive.api.v1.model;

public record AuthResponse(
        Boolean isAuthenticated,
        String token,
        String message) {
}

