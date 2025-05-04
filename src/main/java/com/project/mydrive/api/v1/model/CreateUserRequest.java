package com.project.mydrive.api.v1.model;

// TODO add validations
public record CreateUserRequest(
        String username,
        String password,
        String name,
        String email
) {
}
