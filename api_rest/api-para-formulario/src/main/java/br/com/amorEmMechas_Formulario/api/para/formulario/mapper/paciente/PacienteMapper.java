package br.com.amorEmMechas_Formulario.api.para.formulario.mapper.paciente;

import br.com.amorEmMechas_Formulario.api.para.formulario.dto.paciente.PacienteRequestDto;
import br.com.amorEmMechas_Formulario.api.para.formulario.dto.paciente.PacienteResponseDto;
import br.com.amorEmMechas_Formulario.api.para.formulario.entity.paciente.Paciente;
import br.com.amorEmMechas_Formulario.api.para.formulario.mapper.dadosMedicos.DadosMedicosMapper;
import br.com.amorEmMechas_Formulario.api.para.formulario.mapper.endereco.EnderecoMapper;
import br.com.amorEmMechas_Formulario.api.para.formulario.mapper.filho.FilhoMapper;
import org.springframework.stereotype.Component;

<<<<<<< HEAD
=======
import java.util.Base64;

>>>>>>> dabbcabc21c2bdc15faaac9021171cabe08cf69f
@Component
public class PacienteMapper {

    private final EnderecoMapper enderecoMapper;
    private final DadosMedicosMapper dadosMedicosMapper;
    private final FilhoMapper filhoMapper;

    public PacienteMapper(EnderecoMapper enderecoMapper,
                          DadosMedicosMapper dadosMedicosMapper,
                          FilhoMapper filhoMapper) {
        this.enderecoMapper = enderecoMapper;
        this.dadosMedicosMapper = dadosMedicosMapper;
        this.filhoMapper = filhoMapper;
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

        if (dto.getCabeloAntes() != null) {
            entity.setCabeloAntes(dto.getCabeloAntes().getBytes());
        }

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

        if (paciente.getCabeloAntes() != null) {
            dto.setCabeloAntes(new String(paciente.getCabeloAntes()));
        } else {
            dto.setCabeloAntes("");
        }

<<<<<<< HEAD
        if (paciente.getEndereco() != null) {
            dto.setEndereco(enderecoMapper.toResponse(paciente.getEndereco()));
        }
        if (paciente.getDadosMedicos() != null) {
            dto.setDadosMedicos(dadosMedicosMapper.toResponse(paciente.getDadosMedicos()));
        }
        if (paciente.getFilhos() != null) {
            dto.setFilhos(filhoMapper.toResponseList(paciente.getFilhos()));
            dto.setQtdFilho(paciente.getFilhos().size());
        } else {
            dto.setQtdFilho(0);
        }
=======
        dto.setEndereco(enderecoMapper.toResponse(paciente.getEndereco()));
        dto.setDadosMedicos(dadosMedicosMapper.toResponse(paciente.getDadosMedicos()));
        dto.setFilhos(filhoMapper.toResponseList(paciente.getFilhos()));


>>>>>>> dabbcabc21c2bdc15faaac9021171cabe08cf69f

        return dto;
    }
}
