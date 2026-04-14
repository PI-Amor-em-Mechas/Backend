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

    @Operation(summary = "Atualiza um kit do amor")
    @ApiResponse(responseCode = "201", description = "Kit atualizado com sucesso")
    @ApiResponse(responseCode = "400", description = "Dados inválidos")
    @PutMapping("/{id}")
    public ResponseEntity<KitAmorResponseDto> update(@PathVariable Integer id, @RequestBody @Valid KitAmorRequestDto dto){
        KitAmorResponseDto response = service.update(id, dto);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

}