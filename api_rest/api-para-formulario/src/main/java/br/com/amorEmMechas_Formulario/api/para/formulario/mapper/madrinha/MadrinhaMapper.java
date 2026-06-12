package br.com.amorEmMechas_Formulario.api.para.formulario.mapper.madrinha;

import br.com.amorEmMechas_Formulario.api.para.formulario.dto.madrinha.MadrinhaRequestDto;
import br.com.amorEmMechas_Formulario.api.para.formulario.dto.madrinha.MadrinhaResponseDto;
import br.com.amorEmMechas_Formulario.api.para.formulario.entity.madrinha.Madrinha;
import org.springframework.stereotype.Component;

@Component
public class MadrinhaMapper {

    public Madrinha toEntity(MadrinhaRequestDto dto) {
        if (dto == null) return null;

        Madrinha entity = new Madrinha();
        entity.setNomeCompleto(dto.getNomeCompleto());
        entity.setEmail(dto.getEmail());
        entity.setHorasVoluntarias(dto.getHorasVoluntarias());
        entity.setFuncao(dto.getFuncao());
        entity.setDataCadastro(dto.getDataCadastro());
        entity.setStatus(dto.getStatus());
        return entity;
    }

    public MadrinhaResponseDto toResponse(Madrinha entity) {
        if (entity == null) return null;

        MadrinhaResponseDto dto = new MadrinhaResponseDto();
        dto.setId(entity.getId());
        dto.setNomeCompleto(entity.getNomeCompleto());
        dto.setEmail(entity.getEmail());
        dto.setHorasVoluntarias(entity.getHorasVoluntarias());
        dto.setFuncao(entity.getFuncao());
        dto.setDataCadastro(entity.getDataCadastro());
        dto.setStatus(entity.getStatus());
        return dto;
    }
}
