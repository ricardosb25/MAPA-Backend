package com.mapa.controller;

import com.mapa.dto.auth.AuthResponseDTO;
import com.mapa.dto.auth.ForgotPasswordRequestDTO;
import com.mapa.dto.auth.LoginRequestDTO;
import com.mapa.dto.auth.MessageResponseDTO;
import com.mapa.dto.auth.RegisterRequestDTO;
import com.mapa.dto.auth.ResetPasswordRequestDTO;
import com.mapa.dto.auth.UserResponseDTO;
import com.mapa.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Autenticação", description = "Endpoints de cadastro, login e perfil do usuário")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    @Operation(summary = "Cadastrar usuário", description = "Cria um novo usuário com perfil STUDENT ou TEACHER")
    @ApiResponse(responseCode = "201", description = "Usuário cadastrado com sucesso")
    @ApiResponse(responseCode = "400", description = "Dados inválidos ou aceite dos Termos de Uso ausente")
    @ApiResponse(responseCode = "403", description = "Perfil não permitido para cadastro")
    @ApiResponse(responseCode = "409", description = "E-mail já cadastrado")
    public ResponseEntity<UserResponseDTO> register(@Valid @RequestBody RegisterRequestDTO request) {
        UserResponseDTO user = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(user);
    }

    @PostMapping("/login")
    @Operation(summary = "Realizar login", description = "Autentica o usuário e retorna o token JWT")
    @ApiResponse(responseCode = "200", description = "Login realizado com sucesso")
    @ApiResponse(responseCode = "401", description = "Credenciais inválidas")
    public ResponseEntity<AuthResponseDTO> login(@Valid @RequestBody LoginRequestDTO request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/forgot-password")
    @Operation(summary = "Solicitar redefinição de senha",
            description = "Envia por e-mail um link de redefinição caso o endereço esteja cadastrado. "
                    + "A resposta é sempre genérica para evitar enumeração de usuários.")
    @ApiResponse(responseCode = "200", description = "Solicitação processada (mensagem genérica)")
    @ApiResponse(responseCode = "400", description = "Dados inválidos")
    public ResponseEntity<MessageResponseDTO> forgotPassword(@Valid @RequestBody ForgotPasswordRequestDTO request) {
        return ResponseEntity.ok(authService.forgotPassword(request));
    }

    @PostMapping("/reset-password")
    @Operation(summary = "Redefinir senha",
            description = "Troca a senha do usuário utilizando o token recebido por e-mail")
    @ApiResponse(responseCode = "200", description = "Senha redefinida com sucesso")
    @ApiResponse(responseCode = "400", description = "Token inválido/expirado ou dados inválidos")
    public ResponseEntity<MessageResponseDTO> resetPassword(@Valid @RequestBody ResetPasswordRequestDTO request) {
        return ResponseEntity.ok(authService.resetPassword(request));
    }

    @GetMapping("/me")
    @Operation(summary = "Obter usuário logado", description = "Retorna os dados do usuário autenticado via token JWT")
    @ApiResponse(responseCode = "200", description = "Usuário autenticado")
    @ApiResponse(responseCode = "401", description = "Token ausente ou inválido")
    public ResponseEntity<UserResponseDTO> getCurrentUser() {
        return ResponseEntity.ok(authService.getCurrentUser());
    }
}

