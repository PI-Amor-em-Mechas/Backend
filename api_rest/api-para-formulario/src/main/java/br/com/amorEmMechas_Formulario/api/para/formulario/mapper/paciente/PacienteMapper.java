package br.com.amorEmMechas_Formulario.api.para.formulario.mapper.paciente;

import br.com.amorEmMechas_Formulario.api.para.formulario.dto.filho.FilhoResponseDto;
import br.com.amorEmMechas_Formulario.api.para.formulario.dto.paciente.PacienteRequestDto;
import br.com.amorEmMechas_Formulario.api.para.formulario.dto.paciente.PacienteResponseDto;
import br.com.amorEmMechas_Formulario.api.para.formulario.entity.filho.Filho;
import br.com.amorEmMechas_Formulario.api.para.formulario.entity.paciente.Paciente;
import br.com.amorEmMechas_Formulario.api.para.formulario.mapper.endereco.EnderecoMapper;
import br.com.amorEmMechas_Formulario.api.para.formulario.mapper.dadosMedicos.DadosMedicosMapper;
import br.com.amorEmMechas_Formulario.api.para.formulario.mapper.filho.FilhoMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class PacienteMapper {

    @Autowired
    private EnderecoMapper enderecoMapper;

    @Autowired
    private DadosMedicosMapper dadosMedicosMapper;

    @Autowired
    private FilhoMapper filhoMapper;

    public PacienteResponseDto toResponse(Paciente paciente) {
        if (paciente == null) return null;

        PacienteResponseDto dto = new PacienteResponseDto();
        dto.setId(paciente.getId());
        dto.setNomeCompleto(paciente.getNomeCompleto());
        dto.setEmail(paciente.getEmail());
        dto.setDtPedido(paciente.getDtPedido());
        dto.setCel(paciente.getCel());
        dto.setDtNasc(paciente.getDtNasc());
        dto.setEstadoCivil(paciente.getEstadoCivil());
        dto.setTemFilhos(paciente.getTemFilhos());
        dto.setQtdPessoasEmCasa(paciente.getQtdPessoasEmCasa());
        dto.setCpf(paciente.getCpf());

        if (paciente.getCabeloAntes() != null) {
            dto.setCabeloAntesBase64(Base64.getEncoder().encodeToString(paciente.getCabeloAntes()));
        }

        dto.setEndereco(enderecoMapper.toResponse(paciente.getEndereco()));
        dto.setDadosMedicos(dadosMedicosMapper.toResponse(paciente.getDadosMedicos()));
        dto.setFilhos(filhoMapper.toResponseList(paciente.getFilhos()));

        if (paciente.getFilhos() != null) {
            List<FilhoResponseDto> filhosDto = new ArrayList<>();
            for (Filho f : paciente.getFilhos()) {
                FilhoResponseDto fr = new FilhoResponseDto();
                fr.setId(f.getId());
                fr.setIdade(f.getIdade());
                filhosDto.add(fr);
            }
            dto.setFilhos(filhosDto);
        }



        return dto;
    }

    public Paciente toEntity(PacienteRequestDto dto) {
        if (dto == null) return null;

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

        if (dto.getCabeloAntesBase64() != null) {
            paciente.setCabeloAntes(Base64.getDecoder().decode(dto.getCabeloAntesBase64()));
        }


        return paciente;
    }
}
