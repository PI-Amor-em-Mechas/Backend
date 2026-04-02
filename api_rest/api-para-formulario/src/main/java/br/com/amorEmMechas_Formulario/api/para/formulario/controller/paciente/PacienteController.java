package br.com.amorEmMechas_Formulario.api.para.formulario.controller.paciente;


import br.com.amorEmMechas_Formulario.api.para.formulario.dto.paciente.PacienteRequestDto;
import br.com.amorEmMechas_Formulario.api.para.formulario.dto.paciente.PacienteResponseDto;
import br.com.amorEmMechas_Formulario.api.para.formulario.mapper.paciente.PacienteMapper;
import br.com.amorEmMechas_Formulario.api.para.formulario.service.paciente.PacienteService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/pacientes")
public class PacienteController {

    private PacienteService service;

    public PacienteController(PacienteMapper mapper, PacienteService service) {
        this.service = service;
    }


    @PostMapping
    public ResponseEntity<PacienteResponseDto> save (@RequestBody @Valid PacienteRequestDto dto){
        PacienteResponseDto response = service.create(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);

    }

    @GetMapping("/{id}")
    public ResponseEntity<PacienteResponseDto> findById (@PathVariable Integer id){
        PacienteResponseDto response = service.findById(id);
        return ResponseEntity.ok(response);
    }

}
