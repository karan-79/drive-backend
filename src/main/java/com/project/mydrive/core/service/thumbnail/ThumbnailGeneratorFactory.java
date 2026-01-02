package com.project.mydrive.core.service.thumbnail;

import com.project.mydrive.core.domain.FileType;
import org.springframework.stereotype.Component;

@Component
public class ThumbnailGeneratorFactory {

    public ThumbnailGenerator getInstance(FileType fileType) {
        return switch (fileType) {
            case Image -> new ImageThumbnailGenerator();
            case Pdf -> new PdfThumbnailGenerator();
            default -> throw new RuntimeException("Not implemented");
        };
    }

}
