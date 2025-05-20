package com.project.mydrive.core.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigInteger;
import java.util.UUID;

@Entity
@Getter
@Setter
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
