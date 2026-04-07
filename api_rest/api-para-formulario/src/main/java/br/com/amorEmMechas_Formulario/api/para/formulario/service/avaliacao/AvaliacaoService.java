package br.com.amorEmMechas_Formulario.api.para.formulario.service.avaliacao;

import br.com.amorEmMechas_Formulario.api.para.formulario.dto.avaliacao.AvaliacaoRequestDto;
import br.com.amorEmMechas_Formulario.api.para.formulario.dto.avaliacao.AvaliacaoResponseDto;
import br.com.amorEmMechas_Formulario.api.para.formulario.entity.avaliacao.Avaliacao;
import br.com.amorEmMechas_Formulario.api.para.formulario.entity.solicitante.Solicitante;
import br.com.amorEmMechas_Formulario.api.para.formulario.exception.IdNotFoundException;
import br.com.amorEmMechas_Formulario.api.para.formulario.mapper.avaliacao.AvaliacaoMapper;
import br.com.amorEmMechas_Formulario.api.para.formulario.mapper.solicitante.SolicitanteMapper;
import br.com.amorEmMechas_Formulario.api.para.formulario.repository.avaliacao.AvaliacaoRepository;
import br.com.amorEmMechas_Formulario.api.para.formulario.repository.solicitante.SolicitanteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AvaliacaoService {

    private final AvaliacaoRepository repository;
    private final SolicitanteRepository solicitanteRepository;
    private final AvaliacaoMapper mapper;
    private final SolicitanteMapper solicitanteMapper;

    @Autowired
    public AvaliacaoService(AvaliacaoRepository repository,
                            SolicitanteRepository solicitanteRepository,
                            AvaliacaoMapper mapper,
                            SolicitanteMapper solicitanteMapper) {
        this.repository = repository;
        this.solicitanteRepository = solicitanteRepository;
        this.mapper = mapper;
        this.solicitanteMapper = solicitanteMapper;
    }

    public AvaliacaoResponseDto create(AvaliacaoRequestDto dto) {
        Solicitante solicitante = solicitanteRepository.findById(dto.getSolicitanteId())
                .orElseThrow(() -> new IdNotFoundException("ID SOLICITANTE: " + dto.getSolicitanteId() + " Não Encontrado"));

        Avaliacao avaliacao = mapper.toEntity(dto);
        avaliacao.setSolicitante(solicitante);
        Avaliacao saved = repository.save(avaliacao);

        return mapper.toResponse(saved);
    }

    public List<AvaliacaoResponseDto> findAll() {
        return repository.findAll()
                .stream()
                .map(mapper::toResponse)
                .toList();
    }

    public AvaliacaoResponseDto findById(Integer id) {
        Avaliacao avaliacao = repository.findById(id)
                .orElseThrow(() -> new IdNotFoundException("ID AVALIAÇÃO: " + id + " Não Encontrado"));

        return mapper.toResponse(avaliacao);
    }
}