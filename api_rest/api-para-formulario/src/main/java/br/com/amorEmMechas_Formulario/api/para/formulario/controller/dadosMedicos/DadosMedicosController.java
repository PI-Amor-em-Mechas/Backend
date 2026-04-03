package br.com.amorEmMechas_Formulario.api.para.formulario.controller.dadosMedicos;


import br.com.amorEmMechas_Formulario.api.para.formulario.dto.dadosMedicos.DadosMedicosRequestDto;
import br.com.amorEmMechas_Formulario.api.para.formulario.dto.dadosMedicos.DadosMedicosResponseDto;
import br.com.amorEmMechas_Formulario.api.para.formulario.service.dadosMedicos.DadosMedicosService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/dados-medicos")
@CrossOrigin(origins = "*")
public class DadosMedicosController {



    private DadosMedicosService service;

    public DadosMedicosController(DadosMedicosService service) {
        this.service = service;
    }




    @PostMapping
    public ResponseEntity<DadosMedicosResponseDto> create( @RequestBody @Valid DadosMedicosRequestDto dto){
        DadosMedicosResponseDto response = service.create(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

}
