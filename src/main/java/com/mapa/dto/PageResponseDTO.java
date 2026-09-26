package com.mapa.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.data.domain.Page;

import java.util.List;

@Schema(name = "PageResponse", description = "Envelope padronizado das respostas paginadas da API")
public record PageResponseDTO<T>(

        @Schema(description = "Itens pertencentes à página atual")
        List<T> content,

        @Schema(description = "Índice da página atual, iniciando em zero", example = "0")
        int page,

        @Schema(description = "Quantidade de itens por página", example = "20")
        int size,

        @Schema(description = "Total de itens disponíveis", example = "42")
        long totalElements,

        @Schema(description = "Total de páginas disponíveis", example = "3")
        int totalPages,

        @Schema(description = "Indica se a página atual é a primeira", example = "true")
        boolean first,

        @Schema(description = "Indica se a página atual é a última", example = "false")
        boolean last,

        @Schema(description = "Indica se existe próxima página", example = "true")
        boolean hasNext,

        @Schema(description = "Indica se existe página anterior", example = "false")
        boolean hasPrevious,

        @Schema(description = "Ordenação aplicada na consulta", example = "id: ASC")
        String sort
) {

    public static <T> PageResponseDTO<T> fromPage(Page<T> page) {
        return new PageResponseDTO<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast(),
                page.hasNext(),
                page.hasPrevious(),
                page.getSort().toString());
    }
}
