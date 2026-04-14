package br.com.amorEmMechas_Formulario.api.para.formulario.service.endereco;


import br.com.amorEmMechas_Formulario.api.para.formulario.dto.endereco.EnderecoRequestDto;
import br.com.amorEmMechas_Formulario.api.para.formulario.dto.endereco.EnderecoResponseDto;
import br.com.amorEmMechas_Formulario.api.para.formulario.entity.endereco.Endereco;
import br.com.amorEmMechas_Formulario.api.para.formulario.exception.IdNotFoundException;
import br.com.amorEmMechas_Formulario.api.para.formulario.mapper.endereco.EnderecoMapper;
import br.com.amorEmMechas_Formulario.api.para.formulario.repository.endereco.EnderecoRepository;
import org.springframework.stereotype.Service;

import java.util.List;

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
        return mapper.toResponse(saved);
    }

    public EnderecoResponseDto update(Integer id, EnderecoRequestDto dto) {
        Endereco e = repository.findById(id).orElseThrow(() -> new IdNotFoundException("ID: " + id + " Não Encontrado"));

            e.setRua(dto.getRua());
            e.setBairro(dto.getBairro());
            e.setCep(dto.getCep());
            e.setCidade(dto.getCidade());
            e.setEstado(dto.getEstado());
            e.setNumero(dto.getNumero());

            Endereco endereco = repository.save(e);
            return mapper.toResponse(endereco);
    }

    public List<EnderecoResponseDto> findAll() {
        return repository.findAll()
                .stream()
                .map(mapper::toResponse)
                .toList();
    }

    public EnderecoResponseDto findById(Integer id) {
        Endereco e = repository.findById(id)
                .orElseThrow(() -> new IdNotFoundException("ID ENDEREÇO: " + id + " Não Encontrado"));
        return mapper.toResponse(e);
    }

    public void deleteById(Integer id) {
        if (!repository.existsById(id)) {
            throw new IdNotFoundException("ID ENDEREÇO: " + id + " Não Encontrado");
        }
        repository.deleteById(id);
    }

}
