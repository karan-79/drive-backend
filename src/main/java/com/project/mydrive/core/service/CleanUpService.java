package com.project.mydrive.core.service;

import com.project.mydrive.core.domain.File;
import com.project.mydrive.core.repository.DirectoryRepository;
import com.project.mydrive.core.repository.FileRepository;
import com.project.mydrive.external.document.DocumentClient;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CleanUpService {


    private final FileRepository fileRepository;
    private final DirectoryRepository directoryRepository;
    private final DocumentClient documentClient;

    @Async
    @Transactional
    public void deleteFilesOnBlobAsync() {
        var allDeletedFiles = fileRepository.getAllDeleted();
        var allDeletedDirectories = directoryRepository.getAllDeleted();
        documentClient.deleteDocuments(allDeletedFiles.stream().map(File::getBlobReferenceId).toList());
        fileRepository.deleteAll(allDeletedFiles);

        allDeletedDirectories.forEach(dir -> {
            fileRepository.deleteAll(dir.getFiles());
        });

        directoryRepository.deleteAll(allDeletedDirectories);
    }
}
