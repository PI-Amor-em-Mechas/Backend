package br.com.amorEmMechas_Formulario.api.para.formulario.mapper.dadosMedicos;

import br.com.amorEmMechas_Formulario.api.para.formulario.dto.dadosMedicos.DadosMedicosRequestDto;
import br.com.amorEmMechas_Formulario.api.para.formulario.dto.dadosMedicos.DadosMedicosResponseDto;
import br.com.amorEmMechas_Formulario.api.para.formulario.entity.dadosMedicos.DadosMedicos;
import org.springframework.stereotype.Component;

import java.util.Base64;

@Component
public class DadosMedicosMapper {

    public DadosMedicos toEntity(DadosMedicosRequestDto dto) {
        if (dto == null) return null;

        DadosMedicos entity = new DadosMedicos();
        entity.setMotivo(dto.getMotivo());
        entity.setTipoCancer(dto.getTipoCancer());
        entity.setJustificativa(dto.getJustificativa());
        entity.setDtInicioTratamento(dto.getDtInicioTratamento());
        entity.setTipoAtendimento(dto.getTipoAtendimento());

        if (dto.getRelatorioMedicoBase64() != null) {
            entity.setRelatorioMedico(Base64.getDecoder().decode(dto.getRelatorioMedicoBase64()));
        }

        return entity;
    }

    public DadosMedicosResponseDto toResponse(DadosMedicos entity) {
        if (entity == null) return null;

        DadosMedicosResponseDto dto = new DadosMedicosResponseDto();
        dto.setId(entity.getId());
        dto.setMotivo(entity.getMotivo());
        dto.setTipoCancer(entity.getTipoCancer());
        dto.setJustificativa(entity.getJustificativa());
        dto.setDtInicioTratamento(entity.getDtInicioTratamento());
        dto.setTipoAtendimento(entity.getTipoAtendimento());

        // Se quiser devolver a imagem em Base64
        if (entity.getRelatorioMedico() != null) {
            dto.setRelatorioMedicoBase64(Base64.getEncoder().encodeToString(entity.getRelatorioMedico()));
        }

        return dto;
    }
}
