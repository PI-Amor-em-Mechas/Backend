package br.com.amorEmMechas_Formulario.api.para.formulario.service.paciente;


import br.com.amorEmMechas_Formulario.api.para.formulario.dto.paciente.PacienteRequestDto;
import br.com.amorEmMechas_Formulario.api.para.formulario.dto.paciente.PacienteResponseDto;
import br.com.amorEmMechas_Formulario.api.para.formulario.entity.dadosMedicos.DadosMedicos;
import br.com.amorEmMechas_Formulario.api.para.formulario.entity.endereco.Endereco;
import br.com.amorEmMechas_Formulario.api.para.formulario.entity.filho.Filho;
import br.com.amorEmMechas_Formulario.api.para.formulario.entity.paciente.Paciente;
import br.com.amorEmMechas_Formulario.api.para.formulario.exception.IdNotFoundException;
import br.com.amorEmMechas_Formulario.api.para.formulario.mapper.paciente.PacienteMapper;
import br.com.amorEmMechas_Formulario.api.para.formulario.repository.dadosMedicos.DadosMedicosRepository;
import br.com.amorEmMechas_Formulario.api.para.formulario.repository.endereco.EnderecoRepository;
import br.com.amorEmMechas_Formulario.api.para.formulario.repository.filho.FilhoRepository;
import br.com.amorEmMechas_Formulario.api.para.formulario.repository.paciente.PacienteRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class PacienteService {

    private final PacienteRepository repository;
    private final PacienteMapper mapper;
    private final EnderecoRepository enderecoRepository;
    private final DadosMedicosRepository dadosMedicosRepository;
    private final FilhoRepository filhoRepository;

    public PacienteService(DadosMedicosRepository dadosMedicosRepository,
                           EnderecoRepository enderecoRepository,
                           FilhoRepository filhoRepository,
                           PacienteMapper mapper,
                           PacienteRepository repository) {
        this.dadosMedicosRepository = dadosMedicosRepository;
        this.enderecoRepository = enderecoRepository;
        this.filhoRepository = filhoRepository;
        this.mapper = mapper;
        this.repository = repository;
    }

    public PacienteResponseDto create(PacienteRequestDto dto) {
        Endereco endereco = enderecoRepository.findById(dto.getEnderecoId())
                .orElseThrow(() -> new IdNotFoundException("ID ENDERECO: " + dto.getEnderecoId() + " Não existe"));
        DadosMedicos dadosMedicos = dadosMedicosRepository.findById(dto.getDadosMedicosId())
                .orElseThrow(() -> new IdNotFoundException("ID DADOS MÉDICOS: " + dto.getDadosMedicosId() + " Não existe"));
        dto.setDtPedido(LocalDate.now());

        Paciente paciente = mapper.toEntity(dto);
        paciente.setEndereco(endereco);
        paciente.setDadosMedicos(dadosMedicos);
        Paciente saved = repository.save(paciente);

        if (dto.getTemFilhos() != null && dto.getTemFilhos() && dto.getIdadesFilhos() != null) {
            List<Filho> filhos = dto.getIdadesFilhos().stream()
                    .map(idade -> {
                        Filho f = new Filho();
                        f.setIdade(idade);
                        f.setPaciente(saved);
                        return filhoRepository.save(f);
                    })
                    .toList();
            saved.setFilhos(filhos);
        }

        // 🔹 recalcular qtdFilhos antes de retornar
        saved.setQtdFilhos(saved.getFilhos() != null ? saved.getFilhos().size() : 0);
        repository.save(saved);

        return mapper.toResponse(saved);
    }

    public PacienteResponseDto update(Integer id, PacienteRequestDto pacienteDTO) {
        Paciente paciente = repository.findById(id)
                .orElseThrow(() -> new IdNotFoundException("ID " + id + " Não Encontrado"));

        if (pacienteDTO.getNomeCompleto() != null) {
            paciente.setNomeCompleto(pacienteDTO.getNomeCompleto());
        }
        if (pacienteDTO.getCpf() != null) {
            paciente.setCpf(pacienteDTO.getCpf());
        }
        if (pacienteDTO.getEnderecoId() != null) {
            Endereco endereco = enderecoRepository.findById(pacienteDTO.getEnderecoId())
                    .orElseThrow(() -> new IdNotFoundException("ID ENDERECO: " + pacienteDTO.getEnderecoId() + " Não Encontrado"));
            paciente.setEndereco(endereco);
        }
        if (pacienteDTO.getDadosMedicosId() != null) {
            DadosMedicos dadosMedicos = dadosMedicosRepository.findById(pacienteDTO.getDadosMedicosId())
                    .orElseThrow(() -> new IdNotFoundException("ID DADOS MÉDICOS: " + pacienteDTO.getDadosMedicosId() + " Não Encontrado"));
            paciente.setDadosMedicos(dadosMedicos);
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

        // 🔹 recalcular qtdFilhos antes de salvar
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

    public PacienteResponseDto findById(Integer id) {
        Paciente paciente = repository.findById(id)
                .orElseThrow(() -> new IdNotFoundException("ID PACIENTE: " + id + " Não Encontrado"));
        return mapper.toResponse(paciente);
    }

    public void deleteById(Integer id) {
        if (!repository.existsById(id)) {
            throw new IdNotFoundException("ID PACIENTE: " + id + " Não Encontrado");
        }
        repository.deleteById(id);
    }
}



