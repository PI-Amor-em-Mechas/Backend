package br.com.amorEmMechas_Formulario.api.para.formulario.service.dadosMedicos;


import br.com.amorEmMechas_Formulario.api.para.formulario.dto.dadosMedicos.DadosMedicosRequestDto;
import br.com.amorEmMechas_Formulario.api.para.formulario.dto.dadosMedicos.DadosMedicosResponseDto;
import br.com.amorEmMechas_Formulario.api.para.formulario.entity.dadosMedicos.DadosMedicos;
import br.com.amorEmMechas_Formulario.api.para.formulario.mapper.dadosMedicos.DadosMedicosMapper;
import br.com.amorEmMechas_Formulario.api.para.formulario.repository.dadosMedicos.DadosMedicosRepository;
import org.springframework.stereotype.Service;

@Service
public class DadosMedicosService {


    private DadosMedicosRepository repository;
    private DadosMedicosMapper mapper;

    public DadosMedicosService(DadosMedicosMapper mapper, DadosMedicosRepository repository) {
        this.mapper = mapper;
        this.repository = repository;
    }


    public DadosMedicosResponseDto create (DadosMedicosRequestDto dto){
        DadosMedicos entity = mapper.toEntity(dto);
        DadosMedicos saved = repository.save(entity);
        return mapper.toResponse(saved);

    }
}
