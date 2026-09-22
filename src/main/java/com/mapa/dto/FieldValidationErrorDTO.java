package com.mapa.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "FieldValidationError", description = "Falha de validação associada a um campo da requisição")
public record FieldValidationErrorDTO(

        @Schema(description = "Nome do campo inválido", example = "manufacturer")
        String field,

        @Schema(description = "Mensagem descrevendo a falha de validação", example = "O fabricante é obrigatório")
        String message
) {
}
