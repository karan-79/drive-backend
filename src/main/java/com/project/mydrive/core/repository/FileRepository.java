package com.project.mydrive.core.repository;

import com.project.mydrive.core.domain.File;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FileRepository extends JpaRepository<File,Long> {
}
