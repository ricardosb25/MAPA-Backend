package com.mapa.dto.auth;

public record MessageResponseDTO(String message) {

    public static MessageResponseDTO of(String message) {
        return new MessageResponseDTO(message);
    }
}