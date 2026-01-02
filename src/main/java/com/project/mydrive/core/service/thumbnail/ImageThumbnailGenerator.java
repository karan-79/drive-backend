package com.project.mydrive.core.service.thumbnail;

import net.coobird.thumbnailator.Thumbnails;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class ImageThumbnailGenerator implements ThumbnailGenerator {

    @Override
    public byte[] generate(InputStream content) {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        try {
            Thumbnails.of(content)
                    .size(THUMBNAIL_SIZE, THUMBNAIL_SIZE)
                    .outputFormat(THUMBNAIL_FORMAT)
                    .toOutputStream(os);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return os.toByteArray();
    }
}
