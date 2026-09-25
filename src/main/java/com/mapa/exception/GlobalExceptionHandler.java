package com.mapa.exception;

import com.mapa.dto.ApiErrorDTO;
import com.mapa.dto.FieldValidationErrorDTO;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.core.PropertyReferenceException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.OffsetDateTime;
import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(EmailAlreadyExistsException.class)
    public ResponseEntity<ApiErrorDTO> handleEmailAlreadyExists(
            EmailAlreadyExistsException emailAlreadyExistsException, HttpServletRequest httpRequest) {

        return buildErrorResponse(
                HttpStatus.CONFLICT,
                emailAlreadyExistsException.getMessage(),
                httpRequest,
                List.of());
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ApiErrorDTO> handleInvalidCredentials(
            InvalidCredentialsException invalidCredentialsException, HttpServletRequest httpRequest) {

        return buildErrorResponse(
                HttpStatus.UNAUTHORIZED,
                invalidCredentialsException.getMessage(),
                httpRequest,
                List.of());
    }

    @ExceptionHandler(InvalidResetTokenException.class)
    public ResponseEntity<ApiErrorDTO> handleInvalidResetToken(
            InvalidResetTokenException invalidResetTokenException, HttpServletRequest httpRequest) {

        return buildErrorResponse(
                HttpStatus.BAD_REQUEST,
                invalidResetTokenException.getMessage(),
                httpRequest,
                List.of());
    }

    @ExceptionHandler(RoleNotAllowedException.class)
    public ResponseEntity<ApiErrorDTO> handleRoleNotAllowed(
            RoleNotAllowedException roleNotAllowedException, HttpServletRequest httpRequest) {

        return buildErrorResponse(
                HttpStatus.FORBIDDEN,
                roleNotAllowedException.getMessage(),
                httpRequest,
                List.of());
    }

    @ExceptionHandler(EmailDeliveryException.class)
    public ResponseEntity<ApiErrorDTO> handleEmailDeliveryFailure(
            EmailDeliveryException emailDeliveryException, HttpServletRequest httpRequest) {

        return buildErrorResponse(
                HttpStatus.SERVICE_UNAVAILABLE,
                emailDeliveryException.getMessage(),
                httpRequest,
                List.of());
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiErrorDTO> handleAccessDenied(
            AccessDeniedException accessDeniedException, HttpServletRequest httpRequest) {

        return buildErrorResponse(
                HttpStatus.FORBIDDEN,
                "Acesso negado: permissão de administrador necessária para gerenciar motores.",
                httpRequest,
                List.of());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorDTO> handleInvalidRequestBody(
            MethodArgumentNotValidException validationException, HttpServletRequest httpRequest) {

        List<FieldValidationErrorDTO> fieldErrors = validationException.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(fieldError -> new FieldValidationErrorDTO(fieldError.getField(), fieldError.getDefaultMessage()))
                .toList();

        return buildErrorResponse(
                HttpStatus.BAD_REQUEST,
                "Um ou mais campos da requisição são inválidos",
                httpRequest,
                fieldErrors);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiErrorDTO> handleResourceNotFound(
            ResourceNotFoundException resourceNotFoundException, HttpServletRequest httpRequest) {

        return buildErrorResponse(
                HttpStatus.NOT_FOUND,
                resourceNotFoundException.getMessage(),
                httpRequest,
                List.of());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorDTO> handleUnreadableRequestBody(HttpServletRequest httpRequest) {
        return buildErrorResponse(
                HttpStatus.BAD_REQUEST,
                "Corpo da requisição ausente ou malformado",
                httpRequest,
                List.of());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiErrorDTO> handleDataIntegrityViolation(HttpServletRequest httpRequest) {
        return buildErrorResponse(
                HttpStatus.CONFLICT,
                "A operação viola uma restrição de integridade dos dados",
                httpRequest,
                List.of());
    }

    @ExceptionHandler(PropertyReferenceException.class)
    public ResponseEntity<ApiErrorDTO> handleInvalidSortProperty(
            PropertyReferenceException propertyReferenceException, HttpServletRequest httpRequest) {

        return buildErrorResponse(
                HttpStatus.BAD_REQUEST,
                "Campo de ordenação inválido: " + propertyReferenceException.getPropertyName(),
                httpRequest,
                List.of());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiErrorDTO> handleArgumentTypeMismatch(
            MethodArgumentTypeMismatchException argumentTypeMismatchException, HttpServletRequest httpRequest) {

        return buildErrorResponse(
                HttpStatus.BAD_REQUEST,
                "Valor inválido para o parâmetro: " + argumentTypeMismatchException.getName(),
                httpRequest,
                List.of());
    }

    private ResponseEntity<ApiErrorDTO> buildErrorResponse(
            HttpStatus status,
            String message,
            HttpServletRequest httpRequest,
            List<FieldValidationErrorDTO> fieldErrors) {

        ApiErrorDTO apiError = new ApiErrorDTO(
                OffsetDateTime.now(),
                status.value(),
                status.getReasonPhrase(),
                message,
                httpRequest.getRequestURI(),
                fieldErrors);

        return ResponseEntity.status(status).body(apiError);
    }
}
