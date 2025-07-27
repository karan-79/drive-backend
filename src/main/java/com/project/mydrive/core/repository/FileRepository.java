package com.project.mydrive.core.repository;

import com.project.mydrive.core.domain.File;
import com.project.mydrive.core.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface FileRepository extends JpaRepository<File,Long> {
    Optional<File> getFileByOwnerAndId(User owner, Long id);
    Optional<File> getFileByBlobReferenceId(UUID blobReferenceId);

}
