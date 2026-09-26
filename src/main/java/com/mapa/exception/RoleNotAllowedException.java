package com.mapa.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.FORBIDDEN)
public class RoleNotAllowedException extends RuntimeException {

    public RoleNotAllowedException(String message) {
        super(message);
    }
}
