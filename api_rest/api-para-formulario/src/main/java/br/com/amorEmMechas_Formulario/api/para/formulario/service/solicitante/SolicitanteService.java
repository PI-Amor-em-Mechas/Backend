package br.com.amorEmMechas_Formulario.api.para.formulario.service.solicitante;


import br.com.amorEmMechas_Formulario.api.para.formulario.dto.solicitante.SolicitanteRequestDto;
import br.com.amorEmMechas_Formulario.api.para.formulario.dto.solicitante.SolicitanteResponseDto;
import br.com.amorEmMechas_Formulario.api.para.formulario.entity.solicitante.Solicitante;
import br.com.amorEmMechas_Formulario.api.para.formulario.exception.IdNotFoundException;
import br.com.amorEmMechas_Formulario.api.para.formulario.mapper.solicitante.SolicitanteMapper;
import br.com.amorEmMechas_Formulario.api.para.formulario.repository.solicitante.SolicitanteRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SolicitanteService {


    private SolicitanteRepository repository;
    private SolicitanteMapper mapper;

    public SolicitanteService(SolicitanteMapper mapper, SolicitanteRepository repository) {
        this.mapper = mapper;
        this.repository = repository;
    }


    public SolicitanteResponseDto create(SolicitanteRequestDto dto) {
        Solicitante s = mapper.toEntity(dto);
        Solicitante saved = repository.save(s);
        return mapper.toResponse(saved);
    }

    public List<SolicitanteResponseDto> findAll() {
        return repository.findAll()
                .stream()
                .map(mapper::toResponse)
                .toList();
    }

    public SolicitanteResponseDto findById(Integer id) {
        Solicitante s = repository.findById(id)
                .orElseThrow(() -> new IdNotFoundException("ID SOLICITANTE: " + id + " Não Encontrado"));
        return mapper.toResponse(s);
    }

    public void deleteById(Integer id) {
        if (!repository.existsById(id)) {
            throw new IdNotFoundException("ID SOLICITANTE: " + id + " Não Encontrado");
        }
        repository.deleteById(id);
    }
}
