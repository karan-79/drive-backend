package com.project.mydrive.core.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class RootDirectoryModificationException extends RuntimeException {
    public RootDirectoryModificationException(String message) {
        super(message);
    }
}
