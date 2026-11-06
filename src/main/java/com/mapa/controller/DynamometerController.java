package com.mapa.controller;

import com.mapa.dto.DynoResultDTO;
import com.mapa.dto.DynoTestResponseDTO;
import com.mapa.service.DynamometerPhysicsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/dinamometro")
@RequiredArgsConstructor
@Tag(name = "Dinamômetro Virtual", description = "Simulação de puxada do motor ativo com os mapas ajustados pelo usuário")
public class DynamometerController {

    private final DynamometerPhysicsService dynamometerPhysicsService;

    @PostMapping("/executar")
    @Operation(
            summary = "Executar puxada no dinamômetro virtual",
            description = "Simula uma puxada completa com o motor ativo do usuário e seus mapas de ignição e combustível. "
                    + "Retorna as curvas de potência, torque e AFR e o estado final do motor virtual. "
                    + "Acesso permitido a todos os perfis autenticados.")
    @ApiResponse(responseCode = "200", description = "Simulação executada e registrada no histórico")
    @ApiResponse(responseCode = "400", description = "Nenhum motor ativo selecionado pelo usuário")
    @ApiResponse(responseCode = "401", description = "Ausência de token de autenticação")
    public ResponseEntity<DynoResultDTO> runSimulation() {
        return ResponseEntity.ok(dynamometerPhysicsService.runSimulation());
    }

    @GetMapping("/historico")
    @Operation(
            summary = "Histórico de puxadas do usuário",
            description = "Retorna as tentativas registradas no dinamômetro virtual para o usuário autenticado, "
                    + "do mais recente para o mais antigo. "
                    + "Acesso permitido a todos os perfis autenticados.")
    @ApiResponse(responseCode = "200", description = "Histórico obtido com sucesso")
    @ApiResponse(responseCode = "401", description = "Ausência de token de autenticação")
    public ResponseEntity<List<DynoTestResponseDTO>> findHistory() {
        return ResponseEntity.ok(dynamometerPhysicsService.findHistory());
    }
}
