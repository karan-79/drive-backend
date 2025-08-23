package com.project.mydrive.core.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.util.UUID;

@Entity
@Table(name = "FILES")
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class File {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    String name;

    @Column(name = "BLOB_REFERENCE_ID")
    UUID blobReferenceId;

    boolean isStarred = false;

    @Column(name = "IS_DELETED")
    boolean isDeleted = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "PARENT_DIR")
    Directory parentDirectory;

    @OneToOne(mappedBy = "file", cascade = CascadeType.ALL)
    FileMetadata fileMetadata;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "OWNER_ID", nullable = false)
    User owner;
}
