package br.com.amorEmMechas_Formulario.api.para.formulario.service.paciente;


import br.com.amorEmMechas_Formulario.api.para.formulario.dto.paciente.PacienteRequestDto;
import br.com.amorEmMechas_Formulario.api.para.formulario.dto.paciente.PacienteResponseDto;
import br.com.amorEmMechas_Formulario.api.para.formulario.entity.paciente.Paciente;
import br.com.amorEmMechas_Formulario.api.para.formulario.mapper.paciente.PacienteMapper;
import br.com.amorEmMechas_Formulario.api.para.formulario.repository.PacienteRepository;
import org.springframework.stereotype.Service;

@Service
public class PacienteService {

    private PacienteRepository repository;
    private PacienteMapper mapper;

    public PacienteService(PacienteMapper mapper, PacienteRepository repository) {
        this.mapper = mapper;
        this.repository = repository;
    }

    public PacienteResponseDto create (PacienteRequestDto dto){
        Paciente paciente = mapper.toEntity(dto);
        paciente = repository.save(paciente);
       return mapper.toResponse(paciente);

    }

    public PacienteResponseDto findById(Integer id) {
        Paciente paciente = repository.findById(id).orElseThrow(() -> new RuntimeException("Paciente não encontrado"));
        return mapper.toResponse(paciente);
    }


}
