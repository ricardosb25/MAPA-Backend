package com.mapa.controller;

import com.mapa.domain.enums.ChallengeDifficulty;
import com.mapa.dto.PageResponseDTO;
import com.mapa.dto.challenge.ChallengePerformanceDTO;
import com.mapa.dto.challenge.ChallengeRequestDTO;
import com.mapa.dto.challenge.ChallengeResponseDTO;
import com.mapa.service.ChallengeService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/challenges")
@RequiredArgsConstructor
@Tag(name = "Challenges", description = "Professores criam e atribuem desafios; alunos escolhem o desafio ativo")
public class ChallengeController {

    private final ChallengeService challengeService;

    @GetMapping
    @Operation(summary = "Listar desafios do professor")
    @ApiResponse(responseCode = "200", description = "Página de desafios obtida com sucesso")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<PageResponseDTO<ChallengeResponseDTO>> findAllByTeacher(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long classId,
            @RequestParam(required = false) ChallengeDifficulty difficulty,
            @RequestParam(required = false) Long motorId,
            @ParameterObject @PageableDefault(size = 12, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable) {

        return ResponseEntity.ok(challengeService.findAllByTeacher(search, classId, difficulty, motorId, pageable));
    }

    @GetMapping("/available")
    @Operation(summary = "Listar desafios disponíveis para o aluno")
    @ApiResponse(responseCode = "200", description = "Página de desafios obtida com sucesso")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<PageResponseDTO<ChallengeResponseDTO>> findAvailableForStudent(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long classId,
            @RequestParam(required = false) ChallengeDifficulty difficulty,
            @RequestParam(required = false) Long motorId,
            @ParameterObject @PageableDefault(size = 12, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable) {

        return ResponseEntity.ok(
                challengeService.findAvailableForStudent(search, classId, difficulty, motorId, pageable));
    }

    @GetMapping("/active")
    @Operation(summary = "Consultar o desafio ativo do aluno")
    @ApiResponse(responseCode = "200", description = "Desafio ativo encontrado")
    @ApiResponse(responseCode = "204", description = "Nenhum desafio ativo")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ChallengeResponseDTO> findActiveForStudent(@RequestParam(required = false) Long motorId) {
        ChallengeResponseDTO activeChallenge = challengeService.findActiveForStudent(motorId);
        if (activeChallenge == null) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(activeChallenge);
    }

    @PostMapping
    @Operation(summary = "Criar desafio e atribuir à turma")
    @ApiResponse(responseCode = "201", description = "Desafio criado com sucesso")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<ChallengeResponseDTO> create(@Valid @RequestBody ChallengeRequestDTO request) {
        ChallengeResponseDTO createdChallenge = challengeService.create(request);
        URI resourceLocation = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{challengeId}")
                .buildAndExpand(createdChallenge.id())
                .toUri();
        return ResponseEntity.created(resourceLocation).body(createdChallenge);
    }

    @GetMapping("/{challengeId}")
    @Operation(summary = "Buscar desafio por ID")
    @PreAuthorize("hasAnyRole('TEACHER', 'STUDENT')")
    public ResponseEntity<ChallengeResponseDTO> findById(@PathVariable Long challengeId) {
        return ResponseEntity.ok(challengeService.findById(challengeId));
    }

    @PutMapping("/{challengeId}")
    @Operation(summary = "Editar desafio")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<ChallengeResponseDTO> update(
            @PathVariable Long challengeId,
            @Valid @RequestBody ChallengeRequestDTO request) {

        return ResponseEntity.ok(challengeService.update(challengeId, request));
    }

    @DeleteMapping("/{challengeId}")
    @Operation(summary = "Excluir desafio")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<Void> delete(@PathVariable Long challengeId) {
        challengeService.delete(challengeId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{challengeId}/select")
    @Operation(summary = "Selecionar desafio ativo")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ChallengeResponseDTO> select(@PathVariable Long challengeId) {
        return ResponseEntity.ok(challengeService.selectForStudent(challengeId));
    }

    @GetMapping("/{challengeId}/performance")
    @Operation(summary = "Consultar desempenho da turma no desafio")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<ChallengePerformanceDTO> findPerformance(@PathVariable Long challengeId) {
        return ResponseEntity.ok(challengeService.findPerformance(challengeId));
    }
}
