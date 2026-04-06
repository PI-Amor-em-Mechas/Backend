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

    private PacienteRepository repository;
    private PacienteMapper mapper;
    private EnderecoRepository enderecoRepository;
    private DadosMedicosRepository dadosMedicosRepository;
    private FilhoRepository filhoRepository;

    public PacienteService(DadosMedicosRepository dadosMedicosRepository, EnderecoRepository enderecoRepository, FilhoRepository filhoRepository, PacienteMapper mapper, PacienteRepository repository) {
        this.dadosMedicosRepository = dadosMedicosRepository;
        this.enderecoRepository = enderecoRepository;
        this.filhoRepository = filhoRepository;
        this.mapper = mapper;
        this.repository = repository;
    }

    public PacienteResponseDto create (PacienteRequestDto dto){
        Endereco endereco = enderecoRepository.findById(dto.getEnderecoId()).orElseThrow(() -> new IdNotFoundException("ID ENDERECO: " + dto.getEnderecoId() + " Não existe"));
        DadosMedicos dadosMedicos = dadosMedicosRepository.findById(dto.getDadosMedicosId()).orElseThrow(() -> new IdNotFoundException("ID DADOS MÉDICOS: " + dto.getDadosMedicosId() + " Não existe"));
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


        return mapper.toResponse(saved);

    }

    public PacienteResponseDto findById(Integer id) {
        Paciente paciente = repository.findById(id).orElseThrow(() -> new RuntimeException("Paciente não encontrado"));
        return mapper.toResponse(paciente);
    }


}
