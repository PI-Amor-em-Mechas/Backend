package br.com.amorEmMechas_Formulario.api.para.formulario.controller;

import br.com.amorEmMechas_Formulario.api.para.formulario.entity.endereco.Endereco;
import br.com.amorEmMechas_Formulario.api.para.formulario.entity.dadosMedicos.DadosMedicos;
import br.com.amorEmMechas_Formulario.api.para.formulario.entity.kitAmor.KitAmor;
import br.com.amorEmMechas_Formulario.api.para.formulario.entity.paciente.Paciente;
import br.com.amorEmMechas_Formulario.api.para.formulario.entity.paciente.StatusSolicitacao;
import br.com.amorEmMechas_Formulario.api.para.formulario.repository.paciente.PacienteRepository;
import br.com.amorEmMechas_Formulario.api.para.formulario.security.PhiEncryptionUtil;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = "hl7.security.encryption.key=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=")
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@WithMockUser(roles = "ADMIN")
class SolicitacaoIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired PacienteRepository pacientes;
    @Autowired EntityManager em;
    @Autowired PhiEncryptionUtil encryption;

    private Paciente criar(String nome, StatusSolicitacao status) {
        Paciente paciente = new Paciente();
        paciente.setNomeCompleto(nome);
        paciente.setEmail(UUID.randomUUID() + "@example.test");
        paciente.setDtPedido(LocalDate.now());
        paciente.setStatusSolicitacao(status);
        return pacientes.saveAndFlush(paciente);
    }

    @Test
    void paginaBuscaTodoBancoIncluiLegadosEExcluiEncerradosEAnonimizados() throws Exception {
        String termo = UUID.randomUUID().toString();
        criar(termo + " A", StatusSolicitacao.ATIVA);
        Paciente legado = criar(termo + " B", null);
        criar(termo + " C", StatusSolicitacao.CONCLUIDA);
        criar(termo + " D", StatusSolicitacao.CANCELADA);
        Paciente anonimo = criar(termo + " E", StatusSolicitacao.ATIVA);
        anonimo.setDadosAnonimizados(true);
        pacientes.saveAndFlush(anonimo);
        em.clear();

        mvc.perform(get("/solicitacoes").param("busca", termo.toUpperCase()).param("size", "1"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.content[0].id").value(legado.getId()))
                .andExpect(jsonPath("$.content[0].status").value("ATIVA"));
        mvc.perform(get("/solicitacoes").param("busca", termo).param("size", "1").param("page", "1"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content.length()").value(1));
        mvc.perform(get("/solicitacoes").param("busca", termo + " inexistente"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content.length()").value(0));
    }

    @Test
    void detalhaCpfCompletoSomenteNaSolicitacaoEMantemCriptografia() throws Exception {
        Paciente paciente = criar("Paciente de teste", StatusSolicitacao.ATIVA);
        paciente.setCpf(encryption.encrypt("12345678909"));
        paciente.setCel("11999999999");
        Endereco endereco = new Endereco();
        endereco.setCidade("Sao Paulo");
        endereco.setEstado("SP");
        paciente.setEndereco(endereco);
        pacientes.saveAndFlush(paciente);
        KitAmor kit = new KitAmor();
        kit.setPaciente(paciente);
        kit.setCorPeruca("Castanha");
        em.persist(kit);
        DadosMedicos medico = new DadosMedicos();
        medico.setPaciente(paciente);
        medico.setMotivo("Tratamento de teste");
        medico.setTipoAtendimento("SUS");
        em.persist(medico);
        em.flush();
        em.clear();

        mvc.perform(get("/solicitacoes/{id}", paciente.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paciente.nomeCompleto").value("Paciente de teste"))
                .andExpect(jsonPath("$.paciente.cel").value("11999999999"))
                .andExpect(jsonPath("$.paciente.cpf").value("12345678909"))
                .andExpect(jsonPath("$.paciente.endereco.cidade").value("Sao Paulo"))
                .andExpect(jsonPath("$.paciente.dadosMedicos.motivo").value("Tratamento de teste"))
                .andExpect(jsonPath("$.paciente.dadosMedicos.tipoAtendimento").value("SUS"))
                .andExpect(jsonPath("$.kits[0].corPeruca").value("Castanha"))
                .andExpect(jsonPath("$.status").value("ATIVA"));
        mvc.perform(get("/pacientes/{id}", paciente.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cpf").value("***.***.***-09"));
        em.clear();
        String cpfPersistido = pacientes.findById(paciente.getId()).orElseThrow().getCpf();
        assertThat(cpfPersistido).isNotEqualTo("12345678909");
        assertThat(encryption.decrypt(cpfPersistido)).isEqualTo("12345678909");
    }

    @Test
    void conclusaoPersisteRemoveDaFilaEPreservaPaciente() throws Exception {
        verificarEncerramento(StatusSolicitacao.CONCLUIDA);
    }

    @Test
    void cancelamentoPersisteRemoveDaFilaEPreservaPaciente() throws Exception {
        verificarEncerramento(StatusSolicitacao.CANCELADA);
    }

    private void verificarEncerramento(StatusSolicitacao status) throws Exception {
        Paciente paciente = criar(UUID.randomUUID().toString(), StatusSolicitacao.ATIVA);
        mvc.perform(patch("/solicitacoes/{id}/status", paciente.getId())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"" + status + "\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value(status.name()));
        em.flush();
        em.clear();
        assertThat(pacientes.findById(paciente.getId()).orElseThrow().getStatusSolicitacao()).isEqualTo(status);
        mvc.perform(get("/solicitacoes").param("busca", paciente.getNomeCompleto()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(0));
        mvc.perform(get("/solicitacoes/{id}", paciente.getId()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value(status.name()));
        mvc.perform(patch("/solicitacoes/{id}/status", paciente.getId())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"CANCELADA\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void statusInvalidoNaoMudaPedidoEAnonimizadoNaoPodeSerConsultado() throws Exception {
        Paciente paciente = criar("Validacao", StatusSolicitacao.ATIVA);
        for (String corpo : new String[]{"{}", "{\"status\":\"ATIVA\"}", "{\"status\":\"INVALIDA\"}"}) {
            mvc.perform(patch("/solicitacoes/{id}/status", paciente.getId())
                            .contentType(MediaType.APPLICATION_JSON).content(corpo))
                    .andExpect(status().isBadRequest());
        }
        em.flush();
        em.clear();
        assertThat(pacientes.findById(paciente.getId()).orElseThrow().getStatusSolicitacao()).isEqualTo(StatusSolicitacao.ATIVA);
        paciente = pacientes.findById(paciente.getId()).orElseThrow();
        paciente.setDadosAnonimizados(true);
        pacientes.saveAndFlush(paciente);
        mvc.perform(get("/solicitacoes/{id}", paciente.getId())).andExpect(status().isNotFound());
        mvc.perform(patch("/solicitacoes/{id}/status", paciente.getId())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"CONCLUIDA\"}"))
                .andExpect(status().isNotFound());
        mvc.perform(get("/solicitacoes/2147483647")).andExpect(status().isNotFound());
    }

    @Test
    void novoCadastroTemStatusAtivo() {
        Paciente paciente = new Paciente();
        paciente.setNomeCompleto("Novo cadastro");
        Integer id = pacientes.saveAndFlush(paciente).getId();
        em.clear();
        assertThat(pacientes.findById(id).orElseThrow().getStatusSolicitacao()).isEqualTo(StatusSolicitacao.ATIVA);
    }
}
