package br.com.amorEmMechas_Formulario.api.para.formulario.mapper.dadosMedicos;

import br.com.amorEmMechas_Formulario.api.para.formulario.dto.dadosMedicos.DadosMedicosRequestDto;
import br.com.amorEmMechas_Formulario.api.para.formulario.dto.dadosMedicos.DadosMedicosResponseDto;
import br.com.amorEmMechas_Formulario.api.para.formulario.entity.dadosMedicos.DadosMedicos;
import org.springframework.stereotype.Component;

@Component
public class DadosMedicosMapper {

    public DadosMedicos toEntity(DadosMedicosRequestDto dto) {

        if (dto == null) {
            return null;
        }

        DadosMedicos entity = new DadosMedicos();

        entity.setMotivo(dto.getMotivo());
        entity.setTipoCancer(dto.getTipoCancer());
        entity.setJustificativa(dto.getJustificativa());
        entity.setDtInicioTratamento(dto.getDtInicioTratamento());
        entity.setTipoAtendimento(dto.getTipoAtendimento());

        return entity;
    }

    public DadosMedicosResponseDto toResponse(DadosMedicos entity) {

        if (entity == null) {
            return null;
        }

        DadosMedicosResponseDto dto = new DadosMedicosResponseDto();

        dto.setId(entity.getId());
        dto.setMotivo(entity.getMotivo());
        dto.setTipoCancer(entity.getTipoCancer());
        dto.setJustificativa(entity.getJustificativa());
        dto.setDtInicioTratamento(entity.getDtInicioTratamento());
        dto.setTipoAtendimento(entity.getTipoAtendimento());

        // NOVO
        if (entity.getArquivo() != null) {

            dto.setRelatorioMedicoId(
                    Math.toIntExact(entity.getArquivo().getId())
            );
        }

        return dto;
    }
}