package com.project.mydrive.api.v1.model;

import java.math.BigInteger;
import java.util.UUID;

public record APIFile(Long id, String name, UUID blobRef, Long parentDirId, BigInteger size) {
}
