package br.com.amorEmMechas_Formulario.api.para.formulario.controller.solicitante;

import br.com.amorEmMechas_Formulario.api.para.formulario.dto.solicitante.SolicitanteRequestDto;
import br.com.amorEmMechas_Formulario.api.para.formulario.dto.solicitante.SolicitanteResponseDto;
import br.com.amorEmMechas_Formulario.api.para.formulario.service.solicitante.SolicitanteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Solicitantes", description = "Gerenciamento de solicitantes")
@RestController
@RequestMapping("/solicitantes")
@CrossOrigin(origins = "*")
public class SolicitanteController {

    private SolicitanteService service;

    public SolicitanteController(SolicitanteService service) {
        this.service = service;
    }

    @Operation(summary = "Cadastra um novo solicitante")
    @ApiResponse(responseCode = "201", description = "Solicitante cadastrado com sucesso")
    @ApiResponse(responseCode = "400", description = "Dados inválidos")
    @PostMapping
    public ResponseEntity<SolicitanteResponseDto> create(@RequestBody @Valid SolicitanteRequestDto dto) {
        SolicitanteResponseDto response = service.create(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Lista todos os solicitantes")
    @ApiResponse(responseCode = "200", description = "Lista retornada com sucesso")
    @GetMapping
    public ResponseEntity<List<SolicitanteResponseDto>> findAll() {
        return ResponseEntity.ok(service.findAll());
    }

    @Operation(summary = "Busca solicitante por ID")
    @ApiResponse(responseCode = "200", description = "Solicitante encontrado")
    @ApiResponse(responseCode = "404", description = "Solicitante não encontrado")
    @GetMapping("/{id}")
    public ResponseEntity<SolicitanteResponseDto> findById(@PathVariable Integer id) {
        return ResponseEntity.ok(service.findById(id));
    }
}