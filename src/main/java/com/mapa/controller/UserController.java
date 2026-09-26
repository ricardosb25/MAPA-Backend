package com.mapa.controller;

import com.mapa.domain.enums.UserAuditAction;
import com.mapa.dto.PageResponseDTO;
import com.mapa.dto.auth.UserResponseDTO;
import com.mapa.dto.user.UserAuditLogResponseDTO;
import com.mapa.dto.user.UserCreateRequestDTO;
import com.mapa.dto.user.UserUpdateRequestDTO;
import com.mapa.service.UserAuditLogService;
import com.mapa.service.UserService;
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
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "Usuários",
        description = "Gestão de usuários. Administradores gerenciam qualquer conta; "
                + "usuários comuns só podem consultar e editar a própria conta.")
public class UserController {

    private final UserService userService;
    private final UserAuditLogService userAuditLogService;

    @GetMapping
    @Operation(
            summary = "Listar usuários com paginação",
            description = "Retorna os usuários cadastrados de forma paginada. Acesso restrito a administradores. "
                    + "Parâmetros: page (índice da página, iniciando em 0), "
                    + "size (itens por página, máximo 100) e sort (ex.: id,asc | email,desc).")
    @ApiResponse(responseCode = "200", description = "Página de usuários obtida com sucesso")
    @ApiResponse(responseCode = "401", description = "Token ausente ou inválido")
    @ApiResponse(responseCode = "403", description = "Acesso negado: apenas administradores podem listar usuários")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PageResponseDTO<UserResponseDTO>> findAll(
            @ParameterObject @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.ASC) Pageable pageable) {

        return ResponseEntity.ok(userService.findAll(pageable));
    }

    @PostMapping
    @Operation(
            summary = "Criar usuário",
            description = "Cria uma conta com qualquer perfil, incluindo ADMIN. Acesso restrito a administradores.")
    @ApiResponse(responseCode = "201", description = "Usuário criado com sucesso")
    @ApiResponse(responseCode = "400", description = "Dados inválidos")
    @ApiResponse(responseCode = "403", description = "Acesso negado: apenas administradores podem criar usuários")
    @ApiResponse(responseCode = "409", description = "E-mail já cadastrado")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponseDTO> create(@Valid @RequestBody UserCreateRequestDTO request) {
        UserResponseDTO createdUser = userService.create(request);

        URI resourceLocation = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{userId}")
                .buildAndExpand(createdUser.id())
                .toUri();

        return ResponseEntity.created(resourceLocation).body(createdUser);
    }

    @GetMapping("/audit-logs")
    @Operation(
            summary = "Listar logs de auditoria de usuários",
            description = "Retorna o histórico de criações, edições, anonimizações e redefinições de senha, "
                    + "de forma paginada e ordenada do mais recente para o mais antigo. "
                    + "Acesso restrito a administradores.")
    @ApiResponse(responseCode = "200", description = "Página de logs obtida com sucesso")
    @ApiResponse(responseCode = "401", description = "Token ausente ou inválido")
    @ApiResponse(responseCode = "403", description = "Acesso negado: apenas administradores podem consultar a auditoria")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PageResponseDTO<UserAuditLogResponseDTO>> findAuditLogs(
            @RequestParam(required = false) UserAuditAction action,
            @RequestParam(required = false) Long targetUserId,
            @ParameterObject @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable) {

        return ResponseEntity.ok(userAuditLogService.findAll(action, targetUserId, pageable));
    }

    @GetMapping("/{userId}")
    @Operation(
            summary = "Buscar usuário por ID",
            description = "Administradores buscam qualquer usuário; usuários comuns apenas a própria conta.")
    @ApiResponse(responseCode = "200", description = "Usuário encontrado com sucesso")
    @ApiResponse(responseCode = "401", description = "Token ausente ou inválido")
    @ApiResponse(responseCode = "403", description = "Acesso negado: usuário tentando acessar conta de terceiros")
    @ApiResponse(responseCode = "404", description = "Usuário não encontrado")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserResponseDTO> findById(@PathVariable Long userId) {
        return ResponseEntity.ok(userService.findById(userId));
    }

    @PutMapping("/{userId}")
    @Operation(
            summary = "Editar usuário",
            description = "Administradores podem alterar nome, e-mail, perfil e situação ativa de qualquer conta; "
                    + "usuários comuns apenas nome e e-mail da própria conta.")
    @ApiResponse(responseCode = "200", description = "Usuário atualizado com sucesso")
    @ApiResponse(responseCode = "400", description = "Dados inválidos")
    @ApiResponse(responseCode = "401", description = "Token ausente ou inválido")
    @ApiResponse(responseCode = "403", description = "Acesso negado: edição não permitida para esta conta")
    @ApiResponse(responseCode = "404", description = "Usuário não encontrado")
    @ApiResponse(responseCode = "409", description = "E-mail já cadastrado")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserResponseDTO> update(
            @PathVariable Long userId,
            @Valid @RequestBody UserUpdateRequestDTO request) {

        return ResponseEntity.ok(userService.update(userId, request));
    }

    @DeleteMapping("/{userId}")
    @Operation(
            summary = "Anonimizar usuário",
            description = "Remove os dados pessoais da conta preservando a linha para rastreabilidade "
                    + "(nome, e-mail e senha são substituídos por valores irreversíveis). "
                    + "Administradores anonimizam qualquer conta; usuários comuns apenas a própria.")
    @ApiResponse(responseCode = "204", description = "Usuário anonimizado com sucesso")
    @ApiResponse(responseCode = "401", description = "Token ausente ou inválido")
    @ApiResponse(responseCode = "403", description = "Acesso negado ou regra de negócio violada (último administrador)")
    @ApiResponse(responseCode = "404", description = "Usuário não encontrado")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> anonymize(@PathVariable Long userId) {
        userService.anonymize(userId);
        return ResponseEntity.noContent().build();
    }
}
