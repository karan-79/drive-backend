package com.project.mydrive.core.service.thumbnail;

import java.io.InputStream;

public interface ThumbnailGenerator {
    int THUMBNAIL_SIZE = 200;
    String THUMBNAIL_FORMAT = "png";
    String THUMBNAIL_MIME_TYPE = "image/png";

    byte[] generate(InputStream content);
}
