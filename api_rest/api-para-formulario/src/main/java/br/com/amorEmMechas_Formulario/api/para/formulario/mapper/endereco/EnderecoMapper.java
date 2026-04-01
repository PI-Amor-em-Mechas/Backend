package br.com.amorEmMechas_Formulario.api.para.formulario.mapper.endereco;

import br.com.amorEmMechas_Formulario.api.para.formulario.dto.endereco.EnderecoRequestDto;
import br.com.amorEmMechas_Formulario.api.para.formulario.dto.endereco.EnderecoResponseDto;
import br.com.amorEmMechas_Formulario.api.para.formulario.entity.endereco.Endereco;
import org.springframework.stereotype.Component;

@Component
public class EnderecoMapper {

    public Endereco toEntity(EnderecoRequestDto dto) {
        if (dto == null) return null;

        Endereco entity = new Endereco();
        entity.setCep(dto.getCep());
        entity.setRua(dto.getRua());
        entity.setNumero(dto.getNumero());
        entity.setBairro(dto.getBairro());
        entity.setCidade(dto.getCidade());
        entity.setEstado(dto.getEstado());

        return entity;
    }

    public EnderecoResponseDto toResponse(Endereco entity) {
        if (entity == null) return null;

        EnderecoResponseDto dto = new EnderecoResponseDto();
        dto.setId(entity.getId());
        dto.setCep(entity.getCep());
        dto.setRua(entity.getRua());
        dto.setNumero(entity.getNumero());
        dto.setBairro(entity.getBairro());
        dto.setCidade(entity.getCidade());
        dto.setEstado(entity.getEstado());

        return dto;
    }
}
