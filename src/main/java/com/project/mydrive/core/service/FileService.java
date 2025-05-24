package com.project.mydrive.core.service;

import com.project.mydrive.api.v1.model.APIFile;
import com.project.mydrive.api.v1.model.UpdateFileRequest;
import com.project.mydrive.core.domain.Directory;
import com.project.mydrive.core.domain.File;
import com.project.mydrive.core.domain.FileMetadata;
import com.project.mydrive.core.domain.User;
import com.project.mydrive.core.repository.DirectoryRepository;
import com.project.mydrive.core.repository.FileMetadataRepository;
import com.project.mydrive.core.repository.FileRepository;
import com.project.mydrive.core.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FileService {

    private final String LOCAL_STORE_DIR = "local-blob";
    private final FileRepository fileRepository;
    private final FileMetadataRepository fileMetadataRepository;
    private final DirectoryRepository directoryRepository;

    private final UserRepository userRepository;

    public APIFile save(MultipartFile file, Long parentDirId, UUID userId) throws IOException {

        if (file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }

        User user = userRepository.findById(userId).orElseThrow();
        var dir = parentDirId == null
                ? directoryRepository.getDirectoryByOwnerAndParentDirectoryIsNull(user)
                : directoryRepository.getDirectoryByOwnerAndId(user, parentDirId).orElseThrow();

        UUID blobRef = UUID.randomUUID();
        //TODO move to a client
        try {
            var storagePath = Paths.get(LOCAL_STORE_DIR).toAbsolutePath().normalize();
            Files.createDirectories(storagePath);
            Path target = storagePath.resolve(blobRef.toString());
            Files.copy(file.getInputStream(), target);
        } catch (IOException ex) {
            ex.printStackTrace();
        }

        var fileToSave = new File();
        fileToSave.setName(file.getOriginalFilename());
        fileToSave.setBlobReferenceId(blobRef);
        fileToSave.setOwner(user);
        fileToSave.setParentDirectory(dir);

        var fileMetadata = new FileMetadata();
        fileMetadata.setSize(BigInteger.valueOf(file.getSize()));
        fileMetadata.setMimeType(file.getContentType());
        fileMetadata.setExtension(getFileExtension(file.getOriginalFilename()));
        fileMetadata.setFile(fileToSave);
        fileToSave.setFileMetadata(fileMetadata);

        var savedFile = fileRepository.save(fileToSave);

        return toAPIFile(savedFile);
    }

    private APIFile toAPIFile(File file) {
        return new APIFile(
                file.getId(),
                file.getName(),
                file.getBlobReferenceId(),
                file.getParentDirectory().getId(),
                file.getFileMetadata().getSize()
        );
    }

    private String getFileExtension(String fileName) {
        if (fileName != null && fileName.contains(".")) {
            return fileName.substring(fileName.lastIndexOf(".") + 1);
        }
        return null;
    }

    public APIFile update(Long fileId, UpdateFileRequest fileRequest, UUID userId) {
        User user = userRepository.findById(userId).orElseThrow();

        var file = fileRepository.getFileByOwnerAndId(user, fileId).orElseThrow();

        if (!fileRequest.name().equals(file.getName())) {
            file.setName(fileRequest.name());
        }

        if (file.getParentDirectory() != null
                && fileRequest.parentDirId() != null
                && !file.getParentDirectory().getId().equals(fileRequest.parentDirId())
        ) {
            var dir = directoryRepository.getDirectoryByOwnerAndId(user, fileRequest.parentDirId()).orElseThrow();
            file.setParentDirectory(dir);
        }

        var saved = fileRepository.save(file);
        return toAPIFile(saved);
    }

    public List<APIFile> getFilesUnder(Long parentDirId, UUID userId) {
        User user = userRepository.findById(userId).orElseThrow();

        //TODO should be a lil different or should not be in File Service

        Directory dir = null;
        if (parentDirId == null) {
            dir = directoryRepository.getDirectoryByOwnerAndParentDirectoryIsNull(user);
        } else {
            dir = directoryRepository.getDirectoryByOwnerAndId(user, parentDirId).orElseThrow();
        }

        return dir.getFiles().stream().map(this::toAPIFile).toList();
    }
}
