package com.mapa.controller;

import com.mapa.domain.enums.EcuMapType;
import com.mapa.dto.EcuMapCellUpdateRequestDTO;
import com.mapa.dto.EcuMapResponseDTO;
import com.mapa.service.EcuMapService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/motores/{engineId}/mapas")
@RequiredArgsConstructor
@Tag(name = "Mapas ECU", description = "Consulta e calibragem dos mapas de ignição e combustível da ECU")
public class EcuMapController {

    private final EcuMapService ecuMapService;

    @GetMapping
    @Operation(
            summary = "Obter os mapas da ECU de um motor",
            description = "Retorna os mapas de ignição e combustível do motor para o usuário autenticado. "
                    + "Inclui a grade efetiva, a grade de fábrica e os limites permitidos de edição. "
                    + "Acesso permitido a todos os perfis autenticados.")
    @ApiResponse(responseCode = "200", description = "Mapas obtidos com sucesso")
    @ApiResponse(responseCode = "401", description = "Ausência de token de autenticação")
    @ApiResponse(responseCode = "404", description = "Motor não encontrado")
    public ResponseEntity<List<EcuMapResponseDTO>> findMaps(@PathVariable Long engineId) {
        return ResponseEntity.ok(ecuMapService.findMapsByMotor(engineId));
    }

    @PutMapping("/{mapType}/celulas")
    @Operation(
            summary = "Editar uma célula do mapa",
            description = "Atualiza uma célula do mapa informando os índices de carga e RPM e o novo valor. "
                    + "O valor é validado contra os limites realistas de uma ECU automotiva. "
                    + "Acesso permitido a todos os perfis autenticados.")
    @ApiResponse(responseCode = "200", description = "Célula atualizada com sucesso")
    @ApiResponse(responseCode = "400", description = "Valor fora do intervalo permitido ou índices inválidos")
    @ApiResponse(responseCode = "401", description = "Ausência de token de autenticação")
    @ApiResponse(responseCode = "404", description = "Motor não encontrado")
    public ResponseEntity<EcuMapResponseDTO> updateCell(
            @PathVariable Long engineId,
            @PathVariable EcuMapType mapType,
            @Valid @RequestBody EcuMapCellUpdateRequestDTO cellUpdate) {

        return ResponseEntity.ok(ecuMapService.updateCell(engineId, mapType, cellUpdate));
    }

    @PostMapping("/{mapType}/restaurar")
    @Operation(
            summary = "Restaurar mapa de fábrica",
            description = "Descarta os ajustes do usuário no mapa informado e volta à grade original de fábrica. "
                    + "Acesso permitido a todos os perfis autenticados.")
    @ApiResponse(responseCode = "200", description = "Mapa restaurado para o padrão de fábrica")
    @ApiResponse(responseCode = "401", description = "Ausência de token de autenticação")
    @ApiResponse(responseCode = "404", description = "Motor não encontrado")
    public ResponseEntity<EcuMapResponseDTO> restoreFactoryMap(
            @PathVariable Long engineId,
            @PathVariable EcuMapType mapType) {

        return ResponseEntity.ok(ecuMapService.restoreFactoryMap(engineId, mapType));
    }
}
