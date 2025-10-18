package com.project.mydrive.external.document;

import com.project.mydrive.external.document.excpetions.DocmentRetrievalException;
import com.project.mydrive.external.document.excpetions.DocumentDeletionException;
import com.project.mydrive.external.document.excpetions.DocumentStorageException;
import com.project.mydrive.external.document.model.Document;
import lombok.RequiredArgsConstructor;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
public class S3DocumentClient implements DocumentClient {

    private final S3Client s3Client;

    private final String bucketName;

    private final static String projectKey = "drive-app";

    @Override
    public Document uploadDocument(byte[] content, String contentType) throws DocumentStorageException {
        try {
            var docId = UUID.randomUUID();
            var key = getKey(docId);
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .contentType(contentType)
                    .contentLength((long) content.length)
                    .build();

            PutObjectResponse response = s3Client.putObject(request, RequestBody.fromBytes(content));

            return Document.builder()
                    .id(docId)
                    .s3Key(key)
                    .contentType(contentType)
                    .size(content.length)
                    .build();
        } catch (S3Exception ex) {
            throw new DocumentStorageException("Failed to store the document", ex);
        }
    }

    private static String getKey(UUID docId) {
        return projectKey + "/" + docId.toString();
    }

    @Override
    public Document downloadDocument(UUID id) throws DocmentRetrievalException {
        try {
            GetObjectRequest request = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(getKey(id))
                    .build();
            var obj = s3Client.getObject(request);
            var res = obj.response();
            return Document.builder()
                    .content(obj.readAllBytes())
                    .contentType(res.contentType())
                    .id(id)
                    .contentLength(res.contentLength())
                    .build();

        } catch (NoSuchKeyException ex) {
            throw new DocmentRetrievalException("No document exists with id: " + id, ex);
        } catch (S3Exception ex) {
            throw new DocmentRetrievalException("Failed to retrieve document with id: " + id, ex);
        } catch (IOException ex) {
            throw new DocmentRetrievalException("Something went wrong reading the document with id" + id, ex);
        }
    }

    @Override
    public void deleteDocuments(List<UUID> ids) throws DocumentDeletionException {
        if(ids == null || ids.isEmpty()) {
            return; // Nothing to delete
        }
        try {
            DeleteObjectsRequest request = DeleteObjectsRequest.builder()
                    .bucket(bucketName)
                    .delete(builder -> builder.objects(
                            ids.stream()
                                    .map(id -> ObjectIdentifier.builder().key(getKey(id)).build()).toList()))
                    .build();

            s3Client.deleteObjects(request);
        } catch (S3Exception ex) {
            throw new DocumentDeletionException("Something went wrong deleting documents", ex);
        }
    }
}
