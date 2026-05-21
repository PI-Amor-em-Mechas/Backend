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

import java.util.List;

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

    @Operation(summary = "Lista todas as avaliações")
    @ApiResponse(responseCode = "200", description = "Lista retornada com sucesso")
    @GetMapping
    public ResponseEntity<List<AvaliacaoResponseDto>> findAll() {
        List<AvaliacaoResponseDto> response = service.findAll();
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Busca avaliação por ID")
    @ApiResponse(responseCode = "200", description = "Avaliação encontrada")
    @ApiResponse(responseCode = "404", description = "Avaliação não encontrada")
    @GetMapping("/{id}")
    public ResponseEntity<AvaliacaoResponseDto> findById(@PathVariable Integer id) {
        AvaliacaoResponseDto response = service.findById(id);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Remove uma avaliação por ID")
    @ApiResponse(responseCode = "204", description = "Avaliação removida com sucesso")
    @ApiResponse(responseCode = "404", description = "Avaliação não encontrada")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteById(@PathVariable Integer id) {
        service.deleteById(id);
        return ResponseEntity.noContent().build();
    }

}