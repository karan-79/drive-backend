package com.project.mydrive.core.domain;

public record FileCapability(
        boolean preview,
        boolean edit,
        boolean stream,
        boolean downloadable
) {
}
