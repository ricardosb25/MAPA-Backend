package com.mapa.controller;

import com.mapa.domain.enums.Shift;
import com.mapa.dto.PageResponseDTO;
import com.mapa.dto.schoolClass.ClassStudentResponseDTO;
import com.mapa.dto.schoolClass.ClassDetailResponseDTO;
import com.mapa.dto.schoolClass.ClassJoinRequestDTO;
import com.mapa.dto.schoolClass.ClassRequestDTO;
import com.mapa.dto.schoolClass.ClassResponseDTO;
import com.mapa.service.SchoolClassService;
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
import java.util.List;

@RestController
@RequestMapping("/api/v1/classes")
@RequiredArgsConstructor
@Tag(name = "Classes",
        description = "Professores criam e gerenciam apenas as próprias turmas; "
                + "alunos buscam, entram e saem das turmas pelo código")
public class SchoolClassController {

    private final SchoolClassService classService;

    @GetMapping
    @Operation(
            summary = "Listar minhas turmas (professor) com busca e filtros",
            description = "Retorna apenas as turmas do professor autenticado, de forma paginada. "
                    + "Parâmetros: search (busca case-insensitive por nome ou código), "
                    + "turno (MORNING, AFTERNOON, EVENING ou SATURDAY), page, size e sort.")
    @ApiResponse(responseCode = "200", description = "Página de turmas obtida com sucesso")
    @ApiResponse(responseCode = "401", description = "Token ausente ou inválido")
    @ApiResponse(responseCode = "403", description = "Acesso negado: apenas professores podem listar turmas")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<PageResponseDTO<ClassResponseDTO>> findAllByTeacher(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Shift shift,
            @ParameterObject @PageableDefault(size = 20, sort = "name", direction = Sort.Direction.ASC) Pageable pageable) {

        return ResponseEntity.ok(classService.findAllByTeacher(search, shift, pageable));
    }

    @GetMapping("/mine")
    @Operation(
            summary = "Listar minhas turmas (aluno) com busca e filtros",
            description = "Retorna apenas as turmas em que o aluno autenticado está matriculado, de forma paginada. "
                    + "Parâmetros: search (busca case-insensitive por nome ou código), "
                    + "turno (MORNING, AFTERNOON, EVENING ou SATURDAY), page, size e sort.")
    @ApiResponse(responseCode = "200", description = "Página de turmas obtida com sucesso")
    @ApiResponse(responseCode = "401", description = "Token ausente ou inválido")
    @ApiResponse(responseCode = "403", description = "Acesso negado: apenas alunos podem consultar as próprias turmas")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<PageResponseDTO<ClassResponseDTO>> findAllByStudent(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Shift shift,
            @ParameterObject @PageableDefault(size = 12, sort = "name", direction = Sort.Direction.ASC) Pageable pageable) {

        return ResponseEntity.ok(classService.findAllByStudent(search, shift, pageable));
    }

    @PostMapping
    @Operation(
            summary = "Criar turma",
            description = "Cria uma turma para o professor autenticado e gera automaticamente o código único de entrada.")
    @ApiResponse(responseCode = "201", description = "Turma criada com sucesso")
    @ApiResponse(responseCode = "400", description = "Dados da turma inválidos")
    @ApiResponse(responseCode = "403", description = "Acesso negado: apenas professores podem criar turmas")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<ClassResponseDTO> create(@Valid @RequestBody ClassRequestDTO request) {
        ClassResponseDTO createdClass = classService.create(request);

        URI resourceLocation = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{classId}")
                .buildAndExpand(createdClass.id())
                .toUri();

        return ResponseEntity.created(resourceLocation).body(createdClass);
    }

    @GetMapping("/{classId}")
    @Operation(
            summary = "Buscar turma por ID",
            description = "Retorna a turma completa, incluindo a lista de alunos matriculados. "
                    + "Acesso restrito ao professor dono da turma.")
    @ApiResponse(responseCode = "200", description = "Turma encontrada com sucesso")
    @ApiResponse(responseCode = "403", description = "Acesso negado: turma de outro professor")
    @ApiResponse(responseCode = "404", description = "Turma não encontrada")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<ClassDetailResponseDTO> findById(@PathVariable Long classId) {
        return ResponseEntity.ok(classService.findById(classId));
    }

