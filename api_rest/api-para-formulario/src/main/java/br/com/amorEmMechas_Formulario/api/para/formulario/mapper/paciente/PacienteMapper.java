package br.com.amorEmMechas_Formulario.api.para.formulario.mapper.paciente;


import br.com.amorEmMechas_Formulario.api.para.formulario.dto.paciente.PacienteRequestDto;
import br.com.amorEmMechas_Formulario.api.para.formulario.dto.paciente.PacienteResponseDto;
import br.com.amorEmMechas_Formulario.api.para.formulario.entity.paciente.Paciente;
import org.springframework.stereotype.Component;

@Component
public class PacienteMapper {



    public PacienteResponseDto toResponse(Paciente paciente) {
        if (paciente == null) {
            return null;
        }

        PacienteResponseDto p = new PacienteResponseDto();
        p.setId(paciente.getId());
        p.setNomeCompleto(paciente.getNomeCompleto());
        p.setEmail(paciente.getEmail());
        p.setDtPedido(paciente.getDtPedido());
        p.setCel(paciente.getCel());
        p.setDtNasc(paciente.getDtNasc());
        p.setEstadoCivil(paciente.getEstadoCivil());
        p.setTemFilhos(paciente.getTemFilhos());
        p.setQtdPessoasEmCasa(paciente.getQtdPessoasEmCasa());
        p.setCpf(paciente.getCpf());

        return p;
    }

    public Paciente toEntity(PacienteRequestDto dto) {
        if (dto == null) {
            return null;
        }

        Paciente paciente = new Paciente();
        paciente.setNomeCompleto(dto.getNomeCompleto());
        paciente.setEmail(dto.getEmail());
        paciente.setDtPedido(dto.getDtPedido());
        paciente.setCel(dto.getCel());
        paciente.setDtNasc(dto.getDtNasc());
        paciente.setEstadoCivil(dto.getEstadoCivil());
        paciente.setTemFilhos(dto.getTemFilhos());
        paciente.setQtdPessoasEmCasa(dto.getQtdPessoasEmCasa());
        paciente.setCpf(dto.getCpf());

        return paciente;
    }



}
