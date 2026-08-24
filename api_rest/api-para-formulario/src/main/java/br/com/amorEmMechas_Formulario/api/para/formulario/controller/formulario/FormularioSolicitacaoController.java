package br.com.amorEmMechas_Formulario.api.para.formulario.controller.formulario;

import br.com.amorEmMechas_Formulario.api.para.formulario.dto.paciente.PacienteRequestDto;
import br.com.amorEmMechas_Formulario.api.para.formulario.dto.paciente.PacienteResponseDto;
import br.com.amorEmMechas_Formulario.api.para.formulario.service.paciente.PacienteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Formulario de solicitacao", description = "Envio autenticado do formulario de solicitacao de peruca")
@RestController
@RequestMapping("/formulario-solicitacao-peruca")
public class FormularioSolicitacaoController {

    private final PacienteService pacienteService;

    public FormularioSolicitacaoController(PacienteService pacienteService) {
        this.pacienteService = pacienteService;
    }

    @Operation(summary = "Envia uma solicitacao de peruca")
    @ApiResponse(responseCode = "201", description = "Solicitacao registrada com sucesso")
    @ApiResponse(responseCode = "401", description = "Token ausente ou invalido")
    @ApiResponse(responseCode = "403", description = "Role sem permissao")
    @PostMapping
    public ResponseEntity<PacienteResponseDto> enviar(@RequestBody @Valid PacienteRequestDto dto) {
        PacienteResponseDto response = pacienteService.create(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}