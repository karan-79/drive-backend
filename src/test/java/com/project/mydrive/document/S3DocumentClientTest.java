package com.project.mydrive.document;

import com.project.mydrive.BaseIntegrationTests;
import com.project.mydrive.external.document.model.Document;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class S3DocumentClientTest extends BaseIntegrationTests {


    @Test
    void shouldUploadAndDownloadDocument() {
        // Given
        byte[] content = "Hello, S3!".getBytes();
        String contentType = "text/plain";

        // When
        Document uploadedDocument = documentClient.uploadDocument(content, contentType);

        // Then
        assertThat(uploadedDocument).isNotNull();
        assertThat(uploadedDocument.getId()).isNotNull();
        assertThat(uploadedDocument.getS3Key()).contains(uploadedDocument.getId().toString());
        assertThat(uploadedDocument.getContentType()).isEqualTo(contentType);
        assertThat(uploadedDocument.getSize()).isEqualTo(content.length);

        // When
        Document downloadedDocument = documentClient.downloadDocument(uploadedDocument.getId());

        // Then
        assertThat(downloadedDocument).isNotNull();
        assertThat(downloadedDocument.getContent()).isEqualTo(content);
        assertThat(downloadedDocument.getContentType()).isEqualTo(contentType);
        assertThat(downloadedDocument.getId()).isEqualTo(uploadedDocument.getId());

        // When
        documentClient.deleteDocuments(List.of(uploadedDocument.getId()));

        // Then (verify deletion - optional, might require more advanced testing)
        // For now, we assume deletion is successful if no exception is thrown.
    }
}