package br.com.amorEmMechas_Formulario.api.para.formulario.controller.filho;

import br.com.amorEmMechas_Formulario.api.para.formulario.dto.filho.FilhoRequestDto;
import br.com.amorEmMechas_Formulario.api.para.formulario.dto.filho.FilhoResponseDto;
import br.com.amorEmMechas_Formulario.api.para.formulario.repository.filho.FilhoRepository;
import br.com.amorEmMechas_Formulario.api.para.formulario.repository.paciente.PacienteRepository;
import br.com.amorEmMechas_Formulario.api.para.formulario.service.filho.FilhoService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Valid;
import jakarta.validation.Validator;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Tag(name = "Filhos", description = "Gerenciamento dos filhos dos pacientes")
@RestController
@RequestMapping("/filhos")
public class FilhoController {

    private FilhoService service;
    private PacienteRepository pacienteRepository;
    private FilhoRepository filhoRepository;
    private final ObjectMapper objectMapper;
    private final Validator validator;

    public FilhoController(FilhoRepository filhoRepository, PacienteRepository pacienteRepository,
                           FilhoService service, ObjectMapper objectMapper, Validator validator) {
        this.filhoRepository = filhoRepository;
        this.pacienteRepository = pacienteRepository;
        this.service = service;
        this.objectMapper = objectMapper;
        this.validator = validator;
    }

    @Operation(summary = "Cadastra filho(s) - aceita um objeto unico ou uma lista de filhos no mesmo endpoint")
    @ApiResponse(responseCode = "201", description = "Filho(s) cadastrado(s) com sucesso")
    @ApiResponse(responseCode = "400", description = "Dados invalidos")
    @ApiResponse(responseCode = "404", description = "Paciente não encontrado")
    @PostMapping
    public ResponseEntity<List<FilhoResponseDto>> create(@RequestBody JsonNode body) {
        List<FilhoRequestDto> filhosDto = parseFilhosBody(body);
        filhosDto.forEach(this::validar);

        List<FilhoResponseDto> response = filhosDto.stream()
                .map(service::create)
                .toList();

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Aceita tanto um unico objeto {"idade":..,"pacienteId":..} quanto uma lista
     * [{"idade":..,"pacienteId":..}, ...] no mesmo endpoint POST /filhos.
     */
    private List<FilhoRequestDto> parseFilhosBody(JsonNode body) {
        if (body == null || body.isNull() || body.isMissingNode()) {
            throw new IllegalArgumentException("O corpo da requisicao nao pode ser vazio");
        }

        if (body.isArray()) {
            List<FilhoRequestDto> lista = new ArrayList<>();
            body.forEach(node -> lista.add(objectMapper.convertValue(node, FilhoRequestDto.class)));
            if (lista.isEmpty()) {
                throw new IllegalArgumentException("Envie pelo menos um filho");
            }
            return lista;
        }

        return List.of(objectMapper.convertValue(body, FilhoRequestDto.class));
    }

    private void validar(FilhoRequestDto dto) {
        Set<ConstraintViolation<FilhoRequestDto>> violacoes = validator.validate(dto);
        if (!violacoes.isEmpty()) {
            throw new ConstraintViolationException(violacoes);
        }
    }

    @Operation(summary = "Atualiza um único filho")
    @ApiResponse(responseCode = "200", description = "Filho atualizado com sucesso")
    @ApiResponse(responseCode = "404", description = "Paciente não encontrado")
    @ApiResponse(responseCode = "400", description = "Dado do filho inválido")
    @PutMapping("/unique/{id}")
    public ResponseEntity<FilhoResponseDto> updateSingle(@PathVariable Integer id, @RequestBody @Valid FilhoRequestDto dto) {
        FilhoResponseDto resp = service.update(id, dto);
        int qtdFilhos = filhoRepository.countByPacienteId(dto.getPacienteId());
        resp.setQtdFilho(qtdFilhos);
        return ResponseEntity.status(HttpStatus.OK).body(resp);
    }

    @Operation(summary = "Atualiza vários filhos")
    @ApiResponse(responseCode = "200", description = "Filhos atualizados com sucesso")
    @ApiResponse(responseCode = "404", description = "Paciente não encontrado")
    @ApiResponse(responseCode = "400", description = "Dados dos filhos inválidos")
    @PutMapping("/many")
    public ResponseEntity<List<FilhoResponseDto>> updateMany(@RequestBody @Valid List<FilhoRequestDto> dtoList) {
        List<FilhoResponseDto> respList = service.updateMany(dtoList);
        respList.forEach(resp -> {
            int qtdFilhos = filhoRepository.countByPacienteId(resp.getPacienteId());
            resp.setQtdFilho(qtdFilhos);
        });
        return ResponseEntity.status(HttpStatus.OK).body(respList);
    }

    @Operation(summary = "Lista todos os filhos")
    @ApiResponse(responseCode = "200", description = "Lista retornada com sucesso")
    @GetMapping
    public ResponseEntity<List<FilhoResponseDto>> findAll() {
        return ResponseEntity.ok(service.findAll());
    }

    @Operation(summary = "Busca filho por ID")
    @ApiResponse(responseCode = "200", description = "Filho encontrado")
    @ApiResponse(responseCode = "404", description = "Filho não encontrado")
    @GetMapping("/{id}")
    public ResponseEntity<FilhoResponseDto> findById(@PathVariable Integer id) {
        return ResponseEntity.ok(service.findById(id));
    }

    @Operation(summary = "Remove um filho por ID")
    @ApiResponse(responseCode = "204", description = "Filho removido com sucesso")
    @ApiResponse(responseCode = "404", description = "Filho não encontrado")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteById(@PathVariable Integer id) {
        service.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}