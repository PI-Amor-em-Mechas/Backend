package br.com.amorEmMechas_Formulario.api.para.formulario.service.kitAmor;


import br.com.amorEmMechas_Formulario.api.para.formulario.dto.kitAmor.KitAmorRequestDto;
import br.com.amorEmMechas_Formulario.api.para.formulario.dto.kitAmor.KitAmorResponseDto;
import br.com.amorEmMechas_Formulario.api.para.formulario.entity.kitAmor.KitAmor;
import br.com.amorEmMechas_Formulario.api.para.formulario.entity.paciente.Paciente;
import br.com.amorEmMechas_Formulario.api.para.formulario.entity.solicitante.Solicitante;
import br.com.amorEmMechas_Formulario.api.para.formulario.exception.IdNotFoundException;
import br.com.amorEmMechas_Formulario.api.para.formulario.mapper.kitAmor.KitAmorMapper;
import br.com.amorEmMechas_Formulario.api.para.formulario.repository.kitAmor.KitAmorRepository;
import br.com.amorEmMechas_Formulario.api.para.formulario.repository.paciente.PacienteRepository;
import br.com.amorEmMechas_Formulario.api.para.formulario.repository.solicitante.SolicitanteRepository;
import org.springframework.stereotype.Service;

@Service
public class KitAmorService {


    private KitAmorMapper mapper;
    private KitAmorRepository repository;
    private SolicitanteRepository solicitanteRepository;
    private PacienteRepository pacienteRepository;

    public KitAmorService(KitAmorMapper mapper, PacienteRepository pacienteRepository, KitAmorRepository repository, SolicitanteRepository solicitanteRepository) {
        this.mapper = mapper;
        this.pacienteRepository = pacienteRepository;
        this.repository = repository;
        this.solicitanteRepository = solicitanteRepository;
    }

    public KitAmorResponseDto create (KitAmorRequestDto dto){
        Solicitante solicitante = solicitanteRepository.findById(dto.getSolicitanteId())
                .orElseThrow(() -> new IdNotFoundException("ID SOLICITANTE: "
                        + dto.getSolicitanteId() + " Não Encontrado"));

        Paciente paciente = pacienteRepository.findById(dto.getPacienteId())
                .orElseThrow(()
                        ->new IdNotFoundException
                        ("ID PACIENTE: " + dto.getPacienteId() + " Não Encontrado"));

    KitAmor kitaAmor = mapper.toEntity(dto);
    kitaAmor.setSolicitante(solicitante);
    kitaAmor.setPaciente(paciente);
    KitAmor saved = repository.save(kitaAmor);
    return mapper.toResponse(saved);

}

}
