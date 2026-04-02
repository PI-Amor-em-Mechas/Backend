package br.com.amorEmMechas_Formulario.api.para.formulario.controller.kitAmor;


import br.com.amorEmMechas_Formulario.api.para.formulario.dto.kitAmor.KitAmorRequestDto;
import br.com.amorEmMechas_Formulario.api.para.formulario.dto.kitAmor.KitAmorResponseDto;
import br.com.amorEmMechas_Formulario.api.para.formulario.service.kitAmor.KitAmorService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/kits")
public class KitAmorControlller {


    public KitAmorService service;

    public KitAmorControlller(KitAmorService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<KitAmorResponseDto> create(@RequestBody @Valid KitAmorRequestDto dto) {
        KitAmorResponseDto response = service.create(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);

    }
}
