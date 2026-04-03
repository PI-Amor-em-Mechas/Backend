package br.com.amorEmMechas_Formulario.api.para.formulario.controller.solicitante;


import br.com.amorEmMechas_Formulario.api.para.formulario.dto.solicitante.SolicitanteRequestDto;
import br.com.amorEmMechas_Formulario.api.para.formulario.dto.solicitante.SolicitanteResponseDto;
import br.com.amorEmMechas_Formulario.api.para.formulario.service.solicitante.SolicitanteService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/solicitantes")
@CrossOrigin(origins = "*")
public class SolicitanteController {

    private SolicitanteService service;

    public SolicitanteController(SolicitanteService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<SolicitanteResponseDto> create ( @RequestBody @Valid SolicitanteRequestDto dto){
        SolicitanteResponseDto response = service.create(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
