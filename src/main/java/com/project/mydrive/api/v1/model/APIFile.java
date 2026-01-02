package com.project.mydrive.api.v1.model;

import com.project.mydrive.core.domain.FileCapability;
import com.project.mydrive.core.domain.FileType;

import java.math.BigInteger;
import java.util.UUID;

public record APIFile(
        Long id,
        String name,
        UUID blobRef,
        Long parentDirId,
        BigInteger size,
        FileType type,
        FileCapability capabilities,
        /**
         * TODO perhaps use this when you implement signed URLs to outsource the content
         * currently /fileId/thumbnail is fine
          */

        String thumbnailUrl
) {
}
