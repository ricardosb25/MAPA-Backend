package com.mapa.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;
import java.util.List;

@Schema(name = "ApiError", description = "Representação padronizada dos erros retornados pela API")
public record ApiErrorDTO(

        @Schema(description = "Momento em que o erro foi gerado")
        OffsetDateTime timestamp,

        @Schema(description = "Código HTTP do erro", example = "400")
        int status,

        @Schema(description = "Descrição do código HTTP", example = "Bad Request")
        String error,

        @Schema(description = "Mensagem explicando o motivo do erro", example = "Um ou mais campos da requisição são inválidos")
        String message,

        @Schema(description = "Caminho do recurso acessado", example = "/api/v1/motores")
        String path,

        @Schema(description = "Lista de falhas de validação por campo (vazia quando não houver)")
        List<FieldValidationErrorDTO> fieldErrors
) {
}
