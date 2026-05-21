package br.com.amorEmMechas_Formulario.api.para.formulario.mapper.avaliacao;

import br.com.amorEmMechas_Formulario.api.para.formulario.dto.avaliacao.AvaliacaoRequestDto;
import br.com.amorEmMechas_Formulario.api.para.formulario.dto.avaliacao.AvaliacaoResponseDto;
import br.com.amorEmMechas_Formulario.api.para.formulario.entity.avaliacao.Avaliacao;
import br.com.amorEmMechas_Formulario.api.para.formulario.mapper.solicitante.SolicitanteMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class AvaliacaoMapper {



    @Autowired
    private SolicitanteMapper mapper;


    public AvaliacaoResponseDto toResponse(Avaliacao avaliacao){
        AvaliacaoResponseDto dto = new AvaliacaoResponseDto();
        dto.setId(avaliacao.getId());
        dto.setNotaFormulario(avaliacao.getNotaFormulario());
        dto.setConcluido(avaliacao.getConcluido());
        dto.setConsentimento(avaliacao.getConsentimento());
        dto.setDtConclusao(avaliacao.getDtConclusao());
        dto.setSolicitante(mapper.toResponse(avaliacao.getSolicitante()));
        return dto;
    }

    public Avaliacao toEntity (AvaliacaoRequestDto dto){
        Avaliacao avaliacao = new Avaliacao();
        avaliacao.setNotaFormulario(dto.getNotaFormulario());
        avaliacao.setConsentimento(dto.getConsentimento());
        avaliacao.setConcluido(dto.getConcluido());


        return avaliacao;
    }
}
