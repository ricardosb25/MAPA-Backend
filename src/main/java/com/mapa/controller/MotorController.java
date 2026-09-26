package com.mapa.controller;

import com.mapa.dto.MotorRequestDTO;
import com.mapa.dto.MotorResponseDTO;
import com.mapa.dto.PageResponseDTO;
import com.mapa.service.MotorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/motores")
@RequiredArgsConstructor
@Tag(name = "Motores", description = "Endpoints para gerenciamento e consulta de motores")
public class MotorController {

    private final MotorService motorService;

    @GetMapping
    @Operation(
            summary = "Listar motores com paginação",
            description = "Retorna os motores cadastrados de forma paginada. "
                    + "Parâmetros: page (índice da página, iniciando em 0), "
                    + "size (itens por página, máximo 100) e "
                    + "sort (ex.: id,asc | name,desc | createdAt,desc). "
                    + "Valores não numéricos em page/size são ignorados (usa-se o padrão) "
                    + "e size acima de 100 é limitado a 100.")
    @ApiResponse(responseCode = "200", description = "Página de motores obtida com sucesso")
    @ApiResponse(responseCode = "400", description = "Parâmetros de paginação ou ordenação inválidos")
    public ResponseEntity<PageResponseDTO<MotorResponseDTO>> findAll(
            @ParameterObject @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.ASC) Pageable pageable) {

        PageResponseDTO<MotorResponseDTO> enginesPage = motorService.findAll(pageable);
        return ResponseEntity.ok(enginesPage);
    }

    @GetMapping("/{engineId}")
    @Operation(summary = "Buscar motor por ID", description = "Retorna os detalhes de um motor pelo seu identificador")
    @ApiResponse(responseCode = "200", description = "Motor encontrado com sucesso")
    @ApiResponse(responseCode = "404", description = "Motor não encontrado")
    public ResponseEntity<MotorResponseDTO> findById(@PathVariable Long engineId) {
        MotorResponseDTO engine = motorService.findById(engineId);
        return ResponseEntity.ok(engine);
    }

    @PostMapping
    @Operation(
            summary = "Cadastrar motor",
            description = "Cria um novo motor no catálogo. Acesso restrito a administradores.")
    @ApiResponse(responseCode = "201", description = "Motor cadastrado com sucesso")
    @ApiResponse(responseCode = "400", description = "Dados do motor inválidos")
    @ApiResponse(responseCode = "403", description = "Acesso negado: apenas administradores podem cadastrar motores")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<MotorResponseDTO> create(@Valid @RequestBody MotorRequestDTO motorRequest) {
        MotorResponseDTO createdEngine = motorService.create(motorRequest);

        URI resourceLocation = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{engineId}")
                .buildAndExpand(createdEngine.id())
                .toUri();

        return ResponseEntity.created(resourceLocation).body(createdEngine);
    }

    @PutMapping("/{engineId}")
    @Operation(
            summary = "Atualizar motor",
            description = "Atualiza todos os dados de um motor existente. Acesso restrito a administradores.")
    @ApiResponse(responseCode = "200", description = "Motor atualizado com sucesso")
    @ApiResponse(responseCode = "400", description = "Dados do motor inválidos")
    @ApiResponse(responseCode = "403", description = "Acesso negado: apenas administradores podem editar motores")
    @ApiResponse(responseCode = "404", description = "Motor não encontrado")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<MotorResponseDTO> update(
            @PathVariable Long engineId,
            @Valid @RequestBody MotorRequestDTO motorRequest) {

        MotorResponseDTO updatedEngine = motorService.update(engineId, motorRequest);
        return ResponseEntity.ok(updatedEngine);
    }

    @DeleteMapping("/{engineId}")
    @Operation(summary = "Excluir motor", description = "Remove um motor do catálogo pelo seu identificador")
    @ApiResponse(responseCode = "204", description = "Motor excluído com sucesso")
    @ApiResponse(responseCode = "403", description = "Acesso negado: apenas administradores podem excluir motores")
    @ApiResponse(responseCode = "404", description = "Motor não encontrado")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long engineId) {
        motorService.delete(engineId);
        return ResponseEntity.noContent().build();
    }
}
