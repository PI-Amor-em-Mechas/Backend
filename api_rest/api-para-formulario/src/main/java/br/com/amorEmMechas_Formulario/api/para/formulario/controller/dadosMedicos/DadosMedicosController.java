package br.com.amorEmMechas_Formulario.api.para.formulario.controller.dadosMedicos;

import br.com.amorEmMechas_Formulario.api.para.formulario.dto.dadosMedicos.DadosMedicosRequestDto;
import br.com.amorEmMechas_Formulario.api.para.formulario.dto.dadosMedicos.DadosMedicosResponseDto;
import br.com.amorEmMechas_Formulario.api.para.formulario.service.dadosMedicos.DadosMedicosService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Dados Médicos", description = "Gerenciamento de dados médicos dos pacientes")
@RestController
@RequestMapping("/dados-medicos")
@CrossOrigin(origins = "*")
public class DadosMedicosController {

    private DadosMedicosService service;

    public DadosMedicosController(DadosMedicosService service) {
        this.service = service;
    }

    @Operation(summary = "Cadastra dados médicos de um paciente")
    @ApiResponse(responseCode = "201", description = "Dados médicos cadastrados com sucesso")
    @ApiResponse(responseCode = "400", description = "Dados inválidos")
    @PostMapping
    public ResponseEntity<DadosMedicosResponseDto> create(@RequestBody @Valid DadosMedicosRequestDto dto) {
        DadosMedicosResponseDto response = service.create(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Lista todos os dados médicos")
    @ApiResponse(responseCode = "200", description = "Lista retornada com sucesso")
    @GetMapping
    public ResponseEntity<List<DadosMedicosResponseDto>> findAll() {
        return ResponseEntity.ok(service.findAll());
    }

    @Operation(summary = "Busca dados médicos por ID")
    @ApiResponse(responseCode = "200", description = "Dados médicos encontrados")
    @ApiResponse(responseCode = "404", description = "Dados médicos não encontrados")
    @GetMapping("/{id}")
    public ResponseEntity<DadosMedicosResponseDto> findById(@PathVariable Integer id) {
        return ResponseEntity.ok(service.findById(id));
    }

    @Operation(summary = "Remove dados médicos por ID")
    @ApiResponse(responseCode = "204", description = "Dados médicos removidos com sucesso")
    @ApiResponse(responseCode = "404", description = "Dados médicos não encontrados")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteById(@PathVariable Integer id) {
        service.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}