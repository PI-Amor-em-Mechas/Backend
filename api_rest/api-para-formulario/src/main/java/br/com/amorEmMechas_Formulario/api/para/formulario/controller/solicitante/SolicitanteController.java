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

    @Operation(summary = "atualiza um solicitante")
    @ApiResponse(responseCode = "201", description = "Solicitante atualizado com sucesso")
    @ApiResponse(responseCode = "400", description = "Dados inválidos")
    @PutMapping("/{id}")
    public ResponseEntity<SolicitanteResponseDto> update(@PathVariable Integer id, @RequestBody @Valid SolicitanteRequestDto dto){
        SolicitanteResponseDto response = service.update(id, dto);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }


}