package br.com.amorEmMechas_Formulario.api.para.formulario.service.dadosMedicos;


import br.com.amorEmMechas_Formulario.api.para.formulario.dto.dadosMedicos.DadosMedicosRequestDto;
import br.com.amorEmMechas_Formulario.api.para.formulario.dto.dadosMedicos.DadosMedicosResponseDto;
import br.com.amorEmMechas_Formulario.api.para.formulario.entity.dadosMedicos.DadosMedicos;
import br.com.amorEmMechas_Formulario.api.para.formulario.exception.IdNotFoundException;
import br.com.amorEmMechas_Formulario.api.para.formulario.mapper.dadosMedicos.DadosMedicosMapper;
import br.com.amorEmMechas_Formulario.api.para.formulario.repository.dadosMedicos.DadosMedicosRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DadosMedicosService {


    private DadosMedicosRepository repository;
    private DadosMedicosMapper mapper;

    public DadosMedicosService(DadosMedicosMapper mapper, DadosMedicosRepository repository) {
        this.mapper = mapper;
        this.repository = repository;
    }


    public DadosMedicosResponseDto create(DadosMedicosRequestDto dto) {
        DadosMedicos entity = mapper.toEntity(dto);
        DadosMedicos saved = repository.save(entity);
        return mapper.toResponse(saved);
    }

    public List<DadosMedicosResponseDto> findAll() {
        return repository.findAll()
                .stream()
                .map(mapper::toResponse)
                .toList();
    }

    public DadosMedicosResponseDto findById(Integer id) {
        DadosMedicos entity = repository.findById(id)
                .orElseThrow(() -> new IdNotFoundException("ID DADOS MÉDICOS: " + id + " Não Encontrado"));
        return mapper.toResponse(entity);
    }

    public void deleteById(Integer id) {
        if (!repository.existsById(id)) {
            throw new IdNotFoundException("ID DADOS MÉDICOS: " + id + " Não Encontrado");
        }
        repository.deleteById(id);
    }
}
