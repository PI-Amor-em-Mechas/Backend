package br.com.amorEmMechas_Formulario.api.para.formulario.controller.filho;

import br.com.amorEmMechas_Formulario.api.para.formulario.dto.filho.FilhoRequestDto;
import br.com.amorEmMechas_Formulario.api.para.formulario.dto.filho.FilhoResponseDto;
import br.com.amorEmMechas_Formulario.api.para.formulario.entity.filho.Filho;
import br.com.amorEmMechas_Formulario.api.para.formulario.entity.paciente.Paciente;
import br.com.amorEmMechas_Formulario.api.para.formulario.repository.filho.FilhoRepository;
import br.com.amorEmMechas_Formulario.api.para.formulario.repository.paciente.PacienteRepository;
import br.com.amorEmMechas_Formulario.api.para.formulario.service.filho.FilhoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Filhos", description = "Gerenciamento dos filhos dos pacientes")
@RestController
@RequestMapping("/filhos")
@CrossOrigin(origins = "*")
public class FilhoController {

    private FilhoService service;
    private PacienteRepository pacienteRepository;
    private FilhoRepository filhoRepository;

    public FilhoController(FilhoRepository filhoRepository, PacienteRepository pacienteRepository, FilhoService service) {
        this.filhoRepository = filhoRepository;
        this.pacienteRepository = pacienteRepository;
        this.service = service;
    }

    @Operation(summary = "Cadastra múltiplos filhos de uma vez")
    @ApiResponse(responseCode = "201", description = "Filhos cadastrados com sucesso")
    @ApiResponse(responseCode = "404", description = "Paciente não encontrado")
    @PostMapping("/batch")
    public ResponseEntity<List<FilhoResponseDto>> createBatch(@RequestBody List<FilhoRequestDto> filhosDto) {
        List<FilhoResponseDto> response = filhosDto.stream().map(dto -> {
            Paciente paciente = pacienteRepository.findById(dto.getPacienteId())
                    .orElseThrow(() -> new RuntimeException("Paciente não encontrado"));

            Filho filho = new Filho();
            filho.setIdade(dto.getIdade());
            filho.setPaciente(paciente);
            Filho saved = filhoRepository.save(filho);

            int qtdFilhos = filhoRepository.countByPacienteId(dto.getPacienteId());
            return new FilhoResponseDto(saved, qtdFilhos);
        }).toList();

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Cadastra um único filho")
    @ApiResponse(responseCode = "201", description = "Filho cadastrado com sucesso")
    @ApiResponse(responseCode = "404", description = "Paciente não encontrado")
    @PostMapping("/single")
    public ResponseEntity<FilhoResponseDto> createSingle(@RequestBody FilhoRequestDto dto) {
        Paciente paciente = pacienteRepository.findById(dto.getPacienteId())
                .orElseThrow(() -> new RuntimeException("Paciente não encontrado"));

        Filho filho = new Filho();
        filho.setIdade(dto.getIdade());
        filho.setPaciente(paciente);
        Filho saved = filhoRepository.save(filho);

        int qtdFilhos = filhoRepository.countByPacienteId(dto.getPacienteId());
        return ResponseEntity.status(HttpStatus.CREATED).body(new FilhoResponseDto(saved, qtdFilhos));
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
}