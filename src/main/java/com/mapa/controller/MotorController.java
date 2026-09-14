package com.mapa.controller;

import com.mapa.dto.MotorResponseDTO;
import com.mapa.service.MotorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/motores")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
@Tag(name = "Motores", description = "Endpoints para gerenciamento e consulta de motores")
public class MotorController {

    private final MotorService motorService;

    @GetMapping
    @Operation(summary = "Listar todos os motores", description = "Retorna a lista completa de motores cadastrados")
    @ApiResponse(responseCode = "200", description = "Lista de motores obtida com sucesso")
    public ResponseEntity<List<MotorResponseDTO>> findAll() {
        List<MotorResponseDTO> enginesList = motorService.findAll();
        return ResponseEntity.ok(enginesList);
    }

    @GetMapping("/{engineId}")
    @Operation(summary = "Buscar motor por ID", description = "Retorna os detalhes de um motor pelo seu identificador")
    @ApiResponse(responseCode = "200", description = "Motor encontrado com sucesso")
    @ApiResponse(responseCode = "404", description = "Motor não encontrado")
    public ResponseEntity<MotorResponseDTO> findById(@PathVariable Long engineId) {
        MotorResponseDTO engine = motorService.findById(engineId);
        return ResponseEntity.ok(engine);
    }
}
