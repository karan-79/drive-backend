package com.project.mydrive.core.domain;

import jakarta.persistence.*;

import java.math.BigInteger;
import java.util.UUID;

@Entity
@Table(name = "FILE_METADATA")
public class FileMetadata {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @OneToOne
    @JoinColumn(name = "FILE_ID")
    File file;

    BigInteger size;

    String mimeType;

    String extension;

    Integer width;
    Integer height;
    Float duration;
    String encoding;
    Integer bitrate;
    Float frameRate;
    UUID thumbnailId;
}
