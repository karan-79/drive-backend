package com.project.mydrive.external.document.model;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class Document {
    private UUID id;
    private String s3Key;
    private String contentType;
    private long size;
    private byte[] content;
    private long contentLength;
}
