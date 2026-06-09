package br.com.amorEmMechas_Formulario.api.para.formulario.mapper.paciente;

import br.com.amorEmMechas_Formulario.api.para.formulario.dto.paciente.PacienteRequestDto;
import br.com.amorEmMechas_Formulario.api.para.formulario.dto.paciente.PacienteResponseDto;
import br.com.amorEmMechas_Formulario.api.para.formulario.entity.paciente.Paciente;
import br.com.amorEmMechas_Formulario.api.para.formulario.mapper.dadosMedicos.DadosMedicosMapper;
import br.com.amorEmMechas_Formulario.api.para.formulario.mapper.endereco.EnderecoMapper;
import br.com.amorEmMechas_Formulario.api.para.formulario.mapper.filho.FilhoMapper;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.Period;

@Component
public class PacienteMapper {

    private final EnderecoMapper enderecoMapper;
    private final FilhoMapper filhoMapper;
    private final DadosMedicosMapper dadosMedicosMapper;

    public PacienteMapper(EnderecoMapper enderecoMapper,
                          DadosMedicosMapper dadosMedicosMapper,
                          FilhoMapper filhoMapper) {
        this.enderecoMapper = enderecoMapper;
        this.filhoMapper = filhoMapper;
        this.dadosMedicosMapper = dadosMedicosMapper;
    }

    public Paciente toEntity(PacienteRequestDto dto) {
        if (dto == null) return null;

        Paciente entity = new Paciente();
        entity.setNomeCompleto(dto.getNomeCompleto());
        entity.setEmail(dto.getEmail());
        entity.setCpf(dto.getCpf());
        entity.setCel(dto.getCel());
        entity.setDtPedido(dto.getDtPedido());
        entity.setDtNasc(dto.getDtNasc());
        entity.setEstadoCivil(dto.getEstadoCivil());
        entity.setTemFilhos(dto.getTemFilhos());
        entity.setQtdPessoasEmCasa(dto.getQtdPessoasEmCasa());

        return entity;
    }

    public PacienteResponseDto toResponse(Paciente paciente) {
        if (paciente == null) return null;

        PacienteResponseDto dto = new PacienteResponseDto();

        dto.setId(paciente.getId());
        dto.setNomeCompleto(paciente.getNomeCompleto());
        dto.setEmail(paciente.getEmail());
        dto.setCpf(paciente.getCpf());
        dto.setCel(paciente.getCel());
        dto.setDtPedido(paciente.getDtPedido());
        dto.setDtNasc(paciente.getDtNasc());
        dto.setEstadoCivil(paciente.getEstadoCivil());
        dto.setTemFilhos(paciente.getTemFilhos());
        dto.setQtdPessoasEmCasa(paciente.getQtdPessoasEmCasa());

        // ENDERE├çO
        if (paciente.getEndereco() != null) {
            dto.setEndereco(enderecoMapper.toResponse(paciente.getEndereco()));
        }

        // FILHOS
        if (paciente.getFilhos() != null) {
            dto.setFilhos(filhoMapper.toResponseList(paciente.getFilhos()));
            dto.setQtdFilho(paciente.getFilhos().size());
        } else {
            dto.setQtdFilho(0);
        }

        // DADOS MEDICOS
        if (paciente.getDadosMedicos() != null && !paciente.getDadosMedicos().isEmpty()) {
            dto.setDadosMedicos(dadosMedicosMapper.toResponse(paciente.getDadosMedicos().get(0)));
        }

        // IDADE (calculada a partir de dtNasc)
        if (paciente.getDtNasc() != null) {
            dto.setIdade(Period.between(paciente.getDtNasc(), LocalDate.now()).getYears());
        }

        // ­ƒöÑ AQUI EST├ü O QUE ESTAVA FALTANDO
        if (paciente.getCabeloAntes() != null) {
            dto.setCabeloAntesId(
                    paciente.getCabeloAntes().getId()
            );
        } else {
            dto.setCabeloAntesId(null);
        }

        return dto;
    }
}

