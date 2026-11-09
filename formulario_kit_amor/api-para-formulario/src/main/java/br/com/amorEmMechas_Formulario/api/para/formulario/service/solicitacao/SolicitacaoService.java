package br.com.amorEmMechas_Formulario.api.para.formulario.service.solicitacao;

import br.com.amorEmMechas_Formulario.api.para.formulario.dto.solicitacao.SolicitacaoDetalheDto;
import br.com.amorEmMechas_Formulario.api.para.formulario.dto.solicitacao.SolicitacaoResumoDto;
import br.com.amorEmMechas_Formulario.api.para.formulario.entity.paciente.Paciente;
import br.com.amorEmMechas_Formulario.api.para.formulario.entity.paciente.StatusSolicitacao;
import br.com.amorEmMechas_Formulario.api.para.formulario.exception.IdNotFoundException;
import br.com.amorEmMechas_Formulario.api.para.formulario.repository.kitAmor.KitAmorRepository;
import br.com.amorEmMechas_Formulario.api.para.formulario.repository.paciente.PacienteRepository;
import br.com.amorEmMechas_Formulario.api.para.formulario.service.paciente.PacienteService;
import br.com.amorEmMechas_Formulario.api.para.formulario.security.PhiEncryptionUtil;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional(readOnly = true)
public class SolicitacaoService {
    private final PacienteRepository pacientes;
    private final KitAmorRepository kits;
    private final PacienteService pacienteService;
    private final PhiEncryptionUtil encryption;

    public SolicitacaoService(PacienteRepository pacientes, KitAmorRepository kits, PacienteService pacienteService,
                              PhiEncryptionUtil encryption) {
        this.pacientes = pacientes;
        this.kits = kits;
        this.pacienteService = pacienteService;
        this.encryption = encryption;
    }

    public Page<SolicitacaoResumoDto> listarAtivas(String busca, Pageable pageable) {
        return pacientes.findSolicitacoesAtivas(StatusSolicitacao.ATIVA, busca.strip(), pageable)
                .map(this::resumo);
    }

    public SolicitacaoDetalheDto detalhar(Integer id) {
        Paciente paciente = pacientes.findById(id)
                .filter(p -> !Boolean.TRUE.equals(p.getDadosAnonimizados()))
                .orElseThrow(() -> new IdNotFoundException("Solicitacao nao encontrada"));
        var dadosPaciente = pacienteService.findById(id);
        dadosPaciente.setCpf(encryption.decrypt(paciente.getCpf()));
        return new SolicitacaoDetalheDto(paciente.getId(), paciente.getStatusSolicitacao(),
                dadosPaciente,
                paciente.getSolicitante() == null ? null : paciente.getSolicitante().getNomeCompleto(),
                kits.findByPacienteIdOrderByIdAsc(id).stream()
                        .map(kit -> new SolicitacaoDetalheDto.KitResumo(kit.getId(), kit.getCorPeruca())).toList());
    }

    @Transactional
    public SolicitacaoResumoDto encerrar(Integer id, StatusSolicitacao status) {
        if (status == null || status == StatusSolicitacao.ATIVA) {
            throw new IllegalArgumentException("Informe CONCLUIDA ou CANCELADA para encerrar o pedido");
        }
        Paciente paciente = pacientes.findSolicitacaoParaAtualizar(id)
                .orElseThrow(() -> new IdNotFoundException("Solicitacao nao encontrada"));
        if (paciente.getStatusSolicitacao() != StatusSolicitacao.ATIVA) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A solicitacao ja foi encerrada");
        }
        paciente.setStatusSolicitacao(status);
        return resumo(pacientes.save(paciente));
    }

    private SolicitacaoResumoDto resumo(Paciente paciente) {
        var endereco = paciente.getEndereco();
        return new SolicitacaoResumoDto(paciente.getId(), paciente.getNomeCompleto(), paciente.getEmail(),
                paciente.getDtPedido(), endereco == null ? null : endereco.getCidade(),
                endereco == null ? null : endereco.getEstado(), paciente.getStatusSolicitacao());
    }
}
