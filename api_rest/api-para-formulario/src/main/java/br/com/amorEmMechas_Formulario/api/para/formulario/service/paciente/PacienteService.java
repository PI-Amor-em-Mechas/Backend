package br.com.amorEmMechas_Formulario.api.para.formulario.service.paciente;

import br.com.amorEmMechas_Formulario.api.para.formulario.dto.paciente.PacienteRequestDto;
import br.com.amorEmMechas_Formulario.api.para.formulario.dto.paciente.PacienteResponseDto;
import br.com.amorEmMechas_Formulario.api.para.formulario.entity.arquivo.Arquivo;
import br.com.amorEmMechas_Formulario.api.para.formulario.entity.endereco.Endereco;
import br.com.amorEmMechas_Formulario.api.para.formulario.entity.filho.Filho;
import br.com.amorEmMechas_Formulario.api.para.formulario.entity.paciente.Paciente;
import br.com.amorEmMechas_Formulario.api.para.formulario.entity.solicitante.Solicitante;
import br.com.amorEmMechas_Formulario.api.para.formulario.exception.IdNotFoundException;
import br.com.amorEmMechas_Formulario.api.para.formulario.mapper.paciente.PacienteMapper;
import br.com.amorEmMechas_Formulario.api.para.formulario.repository.arquivo.ArquivoRepository;
import br.com.amorEmMechas_Formulario.api.para.formulario.repository.endereco.EnderecoRepository;
import br.com.amorEmMechas_Formulario.api.para.formulario.repository.filho.FilhoRepository;
import br.com.amorEmMechas_Formulario.api.para.formulario.repository.paciente.PacienteRepository;
import br.com.amorEmMechas_Formulario.api.para.formulario.repository.solicitante.SolicitanteRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class PacienteService {

    private final PacienteRepository repository;
    private final PacienteMapper mapper;
    private final EnderecoRepository enderecoRepository;
    private final ArquivoRepository arquivoRepository;
    private final FilhoRepository filhoRepository;
    private final SolicitanteRepository solicitanteRepository;


    public PacienteService(ArquivoRepository arquivoRepository, PacienteRepository repository, PacienteMapper mapper, EnderecoRepository enderecoRepository, FilhoRepository filhoRepository, SolicitanteRepository solicitanteRepository) {
        this.arquivoRepository = arquivoRepository;
        this.repository = repository;
        this.mapper = mapper;
        this.enderecoRepository = enderecoRepository;
        this.filhoRepository = filhoRepository;
        this.solicitanteRepository = solicitanteRepository;
    }

    public PacienteResponseDto create(PacienteRequestDto dto) {

        Endereco endereco = enderecoRepository.findById(dto.getEnderecoId())
                .orElseThrow(() ->
                        new IdNotFoundException(
                                "ID ENDERECO: "
                                        + dto.getEnderecoId()
                                        + " N├úo existe"
                        )
                );

        Solicitante solicitante = solicitanteRepository
                .findById(dto.getSolicitanteId())
                .orElseThrow(() ->
                        new IdNotFoundException(
                                "ID SOLICITANTE: "
                                        + dto.getSolicitanteId()
                                        + " N├úo Encontrado"
                        )
                );

        dto.setDtPedido(LocalDate.now());

        Paciente paciente = mapper.toEntity(dto);

        paciente.setEndereco(endereco);
        paciente.setSolicitante(solicitante);

        // Ô£à CABELO ANTES (corrigido e mais limpo)
        if (dto.getCabeloAntesId() != null) {

            Arquivo arquivo = arquivoRepository
                    .findById(dto.getCabeloAntesId().longValue())
                    .orElseThrow(() ->
                            new IdNotFoundException(
                                    "ID ARQUIVO (CABELO ANTES): "
                                            + dto.getCabeloAntesId()
                                            + " N├úo Encontrado"
                            )
                    );

            paciente.setCabeloAntes(arquivo);
        }

        // Ô£à FILHOS (corrigido null safety)
        if (Boolean.TRUE.equals(dto.getTemFilhos())
                && dto.getIdadesFilhos() != null
                && !dto.getIdadesFilhos().isEmpty()) {

            List<Filho> filhos = dto.getIdadesFilhos()
                    .stream()
                    .map(idade -> {

                        Filho f = new Filho();
                        f.setIdade(idade);
                        f.setPaciente(paciente);

                        return f;
                    })
                    .toList();

            paciente.setFilhos(new ArrayList<>(filhos));

        } else {
            paciente.setFilhos(new ArrayList<>());
        }

        // Ô£à quantidade sempre consistente
        paciente.setQtdFilhos(paciente.getFilhos().size());

        Paciente saved = repository.save(paciente);

        return mapper.toResponse(saved);
    }

    public PacienteResponseDto update(Integer id, PacienteRequestDto pacienteDTO) {
        Paciente paciente = repository.findById(id)
                .orElseThrow(() -> new IdNotFoundException("ID " + id + " N├úo Encontrado"));

        if (pacienteDTO.getNomeCompleto() != null) {
            paciente.setNomeCompleto(pacienteDTO.getNomeCompleto());
        }
        if (pacienteDTO.getCpf() != null) {
            paciente.setCpf(pacienteDTO.getCpf());
        }
        if (pacienteDTO.getEnderecoId() != null) {
            Endereco endereco = enderecoRepository.findById(pacienteDTO.getEnderecoId())
                    .orElseThrow(() -> new IdNotFoundException("ID ENDERECO: " + pacienteDTO.getEnderecoId() + " N├úo Encontrado"));
            paciente.setEndereco(endereco);
        }

        if (pacienteDTO.getTemFilhos() != null) {
            paciente.setTemFilhos(pacienteDTO.getTemFilhos());
            if (pacienteDTO.getTemFilhos() && pacienteDTO.getIdadesFilhos() != null) {
                List<Filho> filhos = pacienteDTO.getIdadesFilhos().stream()
                        .map(idade -> {
                            Filho f = new Filho();
                            f.setIdade(idade);
                            f.setPaciente(paciente);
                            return filhoRepository.save(f);
                        })
                        .toList();
                paciente.setFilhos(filhos);
            }
        }

        if (pacienteDTO.getCabeloAntesId() != null) {

            Arquivo arquivo = arquivoRepository
                    .findById(pacienteDTO.getCabeloAntesId().longValue())
                    .orElseThrow(() ->
                            new IdNotFoundException(
                                    "ID ARQUIVO: "
                                            + pacienteDTO.getCabeloAntesId()
                                            + " N├úo Encontrado"
                            )
                    );

            paciente.setCabeloAntes(arquivo);
        }

        // ­ƒö╣ recalcular qtdFilhos antes de salvar
        paciente.setQtdFilhos(paciente.getFilhos() != null ? paciente.getFilhos().size() : 0);

        Paciente atualizado = repository.save(paciente);
        return mapper.toResponse(atualizado);
    }

    public List<PacienteResponseDto> findAll() {
        return repository.findAll()
                .stream()
                .map(mapper::toResponse)
                .toList();
    }

    public List<PacienteResponseDto> findWithFilters(String estado, LocalDate dataInicio, LocalDate dataFim) {
        return repository.findWithFilters(estado, dataInicio, dataFim)
                .stream()
                .map(mapper::toResponse)
                .toList();
    }

    public PacienteResponseDto findById(Integer id) {
        Paciente paciente = repository.findById(id)
                .orElseThrow(() -> new IdNotFoundException("ID PACIENTE: " + id + " N├úo Encontrado"));
        return mapper.toResponse(paciente);
    }

    public void deleteById(Integer id) {
        if (!repository.existsById(id)) {
            throw new IdNotFoundException("ID PACIENTE: " + id + " N├úo Encontrado");
        }
        repository.deleteById(id);
    }
}



