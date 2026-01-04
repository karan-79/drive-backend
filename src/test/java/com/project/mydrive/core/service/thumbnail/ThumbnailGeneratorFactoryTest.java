package com.project.mydrive.core.service.thumbnail;

import com.project.mydrive.core.domain.FileType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

class ThumbnailGeneratorFactoryTest {

    private final ThumbnailGeneratorFactory factory = new ThumbnailGeneratorFactory();

    @Test
    void instanceGeneration() {
        var img_gen = factory.getInstance(FileType.Image);
        assertThat(img_gen).isInstanceOf(ImageThumbnailGenerator.class);


        var pdf_gen = factory.getInstance(FileType.Pdf);
        assertThat(pdf_gen).isInstanceOf(PdfThumbnailGenerator.class);
    }

    @Test void throwForUnImplementedFileType() {
        assertThatThrownBy(() -> factory.getInstance(FileType.Video))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Not implemented");
    }

}