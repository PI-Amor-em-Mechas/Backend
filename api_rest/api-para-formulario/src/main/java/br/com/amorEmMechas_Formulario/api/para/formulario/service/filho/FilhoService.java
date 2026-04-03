package br.com.amorEmMechas_Formulario.api.para.formulario.service.filho;


import br.com.amorEmMechas_Formulario.api.para.formulario.dto.filho.FilhoRequestDto;
import br.com.amorEmMechas_Formulario.api.para.formulario.dto.filho.FilhoResponseDto;
import br.com.amorEmMechas_Formulario.api.para.formulario.entity.filho.Filho;
import br.com.amorEmMechas_Formulario.api.para.formulario.mapper.filho.FilhoMapper;
import br.com.amorEmMechas_Formulario.api.para.formulario.repository.filho.FilhoRepository;
import org.springframework.stereotype.Service;

@Service
public class FilhoService {

    private FilhoRepository repository;
    private FilhoMapper mapper;

    public FilhoService(FilhoMapper mapper, FilhoRepository repository) {
        this.mapper = mapper;
        this.repository = repository;
    }


    public FilhoResponseDto create (FilhoRequestDto dto){
        Filho f = mapper.toEntity(dto);
        Filho saved = repository.save(f);
        return mapper.toResponse(saved);
    }
}
