package com.project.mydrive.core.service.thumbnail;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class PdfThumbnailGenerator implements ThumbnailGenerator {
    @Override
    public byte[] generate(InputStream content) {
        ByteArrayOutputStream os = new ByteArrayOutputStream(THUMBNAIL_SIZE * THUMBNAIL_SIZE);
        // TODO find something streamable can't read all bytes for large files
        try (PDDocument document = Loader.loadPDF(content.readAllBytes())) {

            PDFRenderer renderer = new PDFRenderer(document);

            var pageBox = document.getPage(0).getMediaBox();
            float pageWidth = pageBox.getWidth();
            float pageHeight = pageBox.getHeight();

            float scale = (pageWidth > pageHeight) ? THUMBNAIL_SIZE / pageWidth : THUMBNAIL_SIZE / pageHeight;

            BufferedImage image = renderer.renderImage(0, scale);

            ImageIO.write(image, THUMBNAIL_FORMAT, os);

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return os.toByteArray();
    }
}
