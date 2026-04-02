package br.com.amorEmMechas_Formulario.api.para.formulario.mapper.solicitante;

import br.com.amorEmMechas_Formulario.api.para.formulario.dto.solicitante.SolicitanteRequestDto;
import br.com.amorEmMechas_Formulario.api.para.formulario.dto.solicitante.SolicitanteResponseDto;
import br.com.amorEmMechas_Formulario.api.para.formulario.entity.solicitante.Solicitante;
import org.springframework.stereotype.Component;

@Component
public class SolicitanteMapper {

    public Solicitante toEntity(SolicitanteRequestDto dto) {
        if (dto == null) {
            return null;
        }
        Solicitante s = new Solicitante();
        s.setNomeCompleto(dto.getNomeCompleto());
        s.setRg(dto.getRg());
        return s;
    }


    public SolicitanteResponseDto toResponse(Solicitante entity) {
        SolicitanteResponseDto dto = new SolicitanteResponseDto();
        dto.setId(entity.getId());
        dto.setId(entity.getId());
        dto.setNomeCompleto(entity.getNomeCompleto());
        dto.setRg(entity.getRg());
        return dto;
    }
}

