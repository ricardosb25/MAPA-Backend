package com.mapa.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class InvalidResetTokenException extends RuntimeException {

    public InvalidResetTokenException() {
        super("Link de redefinição inválido ou expirado");
    }
}