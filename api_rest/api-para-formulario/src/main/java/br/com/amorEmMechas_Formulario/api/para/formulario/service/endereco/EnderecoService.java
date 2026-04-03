package br.com.amorEmMechas_Formulario.api.para.formulario.service.endereco;


import br.com.amorEmMechas_Formulario.api.para.formulario.dto.endereco.EnderecoRequestDto;
import br.com.amorEmMechas_Formulario.api.para.formulario.dto.endereco.EnderecoResponseDto;
import br.com.amorEmMechas_Formulario.api.para.formulario.entity.endereco.Endereco;
import br.com.amorEmMechas_Formulario.api.para.formulario.mapper.endereco.EnderecoMapper;
import br.com.amorEmMechas_Formulario.api.para.formulario.repository.endereco.EnderecoRepository;
import org.springframework.stereotype.Service;

@Service
public class EnderecoService {




    private EnderecoRepository repository;
    private EnderecoMapper mapper;

    public EnderecoService(EnderecoMapper mapper, EnderecoRepository repository) {
        this.mapper = mapper;
        this.repository = repository;
    }



    public EnderecoResponseDto create (EnderecoRequestDto dto){
        Endereco e = mapper.toEntity(dto);
        Endereco saved = repository.save(e);
        return mapper.toResponse(e);
    }

}
