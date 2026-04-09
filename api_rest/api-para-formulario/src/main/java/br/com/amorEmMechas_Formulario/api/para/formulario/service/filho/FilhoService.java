package br.com.amorEmMechas_Formulario.api.para.formulario.service.filho;


import br.com.amorEmMechas_Formulario.api.para.formulario.dto.filho.FilhoRequestDto;
import br.com.amorEmMechas_Formulario.api.para.formulario.dto.filho.FilhoResponseDto;
import br.com.amorEmMechas_Formulario.api.para.formulario.entity.filho.Filho;
import br.com.amorEmMechas_Formulario.api.para.formulario.exception.IdNotFoundException;
import br.com.amorEmMechas_Formulario.api.para.formulario.mapper.filho.FilhoMapper;
import br.com.amorEmMechas_Formulario.api.para.formulario.repository.filho.FilhoRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class FilhoService {

    private FilhoRepository repository;
    private FilhoMapper mapper;

    public FilhoService(FilhoMapper mapper, FilhoRepository repository) {
        this.mapper = mapper;
        this.repository = repository;
    }


    public FilhoResponseDto create(FilhoRequestDto dto) {
        Filho f = mapper.toEntity(dto);
        Filho saved = repository.save(f);
        return mapper.toResponse(saved);
    }

    public List<FilhoResponseDto> findAll() {
        return repository.findAll()
                .stream()
                .map(filho -> {
                    int qtd = repository.countByPacienteId(
                            filho.getPaciente() != null ? filho.getPaciente().getId() : null);
                    return new FilhoResponseDto(filho, qtd);
                })
                .toList();
    }

    public FilhoResponseDto findById(Integer id) {
        Filho filho = repository.findById(id)
                .orElseThrow(() -> new IdNotFoundException("ID FILHO: " + id + " Não Encontrado"));
        int qtd = repository.countByPacienteId(
                filho.getPaciente() != null ? filho.getPaciente().getId() : null);
        return new FilhoResponseDto(filho, qtd);
    }

    public void deleteById(Integer id) {
        if (!repository.existsById(id)) {
            throw new IdNotFoundException("ID FILHO: " + id + " Não Encontrado");
        }
        repository.deleteById(id);
    }
}
