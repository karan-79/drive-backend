package com.project.mydrive.core.repository;

import com.project.mydrive.core.domain.FileMetadata;
import org.springframework.data.repository.CrudRepository;

public interface FileMetadataRepository extends CrudRepository<FileMetadata, Long> {
}
