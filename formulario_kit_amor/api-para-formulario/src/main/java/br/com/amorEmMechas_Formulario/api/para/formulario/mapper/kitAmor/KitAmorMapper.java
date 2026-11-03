package br.com.amorEmMechas_Formulario.api.para.formulario.mapper.kitAmor;


import br.com.amorEmMechas_Formulario.api.para.formulario.dto.kitAmor.KitAmorRequestDto;
import br.com.amorEmMechas_Formulario.api.para.formulario.dto.kitAmor.KitAmorResponseDto;
import br.com.amorEmMechas_Formulario.api.para.formulario.entity.kitAmor.KitAmor;
import br.com.amorEmMechas_Formulario.api.para.formulario.mapper.paciente.PacienteMapper;
import br.com.amorEmMechas_Formulario.api.para.formulario.mapper.solicitante.SolicitanteMapper;
import org.springframework.stereotype.Component;

@Component
public class KitAmorMapper {

    private final PacienteMapper pacienteMapper;
    private final SolicitanteMapper solicitanteMapper;

    public KitAmorMapper(PacienteMapper pacienteMapper, SolicitanteMapper solicitanteMapper) {
        this.pacienteMapper = pacienteMapper;
        this.solicitanteMapper = solicitanteMapper;
    }

    public KitAmorResponseDto toResponse(KitAmor kitAmor) {
        KitAmorResponseDto dto = new KitAmorResponseDto();
        dto.setId(kitAmor.getId());
        dto.setCorPeruca(kitAmor.getCorPeruca());

        if (kitAmor.getSolicitante() != null) {
            dto.setSolicitanteId(kitAmor.getSolicitante().getId());
            dto.setSolicitante(solicitanteMapper.toResponse(kitAmor.getSolicitante()));
        }
        if (kitAmor.getPaciente() != null) {
            dto.setPacienteId(kitAmor.getPaciente().getId());
            dto.setPaciente(pacienteMapper.toResponse(kitAmor.getPaciente()));
        }

        return dto;
    }

    public KitAmor toEntity(KitAmorRequestDto dto) {
        KitAmor kitAmor = new KitAmor();
        kitAmor.setCorPeruca(dto.getCorPeruca());
        return kitAmor;
    }
}


