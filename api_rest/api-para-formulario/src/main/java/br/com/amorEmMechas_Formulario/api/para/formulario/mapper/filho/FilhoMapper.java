package br.com.amorEmMechas_Formulario.api.para.formulario.mapper.filho;

import br.com.amorEmMechas_Formulario.api.para.formulario.dto.filho.FilhoRequestDto;
import br.com.amorEmMechas_Formulario.api.para.formulario.dto.filho.FilhoResponseDto;
import br.com.amorEmMechas_Formulario.api.para.formulario.entity.filho.Filho;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class FilhoMapper {

    public Filho toEntity(FilhoRequestDto dto) {
        if (dto == null) return null;

        Filho filho = new Filho();
        filho.setIdade(dto.getIdade());
        return filho;
    }

    public FilhoResponseDto toResponse(Filho entity) {
        if (entity == null) return null;

        FilhoResponseDto dto = new FilhoResponseDto();
        dto.setId(entity.getId());
        dto.setIdade(entity.getIdade());
        return dto;
    }

    public List<FilhoResponseDto> toResponseList(List<Filho> filhos) {
        if (filhos == null) return Collections.emptyList();

        return filhos.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }
}







