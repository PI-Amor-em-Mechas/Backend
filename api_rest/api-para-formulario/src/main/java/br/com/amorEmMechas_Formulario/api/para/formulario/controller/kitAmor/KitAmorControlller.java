package br.com.amorEmMechas_Formulario.api.para.formulario.controller.kitAmor;

import br.com.amorEmMechas_Formulario.api.para.formulario.dto.kitAmor.KitAmorRequestDto;
import br.com.amorEmMechas_Formulario.api.para.formulario.dto.kitAmor.KitAmorResponseDto;
import br.com.amorEmMechas_Formulario.api.para.formulario.service.kitAmor.KitAmorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Kit do Amor", description = "Gerenciamento dos kits de peruca")
@RestController
@RequestMapping("/kits")
@CrossOrigin(origins = "*")
public class KitAmorControlller {

    private KitAmorService service; // corrigido: era public

    public KitAmorControlller(KitAmorService service) {
        this.service = service;
    }

    @Operation(summary = "Cadastra um novo kit do amor")
    @ApiResponse(responseCode = "201", description = "Kit cadastrado com sucesso")
    @ApiResponse(responseCode = "400", description = "Dados inválidos")
    @PostMapping
    public ResponseEntity<KitAmorResponseDto> create(@RequestBody @Valid KitAmorRequestDto dto) {
        KitAmorResponseDto response = service.create(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Lista todos os kits do amor")
    @ApiResponse(responseCode = "200", description = "Lista retornada com sucesso")
    @GetMapping
    public ResponseEntity<List<KitAmorResponseDto>> findAll() {
        return ResponseEntity.ok(service.findAll());
    }

    @Operation(summary = "Busca kit do amor por ID")
    @ApiResponse(responseCode = "200", description = "Kit encontrado")
    @ApiResponse(responseCode = "404", description = "Kit não encontrado")
    @GetMapping("/{id}")
    public ResponseEntity<KitAmorResponseDto> findById(@PathVariable Integer id) {
        return ResponseEntity.ok(service.findById(id));
    }

    @Operation(summary = "Remove um kit do amor por ID")
    @ApiResponse(responseCode = "204", description = "Kit removido com sucesso")
    @ApiResponse(responseCode = "404", description = "Kit não encontrado")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteById(@PathVariable Integer id) {
        service.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}