    @PutMapping("/{classId}")
    @Operation(
            summary = "Editar turma",
            description = "Atualiza nome e/ou turno de uma turma. Acesso restrito ao professor dono da turma.")
    @ApiResponse(responseCode = "200", description = "Turma atualizada com sucesso")
    @ApiResponse(responseCode = "400", description = "Dados da turma inválidos")
    @ApiResponse(responseCode = "403", description = "Acesso negado: turma de outro professor")
    @ApiResponse(responseCode = "404", description = "Turma não encontrada")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<ClassResponseDTO> update(
            @PathVariable Long classId,
            @Valid @RequestBody ClassRequestDTO request) {

        return ResponseEntity.ok(classService.update(classId, request));
    }

    @PostMapping("/{classId}/new-code")
    @Operation(
            summary = "Gerar novo código da turma",
            description = "Substitui o código de entrada atual por um novo código único, por exemplo em caso de vazamento. "
                    + "O código antigo deixa de valer imediatamente. Acesso restrito ao professor dono da turma.")
    @ApiResponse(responseCode = "200", description = "Novo código gerado com sucesso")
    @ApiResponse(responseCode = "403", description = "Acesso negado: turma de outro professor")
    @ApiResponse(responseCode = "404", description = "Turma não encontrada")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<ClassResponseDTO> regenerateCode(@PathVariable Long classId) {
        return ResponseEntity.ok(classService.regenerateCode(classId));
    }

    @DeleteMapping("/{classId}")
    @Operation(
            summary = "Excluir turma",
            description = "Remove a turma e todas as matrículas. Acesso restrito ao professor dono da turma.")
    @ApiResponse(responseCode = "204", description = "Turma excluída com sucesso")
    @ApiResponse(responseCode = "403", description = "Acesso negado: turma de outro professor")
    @ApiResponse(responseCode = "404", description = "Turma não encontrada")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<Void> delete(@PathVariable Long classId) {
        classService.delete(classId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{classId}/students")
    @Operation(
            summary = "Listar alunos da turma",
            description = "Retorna os alunos matriculados na turma. Acesso restrito ao professor dono da turma.")
    @ApiResponse(responseCode = "200", description = "Lista de alunos obtida com sucesso")
    @ApiResponse(responseCode = "403", description = "Acesso negado: turma de outro professor")
    @ApiResponse(responseCode = "404", description = "Turma não encontrada")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<List<ClassStudentResponseDTO>> findStudents(@PathVariable Long classId) {
        return ResponseEntity.ok(classService.findStudents(classId));
    }

    @DeleteMapping("/{classId}/students/{studentId}")
    @Operation(
            summary = "Remover aluno da turma",
            description = "Remove a matrícula de um aluno na turma. Acesso restrito ao professor dono da turma.")
    @ApiResponse(responseCode = "204", description = "Aluno removido com sucesso")
    @ApiResponse(responseCode = "403", description = "Acesso negado: turma de outro professor")
    @ApiResponse(responseCode = "404", description = "Turma ou matrícula não encontrada")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<Void> removeStudent(@PathVariable Long classId, @PathVariable Long studentId) {
        classService.removeStudent(classId, studentId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/join")
    @Operation(
            summary = "Entrar em uma turma pelo código",
            description = "Matricula o aluno autenticado na turma correspondente ao código informado.")
    @ApiResponse(responseCode = "200", description = "Aluno matriculado com sucesso")
    @ApiResponse(responseCode = "400", description = "Código ausente ou inválido")
    @ApiResponse(responseCode = "403", description = "Acesso negado: apenas alunos podem entrar em turmas")
    @ApiResponse(responseCode = "404", description = "Turma não encontrada para o código informado")
    @ApiResponse(responseCode = "409", description = "Aluno já matriculado na turma")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ClassResponseDTO> join(@Valid @RequestBody ClassJoinRequestDTO request) {
        return ResponseEntity.ok(classService.join(request.code()));
    }

    @DeleteMapping("/{classId}/leave")
    @Operation(
            summary = "Sair de uma turma",
            description = "Remove a matrícula do aluno autenticado na turma.")
    @ApiResponse(responseCode = "204", description = "Aluno saiu da turma com sucesso")
    @ApiResponse(responseCode = "403", description = "Acesso negado: apenas alunos podem sair de turmas")
    @ApiResponse(responseCode = "404", description = "Turma ou matrícula não encontrada")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<Void> leave(@PathVariable Long classId) {
        classService.leave(classId);
        return ResponseEntity.noContent().build();
    }
}