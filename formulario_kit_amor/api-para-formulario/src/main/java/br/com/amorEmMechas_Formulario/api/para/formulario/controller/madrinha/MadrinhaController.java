package br.com.amorEmMechas_Formulario.api.para.formulario.controller.madrinha;

import br.com.amorEmMechas_Formulario.api.para.formulario.dto.madrinha.MadrinhaRequestDto;
import br.com.amorEmMechas_Formulario.api.para.formulario.dto.madrinha.MadrinhaResponseDto;
import br.com.amorEmMechas_Formulario.api.para.formulario.service.madrinha.MadrinhaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Madrinhas", description = "Gerenciamento das madrinhas do amor (voluntárias)")
@RestController
@RequestMapping("/madrinhas")
public class MadrinhaController {

    private final MadrinhaService service;

    public MadrinhaController(MadrinhaService service) {
        this.service = service;
    }

    @Operation(summary = "Cadastra uma nova madrinha")
    @ApiResponse(responseCode = "201", description = "Madrinha cadastrada com sucesso")
    @ApiResponse(responseCode = "400", description = "Dados inv├ílidos")
    @PostMapping
    public ResponseEntity<MadrinhaResponseDto> create(@RequestBody @Valid MadrinhaRequestDto dto) {
        MadrinhaResponseDto response = service.create(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Atualiza uma madrinha")
    @ApiResponse(responseCode = "200", description = "Madrinha atualizada com sucesso")
    @ApiResponse(responseCode = "404", description = "Madrinha n├úo encontrada")
    @PutMapping("/{id}")
    public ResponseEntity<MadrinhaResponseDto> update(@PathVariable Integer id, @RequestBody @Valid MadrinhaRequestDto dto) {
        MadrinhaResponseDto response = service.update(id, dto);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @Operation(summary = "Lista todas as madrinhas")
    @ApiResponse(responseCode = "200", description = "Lista retornada com sucesso")
    @GetMapping
    public ResponseEntity<List<MadrinhaResponseDto>> findAll(@RequestParam(required = false) String status) {
        if (status != null && !status.isBlank()) {
            return ResponseEntity.ok(service.findByStatus(status));
        }
        return ResponseEntity.ok(service.findAll());
    }

    @Operation(summary = "Busca madrinha por ID")
    @ApiResponse(responseCode = "200", description = "Madrinha encontrada")
    @ApiResponse(responseCode = "404", description = "Madrinha n├úo encontrada")
    @GetMapping("/{id}")
    public ResponseEntity<MadrinhaResponseDto> findById(@PathVariable Integer id) {
        return ResponseEntity.ok(service.findById(id));
    }

    @Operation(summary = "Remove uma madrinha por ID")
    @ApiResponse(responseCode = "204", description = "Madrinha removida com sucesso")
    @ApiResponse(responseCode = "404", description = "Madrinha n├úo encontrada")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteById(@PathVariable Integer id) {
        service.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
