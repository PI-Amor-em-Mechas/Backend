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
}