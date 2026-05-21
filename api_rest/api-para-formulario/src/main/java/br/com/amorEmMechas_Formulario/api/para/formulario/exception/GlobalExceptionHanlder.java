package br.com.amorEmMechas_Formulario.api.para.formulario.exception;


import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDate;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHanlder {


    @ExceptionHandler(IdNotFoundException.class)
    public ResponseEntity<?> idNotFound (IdNotFoundException ex){
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                Map.of(
                        "timestamp", LocalDate.now().toString(),
                        "status", 404,
                        "error", ex.getMessage()

                )
        );
    }

}
