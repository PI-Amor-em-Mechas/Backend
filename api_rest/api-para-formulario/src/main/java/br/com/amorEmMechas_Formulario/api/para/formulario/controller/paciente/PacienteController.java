@Tag(name = "Pacientes", description = "Gerenciamento de pacientes")
@RestController
@RequestMapping("/pacientes")
@CrossOrigin(origins = "*")
public class PacienteController {

    private PacienteService service;

    public PacienteController(PacienteMapper mapper, PacienteService service) {
        this.service = service;
    }

    @Operation(summary = "Cadastra um novo paciente")
    @ApiResponse(responseCode = "201", description = "Paciente criado com sucesso")
    @ApiResponse(responseCode = "400", description = "Dados inválidos")
    @PostMapping
    public ResponseEntity<PacienteResponseDto> save(@RequestBody @Valid PacienteRequestDto dto) {
        PacienteResponseDto response = service.create(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Busca paciente por ID")
    @ApiResponse(responseCode = "200", description = "Paciente encontrado")
    @ApiResponse(responseCode = "404", description = "Paciente não encontrado")
    @GetMapping("/{id}")
    public ResponseEntity<PacienteResponseDto> findById(@PathVariable Integer id) {
        PacienteResponseDto response = service.findById(id);
        return ResponseEntity.ok(response);
    }
}