package br.com.amorEmMechas_Formulario.api.para.formulario.service.madrinha;

import br.com.amorEmMechas_Formulario.api.para.formulario.dto.madrinha.MadrinhaRequestDto;
import br.com.amorEmMechas_Formulario.api.para.formulario.dto.madrinha.MadrinhaResponseDto;
import br.com.amorEmMechas_Formulario.api.para.formulario.entity.madrinha.Madrinha;
import br.com.amorEmMechas_Formulario.api.para.formulario.entity.madrinha.StatusMadrinha;
import br.com.amorEmMechas_Formulario.api.para.formulario.exception.IdNotFoundException;
import br.com.amorEmMechas_Formulario.api.para.formulario.mapper.madrinha.MadrinhaMapper;
import br.com.amorEmMechas_Formulario.api.para.formulario.repository.madrinha.MadrinhaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;

@Service
public class MadrinhaService {

    private final MadrinhaRepository repository;
    private final MadrinhaMapper mapper;

    public MadrinhaService(MadrinhaRepository repository, MadrinhaMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    public MadrinhaResponseDto create(MadrinhaRequestDto dto) {
        validateStatus(dto.getStatus());
        Madrinha entity = mapper.toEntity(dto);
        if (entity.getDataCadastro() == null) {
            entity.setDataCadastro(LocalDate.now());
        }
        Madrinha saved = repository.save(entity);
        return toValidatedResponse(saved);
    }

    public MadrinhaResponseDto update(Integer id, MadrinhaRequestDto dto) {
        if (dto.getStatus() != null) {
            validateStatus(dto.getStatus());
        }
        Madrinha entity = repository.findById(id)
                .orElseThrow(() -> new IdNotFoundException("ID MADRINHA: " + id + " N├úo Encontrado"));

        if (dto.getNomeCompleto() != null) entity.setNomeCompleto(dto.getNomeCompleto());
        if (dto.getEmail() != null) entity.setEmail(dto.getEmail());
        if (dto.getHorasVoluntarias() != null) entity.setHorasVoluntarias(dto.getHorasVoluntarias());
        if (dto.getFuncao() != null) entity.setFuncao(dto.getFuncao());
        if (dto.getStatus() != null) entity.setStatus(dto.getStatus());

        Madrinha updated = repository.save(entity);
        return toValidatedResponse(updated);
    }

    public List<MadrinhaResponseDto> findAll() {
        return repository.findByStatusIn(StatusMadrinha.apiValues()).stream()
                .filter(entity -> StatusMadrinha.isAllowed(entity.getStatus()))
            .map(this::toValidatedResponse)
                .toList();
    }

    public List<MadrinhaResponseDto> findByStatus(String status) {
        validateStatus(status);
        return repository.findByStatus(status).stream()
            .filter(entity -> status.equals(entity.getStatus()))
            .map(this::toValidatedResponse)
                .toList();
    }

    public MadrinhaResponseDto findById(Integer id) {
        Madrinha entity = repository.findById(id)
                .orElseThrow(() -> new IdNotFoundException("ID MADRINHA: " + id + " N├úo Encontrado"));
        return toValidatedResponse(entity);
    }

    public void deleteById(Integer id) {
        if (!repository.existsById(id)) {
            throw new IdNotFoundException("ID MADRINHA: " + id + " N├úo Encontrado");
        }
        repository.deleteById(id);
    }

    private void validateStatus(String status) {
        if (!StatusMadrinha.isAllowed(status)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Status deve ser Ativa, Afastada ou Desassociada"
            );
        }
    }

    private MadrinhaResponseDto toValidatedResponse(Madrinha entity) {
        if (!StatusMadrinha.isAllowed(entity.getStatus())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Status legado da madrinha requer revisao manual"
            );
        }
        return mapper.toResponse(entity);
    }
}
