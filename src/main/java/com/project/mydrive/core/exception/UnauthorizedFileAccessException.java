package com.project.mydrive.core.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.FORBIDDEN)
public class UnauthorizedFileAccessException extends RuntimeException {
    public UnauthorizedFileAccessException(String message) {
        super(message);
    }
}
