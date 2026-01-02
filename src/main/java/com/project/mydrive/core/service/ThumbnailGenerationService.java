package com.project.mydrive.core.service;

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
    public void generateThumbnailAsync(Long fileId, MultipartFile multipartFile) {
        var file = fileRepository.findById(fileId).orElseThrow();

        if (!file.getFileType().getCapability().preview()) return; // TODO logs

        var generator = thumbnailGeneratorFactory.getInstance(file.getFileType());

        try (InputStream is = multipartFile.getInputStream()) {
            var thumbnail = generator.generate(is);

            var uploadedThumb = documentClient.uploadDocument(thumbnail, multipartFile.getContentType());

            file.setThumbnailRef(uploadedThumb.getId());
            fileRepository.save(file);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to generate thumbnail for file " + file.getId(), e);
        } catch (DocumentStorageException e) {
            // TODO
            throw new RuntimeException(e);
        }
    }
}
