package com.project.mydrive.core.service;

import com.project.mydrive.core.domain.File;
import com.project.mydrive.core.repository.FileRepository;
import com.project.mydrive.core.service.thumbnail.ThumbnailGeneratorFactory;
import com.project.mydrive.external.document.DocumentClient;
import com.project.mydrive.external.document.excpetions.DocumentStorageException;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;

@Service
@RequiredArgsConstructor
public class ThumbnailGenerationService {
    private final FileRepository fileRepository;
    private final DocumentClient documentClient;
    private final ThumbnailGeneratorFactory thumbnailGeneratorFactory;

    @Async
    public void generateThumbnailAsync(File file, InputStream fileStream, String contentType) {
        if (!file.getFileType().getCapability().preview()) return; // TODO logs

        var generator = thumbnailGeneratorFactory.getInstance(file.getFileType());

        try {
            var thumbnail = generator.generate(fileStream);

            var uploadedThumb = documentClient.uploadDocument(thumbnail, contentType);

            file.setThumbnailRef(uploadedThumb.getId());
            fileRepository.save(file);
        } catch (DocumentStorageException e) {
            // TODO
            throw new RuntimeException(e);
        }
    }
}
