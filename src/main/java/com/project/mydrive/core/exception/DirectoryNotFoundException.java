package com.project.mydrive.core.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class DirectoryNotFoundException extends RuntimeException {
    public DirectoryNotFoundException(String message) {
        super(message);
    }
}
