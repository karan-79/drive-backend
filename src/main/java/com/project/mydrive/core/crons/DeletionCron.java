package com.project.mydrive.core.crons;

import com.project.mydrive.core.domain.File;
import com.project.mydrive.core.repository.DirectoryRepository;
import com.project.mydrive.core.repository.FileRepository;
import com.project.mydrive.core.repository.UserRepository;
import com.project.mydrive.external.document.DocumentClient;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DeletionCron {

    private final FileRepository fileRepository;
    private final DirectoryRepository directoryRepository;
    private final DocumentClient documentClient;

    @Scheduled(cron = "0 0 0 * * *")
    public void deleteFilesOnBlob() {
        var allDeletedFiles = fileRepository.getAllDeleted();
        var allDeletedDirectories = directoryRepository.getAllDeleted();
        documentClient.deleteDocuments(allDeletedFiles.stream().map(File::getBlobReferenceId).toList());
        fileRepository.deleteAll(allDeletedFiles);

        allDeletedDirectories.forEach(dir -> {
            fileRepository.deleteAll(dir.getFiles());
        });

        directoryRepository.deleteAll(allDeletedDirectories);
    }

    public void invokeCleanUp() {
        new Thread(this::deleteFilesOnBlob).start();
    }
}
