package com.project.mydrive.core.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class FirebaseTokenException extends RuntimeException {
    public FirebaseTokenException(String message) {
        super(message);
    }
}
