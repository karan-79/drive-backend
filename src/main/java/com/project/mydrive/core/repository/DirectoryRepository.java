package com.project.mydrive.core.repository;

import com.project.mydrive.core.domain.Directory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DirectoryRepository extends JpaRepository<Directory, Long> {
}
