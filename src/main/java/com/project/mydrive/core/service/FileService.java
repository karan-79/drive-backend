package com.project.mydrive.core.service;

import com.project.mydrive.core.repository.FileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class FileService {

    private final FileRepository fileRepository;

    public void save(MultipartFile file, String parentDirId) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }
        if (parentDirId == null) {

        }
    }
}
