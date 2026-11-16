package br.com.amorEmMechas_Formulario.api.para.formulario.service.madrinha;

import br.com.amorEmMechas_Formulario.api.para.formulario.dto.madrinha.MadrinhaRequestDto;
import br.com.amorEmMechas_Formulario.api.para.formulario.dto.madrinha.MadrinhaResponseDto;
import br.com.amorEmMechas_Formulario.api.para.formulario.entity.madrinha.Madrinha;
import br.com.amorEmMechas_Formulario.api.para.formulario.exception.IdNotFoundException;
import br.com.amorEmMechas_Formulario.api.para.formulario.mapper.madrinha.MadrinhaMapper;
import br.com.amorEmMechas_Formulario.api.para.formulario.repository.madrinha.MadrinhaRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.Duration;
import java.time.LocalDateTime;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

@Service
public class MadrinhaService {

    private final MadrinhaRepository repository;
    private final MadrinhaMapper mapper;

    public MadrinhaService(MadrinhaRepository repository, MadrinhaMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    public MadrinhaResponseDto create(MadrinhaRequestDto dto) {
        if (dto.getColaboradorId() != null && !dto.getColaboradorId().isBlank()) {
            Optional<Madrinha> existing = repository.findByColaboradorId(dto.getColaboradorId());
            if (existing.isPresent()) {
                return toResponseWithPontoHours(existing.get());
            }
        }
        Madrinha entity = mapper.toEntity(dto);
        if (entity.getDataCadastro() == null) {
            entity.setDataCadastro(LocalDate.now());
        }
        Madrinha saved = repository.save(entity);
        return mapper.toResponse(saved);
    }

    public MadrinhaResponseDto update(Integer id, MadrinhaRequestDto dto) {
        Madrinha entity = repository.findById(id)
                .orElseThrow(() -> new IdNotFoundException("ID MADRINHA: " + id + " N├úo Encontrado"));

        if (dto.getNomeCompleto() != null) entity.setNomeCompleto(dto.getNomeCompleto());
        if (dto.getEmail() != null) entity.setEmail(dto.getEmail());
        if (dto.getHorasVoluntarias() != null) entity.setHorasVoluntarias(dto.getHorasVoluntarias());
        if (dto.getFuncao() != null) entity.setFuncao(dto.getFuncao());
        if (dto.getStatus() != null) entity.setStatus(dto.getStatus());
        if (dto.getColaboradorId() != null) entity.setColaboradorId(dto.getColaboradorId());

        Madrinha updated = repository.save(entity);
        return toResponseWithPontoHours(updated);
    }

    public List<MadrinhaResponseDto> findAll() {
        synchronizeFacialEmployees();
        return repository.findAll().stream()
                .map(this::toResponseWithPontoHours)
                .toList();
    }

    public List<MadrinhaResponseDto> findByStatus(String status) {
        synchronizeFacialEmployees();
        return repository.findByStatus(status).stream()
                .map(this::toResponseWithPontoHours)
                .toList();
    }

    public MadrinhaResponseDto findById(Integer id) {
        Madrinha entity = repository.findById(id)
                .orElseThrow(() -> new IdNotFoundException("ID MADRINHA: " + id + " N├úo Encontrado"));
        return toResponseWithPontoHours(entity);
    }

    public void deleteById(Integer id) {
        if (!repository.existsById(id)) {
            throw new IdNotFoundException("ID MADRINHA: " + id + " N├úo Encontrado");
        }
        repository.deleteById(id);
    }

    private MadrinhaResponseDto toResponseWithPontoHours(Madrinha entity) {
        MadrinhaResponseDto response = mapper.toResponse(entity);
        if (entity.getColaboradorId() == null || entity.getColaboradorId().isBlank()) {
            return response;
        }

        long totalMinutes = 0;
        LocalDateTime openIn = null;
        for (Object[] point : repository.findPontosByColaboradorId(entity.getColaboradorId())) {
            LocalDateTime timestamp = toLocalDateTime(point[0]);
            String type = String.valueOf(point[1]);
            if ("IN".equalsIgnoreCase(type)) {
                openIn = timestamp;
            } else if ("OUT".equalsIgnoreCase(type) && openIn != null && !timestamp.isBefore(openIn)) {
                totalMinutes += Duration.between(openIn, timestamp).toMinutes();
                openIn = null;
            }
        }

        int minutes = Math.toIntExact(totalMinutes);
        response.setHorasVoluntarias(minutes / 60);
        response.setMinutosVoluntarios(minutes);
        response.setDuracaoVoluntaria(formatDuration(minutes));
        return response;
    }

    private void synchronizeFacialEmployees() {
        for (Object[] facialEmployee : repository.findColaboradoresFaciaisAtivos()) {
            String employeeId = String.valueOf(facialEmployee[0]);
            if (repository.findByColaboradorId(employeeId).isPresent()) {
                continue;
            }

            Madrinha madrinha = new Madrinha();
            madrinha.setNomeCompleto(String.valueOf(facialEmployee[1]));
            madrinha.setColaboradorId(employeeId);
            madrinha.setHorasVoluntarias(0);
            madrinha.setDataCadastro(toLocalDate(facialEmployee[2]));
            madrinha.setStatus("Ativa");
            madrinha.setFuncao("Voluntária");
            repository.save(madrinha);
        }
    }

    private LocalDate toLocalDate(Object value) {
        if (value instanceof java.sql.Timestamp timestamp) {
            return timestamp.toLocalDateTime().toLocalDate();
        }
        if (value instanceof java.sql.Date date) {
            return date.toLocalDate();
        }
        return LocalDate.parse(String.valueOf(value).substring(0, 10));
    }

    private String formatDuration(int minutes) {
        if (minutes < 60) {
            return minutes + "min";
        }
        int hours = minutes / 60;
        int remainingMinutes = minutes % 60;
        return remainingMinutes == 0
                ? hours + "h"
                : hours + "h " + remainingMinutes + "min";
    }

    private LocalDateTime toLocalDateTime(Object value) {
        if (value instanceof Timestamp timestamp) {
            return timestamp.toLocalDateTime();
        }
        if (value instanceof LocalDateTime localDateTime) {
            return localDateTime;
        }
        return LocalDateTime.parse(String.valueOf(value).replace(' ', 'T'));
    }
}
