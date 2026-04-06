package br.com.amorEmMechas_Formulario.api.para.formulario.controller.avaliacao;

import br.com.amorEmMechas_Formulario.api.para.formulario.dto.avaliacao.AvaliacaoRequestDto;
import br.com.amorEmMechas_Formulario.api.para.formulario.dto.avaliacao.AvaliacaoResponseDto;
import br.com.amorEmMechas_Formulario.api.para.formulario.service.avaliacao.AvaliacaoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Avaliações", description = "Gerenciamento de avaliações")
@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/avaliacoes")
public class AvaliacaoController {

    @Autowired
    private AvaliacaoService service;

    @Operation(summary = "Cria uma nova avaliação")
    @ApiResponse(responseCode = "201", description = "Avaliação criada com sucesso")
    @ApiResponse(responseCode = "400", description = "Dados inválidos")
    @PostMapping
    public ResponseEntity<AvaliacaoResponseDto> createAval(@RequestBody @Valid AvaliacaoRequestDto dto) {
        AvaliacaoResponseDto response = service.create(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}