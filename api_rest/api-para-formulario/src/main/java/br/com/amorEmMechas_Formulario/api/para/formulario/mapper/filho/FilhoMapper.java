package br.com.amorEmMechas_Formulario.api.para.formulario.mapper.filho;

import br.com.amorEmMechas_Formulario.api.para.formulario.dto.filho.FilhoRequestDto;
import br.com.amorEmMechas_Formulario.api.para.formulario.entity.filho.Filho;
import br.com.amorEmMechas_Formulario.api.para.formulario.entity.paciente.Paciente;
import org.springframework.stereotype.Component;

@Component
public class FilhoMapper {


    public Filho toEntity (FilhoRequestDto dto, Paciente paciente){
        if (dto == null) return null;

        Filho filho = new Filho();
        filho.setIdade(dto.getIdade());
        filho.setPaciente(paciente);

        return filho;
    }

}
