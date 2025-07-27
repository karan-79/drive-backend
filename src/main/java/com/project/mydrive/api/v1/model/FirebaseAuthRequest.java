package com.project.mydrive.api.v1.model;

public record FirebaseAuthRequest(
        String idToken,
        String firstName,
        String lastName
) {
}
