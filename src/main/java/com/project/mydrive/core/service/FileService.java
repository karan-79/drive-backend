package com.project.mydrive.core.service;

import com.project.mydrive.api.v1.model.APIFile;
import com.project.mydrive.api.v1.model.FileResource;
import com.project.mydrive.api.v1.model.UpdateFileRequest;
import com.project.mydrive.core.domain.*;
import com.project.mydrive.core.exception.*;
import com.project.mydrive.core.repository.DirectoryRepository;
import com.project.mydrive.core.repository.FileRepository;
import com.project.mydrive.core.service.thumbnail.ThumbnailGenerator;
import com.project.mydrive.external.document.DocumentClient;
import com.project.mydrive.external.document.excpetions.DocmentRetrievalException;
import com.project.mydrive.external.document.excpetions.DocumentStorageException;
import com.project.mydrive.external.document.model.Document;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.apache.tika.Tika;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

@Service
@RequiredArgsConstructor
public class FileService {

    private final FileRepository fileRepository;
    private final CleanUpService cleanUpService;
    private final DirectoryRepository directoryRepository;
    private final DocumentClient documentClient;
    private final ThumbnailGenerationService thumbnailGenerationService;


    @Transactional
    public APIFile save(MultipartFile file, Long parentDirId, User user) {

        if (file.isEmpty()) {
            throw new EmptyFileException("File was empty");
        }

        var dir = parentDirId == null
                ? directoryRepository.getDirectoryByOwnerAndParentDirectoryIsNullAndIsDeletedIsFalse(user)
                : directoryRepository.getDirectoryByOwnerAndIdAndIsDeletedIsFalse(user, parentDirId).orElseThrow(() -> new DirectoryNotFoundException("Parent directory with ID " + parentDirId + " not found."));

        Document uploadedDocument;
        String documentContentType = extractContentType(file);

        if(!documentContentType.equalsIgnoreCase(file.getContentType())) {
            throw new IllegalStateException("Request content type does not match the actual.");
        }

        try {
            uploadedDocument = documentClient.uploadDocument(file.getBytes(), documentContentType);
        } catch (IOException | DocumentStorageException e) {
            throw new FileUploadException("Failed to upload file: " + e.getMessage(), e);
        }

        var fileToSave = new File();
        fileToSave.setName(file.getOriginalFilename());
        fileToSave.setBlobReferenceId(uploadedDocument.getId());
        fileToSave.setOwner(user);
        fileToSave.setFileType(FileType.getFileTypeFromContentType(documentContentType));
        fileToSave.setParentDirectory(dir);

        var fileMetadata = new FileMetadata();
        fileMetadata.setSize(BigInteger.valueOf(uploadedDocument.getSize()));
        fileMetadata.setMimeType(uploadedDocument.getContentType());
        fileMetadata.setExtension(getFileExtension(file.getOriginalFilename()));
        fileMetadata.setFile(fileToSave);
        fileToSave.setFileMetadata(fileMetadata);

        var savedFile = fileRepository.save(fileToSave);

        thumbnailGenerationService.generateThumbnailAsync(savedFile.getId(), file);

        return toAPIFile(savedFile);
    }

    private String extractContentType(MultipartFile file) {
        try(InputStream is = file.getInputStream()) {
            return new Tika().detect(is);
        } catch (IOException e) {
            throw new FileUploadException("Failed to read file content type", e);
        }
    }

    public FileResource loadThumbnail(Long fileId, User user) {
        var file = fileRepository.getFileByOwnerAndIdAndIsDeletedIsFalse(user, fileId).orElseThrow(fileNotFoundException(fileId));

        Document downloadedDocument;
        try {
            downloadedDocument = documentClient.downloadDocument(file.getThumbnailRef());
        } catch (DocmentRetrievalException e) {
            throw new FileDownloadException("Failed to retrieve blob for thumbnailID " + file.getThumbnailRef() + ": " + e.getMessage(), e);
        }


        return new FileResource(file.getName(), ThumbnailGenerator.THUMBNAIL_MIME_TYPE, new ByteArrayResource(downloadedDocument.getContent()));
    }

    public FileResource downloadFile(UUID blobRef, User user) {
        var file = fileRepository.getFileByBlobReferenceId(blobRef).orElseThrow(fileNotFoundException("File with blob reference ID " + blobRef + " not found."));

        if (!file.getOwner().getId().equals(user.getId())) {
            throw new UnauthorizedFileAccessException("File does not belong to user");
        }

        Document downloadedDocument;
        try {
            downloadedDocument = documentClient.downloadDocument(blobRef);
        } catch (DocmentRetrievalException e) {
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
        var file = fileRepository.getFileByBlobReferenceId(blobRef).orElseThrow(fileNotFoundException("File with blob reference ID " + blobRef + " not found."));

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
                file.getFileMetadata().getSize(),
                file.getFileType(),
                file.getFileType().getCapability(),
                Optional.ofNullable(file.getThumbnailRef()).map(id -> "/" + id).orElse(null)
        );
    }

    private String getFileExtension(String fileName) {
        if (fileName != null && fileName.contains(".")) {
            return fileName.substring(fileName.lastIndexOf(".") + 1);
        }
        return null;
    }

    public APIFile update(Long fileId, UpdateFileRequest fileRequest, User user) {
        var file = fileRepository.getFileByOwnerAndIdAndIsDeletedIsFalse(user, fileId).orElseThrow(fileNotFoundException(fileId));

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

    private static Supplier<FileNotFoundException> fileNotFoundException(String fileId) {
        return () -> new FileNotFoundException(fileId);
    }

    private static Supplier<FileNotFoundException> fileNotFoundException(Long fileId) {
        return () -> new FileNotFoundException("File with ID " + fileId + " not found for user.");
    }

    public List<APIFile> getFilesUnder(Long parentDirId, User user) {
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
