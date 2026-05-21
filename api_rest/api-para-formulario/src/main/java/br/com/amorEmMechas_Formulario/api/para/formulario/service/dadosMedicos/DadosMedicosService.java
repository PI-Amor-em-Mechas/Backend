package br.com.amorEmMechas_Formulario.api.para.formulario.service.dadosMedicos;

import br.com.amorEmMechas_Formulario.api.para.formulario.dto.dadosMedicos.DadosMedicosRequestDto;
import br.com.amorEmMechas_Formulario.api.para.formulario.dto.dadosMedicos.DadosMedicosResponseDto;
import br.com.amorEmMechas_Formulario.api.para.formulario.entity.arquivo.Arquivo;
import br.com.amorEmMechas_Formulario.api.para.formulario.entity.dadosMedicos.DadosMedicos;
import br.com.amorEmMechas_Formulario.api.para.formulario.entity.paciente.Paciente;
import br.com.amorEmMechas_Formulario.api.para.formulario.exception.IdNotFoundException;
import br.com.amorEmMechas_Formulario.api.para.formulario.mapper.dadosMedicos.DadosMedicosMapper;
import br.com.amorEmMechas_Formulario.api.para.formulario.repository.arquivo.ArquivoRepository;
import br.com.amorEmMechas_Formulario.api.para.formulario.repository.dadosMedicos.DadosMedicosRepository;
import br.com.amorEmMechas_Formulario.api.para.formulario.repository.paciente.PacienteRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DadosMedicosService {

    private final DadosMedicosRepository repository;
    private final DadosMedicosMapper mapper;
    private final ArquivoRepository arquivoRepository;
    private final PacienteRepository pacienteRepository;

    public DadosMedicosService(ArquivoRepository arquivoRepository, DadosMedicosRepository repository, DadosMedicosMapper mapper, PacienteRepository pacienteRepository) {
        this.arquivoRepository = arquivoRepository;
        this.repository = repository;
        this.mapper = mapper;
        this.pacienteRepository = pacienteRepository;
    }

    public DadosMedicosResponseDto create(DadosMedicosRequestDto dto) {

        DadosMedicos entity = mapper.toEntity(dto);

        // NOVO
        if (dto.getRelatorioMedicoId() != null) {

            Arquivo arquivo = arquivoRepository
                    .findById(dto.getRelatorioMedicoId().longValue())
                    .orElseThrow(() ->
                            new IdNotFoundException(
                                    "ID ARQUIVO: "
                                            + dto.getRelatorioMedicoId()
                                            + " Não Encontrado"
                            )
                    );

            entity.setArquivo(arquivo);
        }

        Paciente paciente = pacienteRepository.findById(dto.getPacienteId())
                .orElseThrow(() ->
                        new IdNotFoundException(
                                "ID PACIENTE: " + dto.getPacienteId() + " não encontrado"
                        )
                );

        entity.setPaciente(paciente);

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
                .orElseThrow(() ->
                        new IdNotFoundException(
                                "ID DADOS MÉDICOS: "
                                        + id
                                        + " Não Encontrado"
                        )
                );

        return mapper.toResponse(entity);
    }

    public void deleteById(Integer id) {

        if (!repository.existsById(id)) {

            throw new IdNotFoundException(
                    "ID DADOS MÉDICOS: "
                            + id
                            + " Não Encontrado"
            );
        }

        repository.deleteById(id);
    }

    public DadosMedicosResponseDto update(
            Integer id,
            DadosMedicosRequestDto dto
    ) {

        DadosMedicos dadosMedicos = repository.findById(id)
                .orElseThrow(() ->
                        new IdNotFoundException(
                                "ID "
                                        + id
                                        + " Não foi encontrado"
                        )
                );

        dadosMedicos.setMotivo(dto.getMotivo());
        dadosMedicos.setTipoCancer(dto.getTipoCancer());
        dadosMedicos.setJustificativa(dto.getJustificativa());
        dadosMedicos.setDtInicioTratamento(dto.getDtInicioTratamento());
        dadosMedicos.setTipoAtendimento(dto.getTipoAtendimento());

        // NOVO
        if (dto.getRelatorioMedicoId() != null) {

            Arquivo arquivo = arquivoRepository
                    .findById(dto.getRelatorioMedicoId().longValue())
                    .orElseThrow(() ->
                            new IdNotFoundException(
                                    "ID ARQUIVO: "
                                            + dto.getRelatorioMedicoId()
                                            + " Não Encontrado"
                            )
                    );

            dadosMedicos.setArquivo(arquivo);
        }

        DadosMedicos dadosSave = repository.save(dadosMedicos);

        return mapper.toResponse(dadosSave);
    }
}