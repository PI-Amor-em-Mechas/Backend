package br.com.amorEmMechas_Formulario.api.para.formulario.controller.endereco;

import br.com.amorEmMechas_Formulario.api.para.formulario.dto.endereco.EnderecoRequestDto;
import br.com.amorEmMechas_Formulario.api.para.formulario.dto.endereco.EnderecoResponseDto;
import br.com.amorEmMechas_Formulario.api.para.formulario.dto.endereco.ViaCepResponseDto;
import br.com.amorEmMechas_Formulario.api.para.formulario.service.endereco.EnderecoService;
import br.com.amorEmMechas_Formulario.api.para.formulario.service.endereco.ViaCepService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Endereços", description = "Gerenciamento de endereços")
@RestController
@RequestMapping("/enderecos")
@CrossOrigin(origins = "*")
public class EnderecoController {

    private EnderecoService service;

    @Autowired
    private ViaCepService viaCepService;

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

    @Operation(summary = "Cadastra um novo endereço")
    @ApiResponse(responseCode = "201", description = "Endereço cadastrado com sucesso")
    @ApiResponse(responseCode = "400", description = "Dados inválidos")
    @PostMapping("/viacep")
    public ViaCepResponseDto preencherEndereco(@RequestBody EnderecoRequestDto request) {
        return viaCepService.buscarEnderecoPorCep(request.getCep());
    }

    @Operation(summary = "Atualizar um endereço")
    @ApiResponse(responseCode = "200", description = "Endereço atualizado com sucesso")
    @ApiResponse(responseCode = "400", description = "Dados inválidos")
    @PutMapping("/{id}")
    public ResponseEntity<EnderecoResponseDto> update(@PathVariable Integer id, @RequestBody @Valid EnderecoRequestDto dto) {
        EnderecoResponseDto response = service.update(id, dto);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @Operation(summary = "Lista todos os endereços")
    @ApiResponse(responseCode = "200", description = "Lista retornada com sucesso")
    @GetMapping
    public ResponseEntity<List<EnderecoResponseDto>> findAll() {
        return ResponseEntity.ok(service.findAll());
    }

    @Operation(summary = "Busca endereço por ID")
    @ApiResponse(responseCode = "200", description = "Endereço encontrado")
    @ApiResponse(responseCode = "404", description = "Endereço não encontrado")
    @GetMapping("/{id}")
    public ResponseEntity<EnderecoResponseDto> findById(@PathVariable Integer id) {
        return ResponseEntity.ok(service.findById(id));
    }

}







