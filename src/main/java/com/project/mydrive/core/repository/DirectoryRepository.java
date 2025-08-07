package com.project.mydrive.core.repository;

import com.project.mydrive.core.domain.Directory;
import com.project.mydrive.core.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface DirectoryRepository extends JpaRepository<Directory, Long> {
    Optional<Directory> getDirectoryByOwnerAndIdAndIsDeletedIsFalse(User owner, Long id);
    List<Directory> findAllByOwner(User owner);

    Directory getDirectoryByOwnerAndParentDirectoryIsNullAndIsDeletedIsFalse(User owner);

    @Query("Select d from Directory d where d.isDeleted = true")
    List<Directory> getAllDeleted();
}
