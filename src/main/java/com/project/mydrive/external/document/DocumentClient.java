package com.project.mydrive.external.document;

import com.project.mydrive.external.document.excpetions.DocmentRetrievalException;
import com.project.mydrive.external.document.excpetions.DocumentDeletionException;
import com.project.mydrive.external.document.excpetions.DocumentStorageException;
import com.project.mydrive.external.document.model.Document;

import java.util.List;
import java.util.UUID;

public interface DocumentClient {

    Document uploadDocument(byte[] content, String contentType) throws DocumentStorageException;

    Document downloadDocument(UUID id) throws DocmentRetrievalException;

    void deleteDocuments(List<UUID> ids) throws DocumentDeletionException;
}
