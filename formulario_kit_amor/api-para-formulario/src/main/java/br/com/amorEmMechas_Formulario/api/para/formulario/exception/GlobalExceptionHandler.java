package br.com.amorEmMechas_Formulario.api.para.formulario.exception;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(org.springframework.web.multipart.MaxUploadSizeExceededException.class)
    public ResponseEntity<?> uploadMuitoGrande(org.springframework.web.multipart.MaxUploadSizeExceededException ex) {
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(Map.of(
                "timestamp", LocalDateTime.now().toString(),
                "status", 413,
                "erro", "Limite de 10 MiB por arquivo"));
    }

    @ExceptionHandler(org.springframework.web.server.ResponseStatusException.class)
    public ResponseEntity<?> responseStatus(org.springframework.web.server.ResponseStatusException ex) {
        return ResponseEntity.status(ex.getStatusCode()).body(Map.of(
                "timestamp", LocalDateTime.now().toString(),
                "status", ex.getStatusCode().value(),
                "erro", ex.getReason() == null ? "Falha na requisicao" : ex.getReason()));
    }

    @ExceptionHandler(IdNotFoundException.class)
    public ResponseEntity<?> idNotFound(IdNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                Map.of(
                        "timestamp", LocalDateTime.now().toString(),
                        "status", 404,
                        "erro", ex.getMessage()
                )
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> validationErrors(MethodArgumentNotValidException ex) {
        Map<String, String> erros = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
                erros.put(error.getField(), error.getDefaultMessage())
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                Map.of(
                        "timestamp", LocalDateTime.now().toString(),
                        "status", 400,
                        "erro", "Dados invalidos",
                        "campos", erros
                )
        );
    }

    // Validacao manual (ex: quando o corpo aceita objeto unico OU lista, e cada
    // item precisa ser validado fora do fluxo automatico do @Valid)
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<?> constraintViolation(ConstraintViolationException ex) {
        Map<String, String> erros = new HashMap<>();
        for (ConstraintViolation<?> violacao : ex.getConstraintViolations()) {
            erros.put(violacao.getPropertyPath().toString(), violacao.getMessage());
        }

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                Map.of(
                        "timestamp", LocalDateTime.now().toString(),
                        "status", 400,
                        "erro", "Dados invalidos",
                        "campos", erros
                )
        );
    }

    // Regras de negocio quebradas manualmente com IllegalArgumentException
    // (ex: consentimento LGPD ausente) - antes virava 500 sem handler
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<?> illegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                Map.of(
                        "timestamp", LocalDateTime.now().toString(),
                        "status", 400,
                        "erro", ex.getMessage()
                )
        );
    }
}