package br.com.amorEmMechas_Formulario.api.para.formulario.controller.paciente;

import br.com.amorEmMechas_Formulario.api.para.formulario.dto.paciente.PacienteRequestDto;
import br.com.amorEmMechas_Formulario.api.para.formulario.dto.paciente.PacienteResponseDto;
import br.com.amorEmMechas_Formulario.api.para.formulario.service.paciente.PacienteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Tag(name = "Pacientes", description = "Gerenciamento de pacientes")
@RestController
@RequestMapping("/pacientes")
public class PacienteController {

    private final PacienteService service;

    public PacienteController(PacienteService service) {
        this.service = service;
    }

    @Operation(summary = "Cadastra um novo paciente")
    @ApiResponse(responseCode = "201", description = "Paciente criado com sucesso")
    @ApiResponse(responseCode = "400", description = "Dados inv├ílidos")
    @PostMapping
    public ResponseEntity<PacienteResponseDto> save(@RequestBody @Valid PacienteRequestDto dto) {
        PacienteResponseDto response = service.create(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }


    @Operation(summary = "Atualiza um paciente")
    @ApiResponse(responseCode = "200", description = "Paciente atualizado com sucesso")
    @ApiResponse(responseCode = "400", description = "Dados inv├ílidos")
    @PutMapping("/{id}")
    public ResponseEntity<PacienteResponseDto> update(@PathVariable Integer id, @RequestBody @Valid PacienteRequestDto dto) {
        PacienteResponseDto res = service.update(id, dto);
        return ResponseEntity.status(HttpStatus.OK).body(res);
    }

    @Operation(summary = "Lista todos os pacientes com filtros opcionais e paginacao")
    @ApiResponse(responseCode = "200", description = "Lista retornada com sucesso")
    @GetMapping
    public ResponseEntity<Page<PacienteResponseDto>> findAll(
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFim,
            @PageableDefault(size = 20) Pageable pageable) {
        if (estado != null || dataInicio != null || dataFim != null) {
            return ResponseEntity.ok(service.findWithFilters(estado, dataInicio, dataFim, pageable));
        }
        return ResponseEntity.ok(service.findAll(pageable));
    }

    @Operation(summary = "Busca paciente por ID")
    @ApiResponse(responseCode = "200", description = "Paciente encontrado")
    @ApiResponse(responseCode = "404", description = "Paciente n├úo encontrado")
    @GetMapping("/{id}")
    public ResponseEntity<PacienteResponseDto> findById(@PathVariable Integer id) {
        return ResponseEntity.ok(service.findById(id));
    }

    @Operation(summary = "Remove um paciente por ID")
    @ApiResponse(responseCode = "204", description = "Paciente removido com sucesso")
    @ApiResponse(responseCode = "404", description = "Paciente n├úo encontrado")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteById(@PathVariable Integer id) {
        service.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    // === LGPD - Direitos do Titular (Art. 18) ===

    @Operation(summary = "LGPD Art.18 VI - Anonimiza dados pessoais do paciente")
    @ApiResponse(responseCode = "204", description = "Dados anonimizados com sucesso")
    @ApiResponse(responseCode = "404", description = "Paciente nao encontrado")
    @PostMapping("/{id}/anonimizar")
    public ResponseEntity<Void> anonimizar(@PathVariable Integer id) {
        service.anonimizar(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "LGPD Art.8 §5 - Revoga consentimento do titular")
    @ApiResponse(responseCode = "204", description = "Consentimento revogado")
    @ApiResponse(responseCode = "404", description = "Paciente nao encontrado")
    @PostMapping("/{id}/revogar-consentimento")
    public ResponseEntity<Void> revogarConsentimento(@PathVariable Integer id) {
        service.revogarConsentimento(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "LGPD Art.18 V - Exporta dados pessoais do titular (portabilidade)")
    @ApiResponse(responseCode = "200", description = "Dados exportados com sucesso")
    @ApiResponse(responseCode = "404", description = "Paciente nao encontrado")
    @GetMapping("/{id}/exportar")
    public ResponseEntity<Map<String, Object>> exportarDados(@PathVariable Integer id) {
        return ResponseEntity.ok(service.exportarDados(id));
    }

}
