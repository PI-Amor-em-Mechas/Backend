package br.com.amorEmMechas_Formulario.api.para.formulario.controller.endereco;

import br.com.amorEmMechas_Formulario.api.para.formulario.dto.endereco.EnderecoRequestDto;
import br.com.amorEmMechas_Formulario.api.para.formulario.dto.endereco.EnderecoResponseDto;
import br.com.amorEmMechas_Formulario.api.para.formulario.service.endereco.EnderecoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Endereços", description = "Gerenciamento de endereços")
@RestController
@RequestMapping("/enderecos")
@CrossOrigin(origins = "*")
public class EnderecoController {

    private EnderecoService service;

    public EnderecoController(EnderecoService service) {
        this.service = service;
    }

    @Operation(summary = "Cadastra um novo endereço")
    @ApiResponse(responseCode = "201", description = "Endereço cadastrado com sucesso")
    @ApiResponse(responseCode = "400", description = "Dados inválidos")
    @PostMapping
    public ResponseEntity<EnderecoResponseDto> create(@RequestBody @Valid EnderecoRequestDto dto) {
        EnderecoResponseDto response = service.create(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}