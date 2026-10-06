package com.mapa.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class AlreadyEnrolledException extends RuntimeException {

    public AlreadyEnrolledException(String message) {
        super(message);
    }
}