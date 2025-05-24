package com.project.mydrive.core.repository;

import com.project.mydrive.core.domain.Directory;
import com.project.mydrive.core.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DirectoryRepository extends JpaRepository<Directory, Long> {
    Optional<Directory> getDirectoryByOwnerAndId(User owner, Long id);

    List<Directory> findAllByOwner(User owner);

    Directory getDirectoryByOwnerAndParentDirectoryIsNull(User owner);
}
