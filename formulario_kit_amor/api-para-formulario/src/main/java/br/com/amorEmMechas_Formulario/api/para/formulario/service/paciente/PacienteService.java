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
import br.com.amorEmMechas_Formulario.api.para.formulario.security.PhiEncryptionUtil;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class PacienteService {

    private final PacienteRepository repository;
    private final PacienteMapper mapper;
    private final EnderecoRepository enderecoRepository;
    private final ArquivoRepository arquivoRepository;
    private final FilhoRepository filhoRepository;
    private final SolicitanteRepository solicitanteRepository;
    private final PhiEncryptionUtil phiEncryptionUtil;

    public PacienteService(ArquivoRepository arquivoRepository,
                           PacienteRepository repository,
                           PacienteMapper mapper,
                           EnderecoRepository enderecoRepository,
                           FilhoRepository filhoRepository,
                           SolicitanteRepository solicitanteRepository,
                           PhiEncryptionUtil phiEncryptionUtil) {
        this.arquivoRepository = arquivoRepository;
        this.repository = repository;
        this.mapper = mapper;
        this.enderecoRepository = enderecoRepository;
        this.filhoRepository = filhoRepository;
        this.solicitanteRepository = solicitanteRepository;
        this.phiEncryptionUtil = phiEncryptionUtil;
    }

    @Transactional
    public PacienteResponseDto create(PacienteRequestDto dto) {
        // LGPD Art. 7, 8 - Consentimento obrigatorio
        if (!Boolean.TRUE.equals(dto.getConsentimentoLgpd())) {
            throw new IllegalArgumentException("Consentimento LGPD e obrigatorio para cadastro de dados pessoais");
        }

        Endereco endereco = buscarEndereco(dto.getEnderecoId());
        // solicitanteId e opcional no formulario (sem @NotNull no DTO) - so busca se foi enviado,
        // senao o cadastro publico quebrava com 500 sempre que o campo vinha nulo
        Solicitante solicitante = dto.getSolicitanteId() != null ? buscarSolicitante(dto.getSolicitanteId()) : null;

        dto.setDtPedido(LocalDate.now());

        Paciente paciente = mapper.toEntity(dto);
        paciente.setEndereco(endereco);
        paciente.setSolicitante(solicitante);

        // LGPD - Registrar consentimento
        paciente.setConsentimentoLgpd(true);
        paciente.setDtConsentimento(LocalDateTime.now());
        paciente.setFinalidadeTratamento("Cadastro para confeccao de peruca oncologica - Projeto Amor em Mechas");

        // Criptografar CPF (dado sensivel - HL7/LGPD)
        if (dto.getCpf() != null) {
            paciente.setCpf(phiEncryptionUtil.encrypt(dto.getCpf()));
        }

        vincularCabeloAntes(paciente, dto.getCabeloAntesId());
        vincularFilhos(paciente, dto);

        paciente.setQtdFilhos(paciente.getFilhos().size());

        Paciente saved = repository.save(paciente);
        PacienteResponseDto responseDto = mapper.toResponse(saved);
        responseDto.setCpf(mascaraCpf(saved.getCpf()));
        return responseDto;
    }

    @Transactional
    public PacienteResponseDto update(Integer id, PacienteRequestDto pacienteDTO) {
        Paciente paciente = repository.findById(id)
                .orElseThrow(() -> new IdNotFoundException("ID " + id + " Nao Encontrado"));

        if (pacienteDTO.getNomeCompleto() != null) {
            paciente.setNomeCompleto(pacienteDTO.getNomeCompleto());
        }
        if (pacienteDTO.getCpf() != null) {
            paciente.setCpf(phiEncryptionUtil.encrypt(pacienteDTO.getCpf()));
        }
        if (pacienteDTO.getEnderecoId() != null) {
            Endereco endereco = buscarEndereco(pacienteDTO.getEnderecoId());
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

        vincularCabeloAntes(paciente, pacienteDTO.getCabeloAntesId());

        paciente.setQtdFilhos(paciente.getFilhos() != null ? paciente.getFilhos().size() : 0);

        Paciente atualizado = repository.save(paciente);
        PacienteResponseDto dto = mapper.toResponse(atualizado);
        dto.setCpf(mascaraCpf(atualizado.getCpf()));
        return dto;
    }

    @Transactional(readOnly = true)
    public Page<PacienteResponseDto> findAll(Pageable pageable) {
        return repository.findAll(pageable)
                .map(p -> {
                    PacienteResponseDto dto = mapper.toResponse(p);
                    dto.setCpf(mascaraCpf(p.getCpf()));
                    return dto;
                });
    }

    @Transactional(readOnly = true)
    public Page<PacienteResponseDto> findWithFilters(String estado, LocalDate dataInicio, LocalDate dataFim, Pageable pageable) {
        return repository.findWithFilters(estado, dataInicio, dataFim, pageable)
                .map(p -> {
                    PacienteResponseDto dto = mapper.toResponse(p);
                    dto.setCpf(mascaraCpf(p.getCpf()));
                    return dto;
                });
    }

    @Transactional(readOnly = true)
    public PacienteResponseDto findById(Integer id) {
        Paciente paciente = repository.findById(id)
                .orElseThrow(() -> new IdNotFoundException("ID PACIENTE: " + id + " Nao Encontrado"));
        PacienteResponseDto dto = mapper.toResponse(paciente);
        dto.setCpf(mascaraCpf(paciente.getCpf()));
        return dto;
    }

    @Transactional
    public void deleteById(Integer id) {
        if (!repository.existsById(id)) {
            throw new IdNotFoundException("ID PACIENTE: " + id + " Nao Encontrado");
        }
        repository.deleteById(id);
    }

    /**
     * LGPD Art. 18, VI - Direito de eliminacao/anonimizacao.
     * Anonimiza os dados pessoais em vez de excluir o registro.
     */
    @Transactional
    public void anonimizar(Integer id) {
        Paciente paciente = repository.findById(id)
                .orElseThrow(() -> new IdNotFoundException("ID PACIENTE: " + id + " Nao Encontrado"));

        paciente.setNomeCompleto("[ANONIMIZADO]");
        paciente.setEmail("[ANONIMIZADO]");
        paciente.setCpf("[ANONIMIZADO]");
        paciente.setCel("[ANONIMIZADO]");
        paciente.setDtNasc(null);
        paciente.setEstadoCivil(null);
        paciente.setConsentimentoLgpd(false);
        paciente.setDadosAnonimizados(true);
        paciente.setDtAnonimizacao(LocalDateTime.now());

        // Anonimizar dados medicos vinculados
        if (paciente.getDadosMedicos() != null) {
            paciente.getDadosMedicos().forEach(dm -> {
                dm.setMotivo("[ANONIMIZADO]");
                dm.setTipoCancer("[ANONIMIZADO]");
                dm.setJustificativa("[ANONIMIZADO]");
            });
        }

        repository.save(paciente);
    }

    /**
     * LGPD Art. 8, §5 - Revogacao de consentimento.
     */
    @Transactional
    public void revogarConsentimento(Integer id) {
        Paciente paciente = repository.findById(id)
                .orElseThrow(() -> new IdNotFoundException("ID PACIENTE: " + id + " Nao Encontrado"));

        paciente.setConsentimentoLgpd(false);
        paciente.setDtConsentimento(null);
        repository.save(paciente);
    }

    /**
     * LGPD Art. 18, V - Direito de portabilidade.
     * Exporta todos os dados pessoais do titular em formato estruturado.
     */
    @Transactional(readOnly = true)
    public Map<String, Object> exportarDados(Integer id) {
        Paciente paciente = repository.findById(id)
                .orElseThrow(() -> new IdNotFoundException("ID PACIENTE: " + id + " Nao Encontrado"));

        Map<String, Object> dados = new LinkedHashMap<>();
        dados.put("id", paciente.getId());
        dados.put("nomeCompleto", paciente.getNomeCompleto());
        dados.put("email", paciente.getEmail());
        dados.put("celular", paciente.getCel());
        dados.put("cpf", paciente.getCpf() != null ? mascaraCpf(paciente.getCpf()) : null);
        dados.put("dataNascimento", paciente.getDtNasc());
        dados.put("estadoCivil", paciente.getEstadoCivil());
        dados.put("temFilhos", paciente.getTemFilhos());
        dados.put("qtdFilhos", paciente.getQtdFilhos());
        dados.put("qtdPessoasEmCasa", paciente.getQtdPessoasEmCasa());
        dados.put("dataPedido", paciente.getDtPedido());
        dados.put("consentimentoLgpd", paciente.getConsentimentoLgpd());
        dados.put("dataConsentimento", paciente.getDtConsentimento());
        dados.put("finalidadeTratamento", paciente.getFinalidadeTratamento());

        if (paciente.getEndereco() != null) {
            Map<String, Object> endereco = new LinkedHashMap<>();
            endereco.put("cep", paciente.getEndereco().getCep());
            endereco.put("logradouro", paciente.getEndereco().getRua());
            endereco.put("numero", paciente.getEndereco().getNumero());
            endereco.put("bairro", paciente.getEndereco().getBairro());
            endereco.put("cidade", paciente.getEndereco().getCidade());
            endereco.put("estado", paciente.getEndereco().getEstado());
            dados.put("endereco", endereco);
        }

        if (paciente.getDadosMedicos() != null && !paciente.getDadosMedicos().isEmpty()) {
            List<Map<String, Object>> dadosMedicosList = paciente.getDadosMedicos().stream()
                    .map(dm -> {
                        Map<String, Object> dml = new LinkedHashMap<>();
                        dml.put("motivo", dm.getMotivo());
                        dml.put("tipoCancer", dm.getTipoCancer());
                        dml.put("justificativa", dm.getJustificativa());
                        dml.put("tipoAtendimento", dm.getTipoAtendimento());
                        dml.put("dtInicioTratamento", dm.getDtInicioTratamento());
                        return dml;
                    }).toList();
            dados.put("dadosMedicos", dadosMedicosList);
        }

        return dados;
    }

    /**
     * Mascara CPF para exibicao: ***.***.***-XX
     */
    public String mascaraCpf(String cpfCriptografado) {
        try {
            String cpfDecifrado = phiEncryptionUtil.decrypt(cpfCriptografado);
            if (cpfDecifrado.length() >= 11) {
                return "***.***.***-" + cpfDecifrado.substring(9);
            }
            return "***";
        } catch (Exception e) {
            return "***.***.***-**";
        }
    }

    private Endereco buscarEndereco(Integer enderecoId) {
        return enderecoRepository.findById(enderecoId)
                .orElseThrow(() -> new IdNotFoundException("ID ENDERECO: " + enderecoId + " Nao existe"));
    }

    private Solicitante buscarSolicitante(Integer solicitanteId) {
        return solicitanteRepository.findById(solicitanteId)
                .orElseThrow(() -> new IdNotFoundException("ID SOLICITANTE: " + solicitanteId + " Nao Encontrado"));
    }

    private void vincularCabeloAntes(Paciente paciente, Integer cabeloAntesId) {
        if (cabeloAntesId != null) {
            Arquivo arquivo = arquivoRepository
                    .findById(cabeloAntesId.longValue())
                    .orElseThrow(() -> new IdNotFoundException(
                            "ID ARQUIVO (CABELO ANTES): " + cabeloAntesId + " Nao Encontrado"));
            paciente.setCabeloAntes(arquivo);
        }
    }

    private void vincularFilhos(Paciente paciente, PacienteRequestDto dto) {
        if (Boolean.TRUE.equals(dto.getTemFilhos())
                && dto.getIdadesFilhos() != null
                && !dto.getIdadesFilhos().isEmpty()) {

            List<Filho> filhos = dto.getIdadesFilhos().stream()
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
    }
}