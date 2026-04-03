package br.com.amorEmMechas_Formulario.api.para.formulario.service.solicitante;


import br.com.amorEmMechas_Formulario.api.para.formulario.dto.solicitante.SolicitanteRequestDto;
import br.com.amorEmMechas_Formulario.api.para.formulario.dto.solicitante.SolicitanteResponseDto;
import br.com.amorEmMechas_Formulario.api.para.formulario.entity.solicitante.Solicitante;
import br.com.amorEmMechas_Formulario.api.para.formulario.mapper.solicitante.SolicitanteMapper;
import br.com.amorEmMechas_Formulario.api.para.formulario.repository.solicitante.SolicitanteRepository;
import org.springframework.stereotype.Service;

@Service
public class SolicitanteService {


    private SolicitanteRepository repository;
    private SolicitanteMapper mapper;

    public SolicitanteService(SolicitanteMapper mapper, SolicitanteRepository repository) {
        this.mapper = mapper;
        this.repository = repository;
    }


    public SolicitanteResponseDto create (SolicitanteRequestDto dto){
        Solicitante s = mapper.toEntity(dto);
        Solicitante saved = repository.save(s);
        return mapper.toResponse(saved);
    }
}
