package com.project.mydrive.api.v1.model;

import java.util.UUID;

public record APIUser(
        UUID id,
        String username,
        String name,
        String email,
        String storageUsed,
        String storageLimit
) {
}
