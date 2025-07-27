package com.project.mydrive.external.document.excpetions;

public class DocumentDeletionException extends RuntimeException {
    public DocumentDeletionException(String message, Throwable cause) {
        super(message, cause);
    }
}
