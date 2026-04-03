package br.com.amorEmMechas_Formulario.api.para.formulario.controller.endereco;


import br.com.amorEmMechas_Formulario.api.para.formulario.dto.endereco.EnderecoRequestDto;
import br.com.amorEmMechas_Formulario.api.para.formulario.dto.endereco.EnderecoResponseDto;
import br.com.amorEmMechas_Formulario.api.para.formulario.service.endereco.EnderecoService;
import br.com.amorEmMechas_Formulario.api.para.formulario.service.paciente.PacienteService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/enderecos")
@CrossOrigin(origins = "*")
public class EnderecoController {

    private EnderecoService service;

    public EnderecoController(EnderecoService service) {
        this.service = service;
    }


    @PostMapping
    public ResponseEntity<EnderecoResponseDto> create (@RequestBody @Valid EnderecoRequestDto dto){
        EnderecoResponseDto response = service.create(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);

    }


}
