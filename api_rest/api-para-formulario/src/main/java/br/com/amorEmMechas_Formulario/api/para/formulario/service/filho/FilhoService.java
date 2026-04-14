package br.com.amorEmMechas_Formulario.api.para.formulario.service.filho;


import br.com.amorEmMechas_Formulario.api.para.formulario.dto.filho.FilhoRequestDto;
import br.com.amorEmMechas_Formulario.api.para.formulario.dto.filho.FilhoResponseDto;
import br.com.amorEmMechas_Formulario.api.para.formulario.entity.filho.Filho;
<<<<<<< HEAD
import br.com.amorEmMechas_Formulario.api.para.formulario.entity.paciente.Paciente;
=======
>>>>>>> dabbcabc21c2bdc15faaac9021171cabe08cf69f
import br.com.amorEmMechas_Formulario.api.para.formulario.exception.IdNotFoundException;
import br.com.amorEmMechas_Formulario.api.para.formulario.mapper.filho.FilhoMapper;
import br.com.amorEmMechas_Formulario.api.para.formulario.repository.filho.FilhoRepository;
import br.com.amorEmMechas_Formulario.api.para.formulario.repository.paciente.PacienteRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class FilhoService {

    private FilhoRepository repository;
    private FilhoMapper mapper;
    private PacienteRepository pacienteRepository;

    public FilhoService(FilhoMapper mapper, PacienteRepository pacienteRepository, FilhoRepository repository) {
        this.mapper = mapper;
        this.pacienteRepository = pacienteRepository;
        this.repository = repository;
    }

<<<<<<< HEAD
    public FilhoResponseDto create (FilhoRequestDto dto){
=======

    public FilhoResponseDto create(FilhoRequestDto dto) {
>>>>>>> dabbcabc21c2bdc15faaac9021171cabe08cf69f
        Filho f = mapper.toEntity(dto);
        Filho saved = repository.save(f);
        return mapper.toResponse(saved);
    }

<<<<<<< HEAD

    public FilhoResponseDto update (Integer id, FilhoRequestDto dto){
        Filho filhoBuscado = repository.findById(id).orElseThrow(() -> new IdNotFoundException("ID " + id + " Não Encontrado"));

        Paciente paciente = pacienteRepository.findById(dto.getPacienteId()).orElseThrow(() -> new IdNotFoundException("ID " + id + " Não Encontrado"));

        filhoBuscado.setIdade(dto.getIdade());
        filhoBuscado.setPaciente(paciente);
        Filho filhoSave = repository.save(filhoBuscado);
        return mapper.toResponse(filhoSave);
    }

    public List<FilhoResponseDto> updateMany(List<FilhoRequestDto> filhosDto) {
        return filhosDto.stream().map(dto -> {
            Filho filho = repository.findById(dto.getId())
                    .orElseThrow(() -> new IdNotFoundException("Filho com ID " + dto.getId() + " não encontrado"));

            Paciente paciente = pacienteRepository.findById(dto.getPacienteId())
                    .orElseThrow(() -> new IdNotFoundException("Paciente com ID " + dto.getPacienteId() + " não encontrado"));

            filho.setIdade(dto.getIdade());
            filho.setPaciente(paciente);

            Filho atualizado = repository.save(filho);
            return mapper.toResponse(atualizado);
        }).toList();
=======
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
>>>>>>> dabbcabc21c2bdc15faaac9021171cabe08cf69f
    }
}
