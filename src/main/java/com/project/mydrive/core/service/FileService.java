package com.project.mydrive.core.service;

import com.project.mydrive.api.v1.model.APIFile;
import com.project.mydrive.api.v1.model.FileResource;
import com.project.mydrive.api.v1.model.UpdateFileRequest;
import com.project.mydrive.core.crons.DeletionCron;
import com.project.mydrive.core.domain.Directory;
import com.project.mydrive.core.domain.File;
import com.project.mydrive.core.domain.FileMetadata;
import com.project.mydrive.core.domain.User;
import com.project.mydrive.core.exception.DirectoryNotFoundException;
import com.project.mydrive.core.exception.EmptyFileException;
import com.project.mydrive.core.exception.FileDownloadException;
import com.project.mydrive.core.exception.FileNotFoundException;
import com.project.mydrive.core.exception.FileUploadException;
import com.project.mydrive.core.exception.UnauthorizedFileAccessException;
import com.project.mydrive.core.exception.UserNotFoundException;
import com.project.mydrive.core.repository.DirectoryRepository;
import com.project.mydrive.core.repository.FileMetadataRepository;
import com.project.mydrive.core.repository.FileRepository;
import com.project.mydrive.core.repository.UserRepository;

import com.project.mydrive.external.document.DocumentClient;
import com.project.mydrive.external.document.model.Document;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigInteger;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FileService {

    private final FileRepository fileRepository;
    private final FileMetadataRepository fileMetadataRepository;
    private final CleanUpService cleanUpService;
    private final DirectoryRepository directoryRepository;
    private final UserRepository userRepository;
    private final DocumentClient documentClient;

    @Transactional
    public APIFile save(MultipartFile file, Long parentDirId, UUID userId) {

        if (file.isEmpty()) {
            throw new EmptyFileException("File was empty");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User with ID " + userId + " not found."));
        var dir = parentDirId == null
                ? directoryRepository.getDirectoryByOwnerAndParentDirectoryIsNullAndIsDeletedIsFalse(user)
                : directoryRepository.getDirectoryByOwnerAndIdAndIsDeletedIsFalse(user, parentDirId).orElseThrow(() -> new DirectoryNotFoundException("Parent directory with ID " + parentDirId + " not found."));

        Document uploadedDocument;
        try {
            uploadedDocument = documentClient.uploadDocument(file.getBytes(), file.getContentType());
        } catch (IOException e) {
            throw new FileUploadException("Failed to upload file: " + e.getMessage(), e);
        }

        var fileToSave = new File();
        fileToSave.setName(file.getOriginalFilename());
        fileToSave.setBlobReferenceId(uploadedDocument.getId());
        fileToSave.setOwner(user);
        fileToSave.setParentDirectory(dir);

        var fileMetadata = new FileMetadata();
        fileMetadata.setSize(BigInteger.valueOf(uploadedDocument.getSize()));
        fileMetadata.setMimeType(uploadedDocument.getContentType());
        fileMetadata.setExtension(getFileExtension(file.getOriginalFilename()));
        fileMetadata.setFile(fileToSave);
        fileToSave.setFileMetadata(fileMetadata);

        var savedFile = fileRepository.save(fileToSave);

        return toAPIFile(savedFile);
    }

    public FileResource downloadFile(UUID blobRef, UUID userId) {
        var user = userRepository.findById(userId).orElseThrow();

        var file = fileRepository.getFileByBlobReferenceId(blobRef).orElseThrow(() -> new FileNotFoundException("File with blob reference ID " + blobRef + " not found."));

        if (!file.getOwner().getId().equals(user.getId())) {
            throw new UnauthorizedFileAccessException("File does not belong to user");
        }

        Document downloadedDocument;
        try {
            downloadedDocument = documentClient.downloadDocument(blobRef);
        } catch (Exception e) {
            throw new FileDownloadException("Failed to download file with blob reference ID " + blobRef + ": " + e.getMessage(), e);
        }

        return new FileResource(file.getName(), downloadedDocument.getContentType(), new ByteArrayResource(downloadedDocument.getContent()));
    }

    /**
     * LEARNED: Eventual consistency
     * Don't delete files immediately, instead mark them as deleted.
     * and a cron would handle stuff about actual deletion
     */

    public void deleteFile(UUID blobRef, User user) {
        // validate if user owns the file
        var file = fileRepository.getFileByBlobReferenceId(blobRef).orElseThrow(() -> new FileNotFoundException("File with blob reference ID " + blobRef + " not found."));

        if (!file.getOwner().getId().equals(user.getId())) {
            throw new UnauthorizedFileAccessException("File does not belong to user");
        }

        file.setDeleted(true);
        fileRepository.save(file);
        cleanUpService.deleteFilesOnBlobAsync();
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
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User with ID " + userId + " not found."));

        var file = fileRepository.getFileByOwnerAndIdAndIsDeletedIsFalse(user, fileId).orElseThrow(() -> new FileNotFoundException("File with ID " + fileId + " not found for user."));

        if (!fileRequest.name().equals(file.getName())) {
            file.setName(fileRequest.name());
        }

        if (file.getParentDirectory() != null
                && fileRequest.parentDirId() != null
                && !file.getParentDirectory().getId().equals(fileRequest.parentDirId())
        ) {
            var dir = directoryRepository.getDirectoryByOwnerAndIdAndIsDeletedIsFalse(user, fileRequest.parentDirId()).orElseThrow(() -> new DirectoryNotFoundException("Parent directory with ID " + fileRequest.parentDirId() + " not found."));
            file.setParentDirectory(dir);
        }

        var saved = fileRepository.save(file);
        return toAPIFile(saved);
    }

    public List<APIFile> getFilesUnder(Long parentDirId, UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User with ID " + userId + " not found."));

        //TODO should be a lil different or should not be in File Service
        Directory dir = null;
        if (parentDirId == null) {
            dir = directoryRepository.getDirectoryByOwnerAndParentDirectoryIsNullAndIsDeletedIsFalse(user);
        } else {
            dir = directoryRepository.getDirectoryByOwnerAndIdAndIsDeletedIsFalse(user, parentDirId).orElseThrow(() -> new DirectoryNotFoundException("Directory with ID " + parentDirId + " not found."));
        }

        return dir.getFiles().stream().map(this::toAPIFile).toList();
    }
}
