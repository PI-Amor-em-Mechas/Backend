package br.com.amorEmMechas_Formulario.api.para.formulario.controller;

import br.com.amorEmMechas_Formulario.api.para.formulario.entity.paciente.Paciente;
import br.com.amorEmMechas_Formulario.api.para.formulario.repository.paciente.PacienteRepository;
import br.com.amorEmMechas_Formulario.api.para.formulario.security.audit.AuditLog;
import br.com.amorEmMechas_Formulario.api.para.formulario.security.audit.AuditLogRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.hasItems;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class SolicitacaoSecurityIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired PacienteRepository pacientes;
    @Autowired AuditLogRepository auditoria;

    @Test
    void anonimoNaoListaDetalhaOuAltera() throws Exception {
        mvc.perform(get("/solicitacoes")).andExpect(status().isUnauthorized());
        mvc.perform(get("/solicitacoes/1")).andExpect(status().isUnauthorized());
        mvc.perform(patch("/solicitacoes/1/status").contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"CONCLUIDA\"}")).andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "USER")
    void usuarioSemPerfilDaEquipeNaoAcessa() throws Exception {
        mvc.perform(get("/solicitacoes")).andExpect(status().isForbidden());
        mvc.perform(get("/solicitacoes/1")).andExpect(status().isForbidden());
        mvc.perform(patch("/solicitacoes/1/status").contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"CANCELADA\"}")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ATENDENTE")
    void atendenteConsultaEEncerraPedido() throws Exception {
        Paciente paciente = new Paciente();
        paciente.setNomeCompleto("Fluxo atendente");
        Integer id = pacientes.saveAndFlush(paciente).getId();
        mvc.perform(get("/solicitacoes")).andExpect(status().isOk());
        mvc.perform(get("/solicitacoes/{id}", id)).andExpect(status().isOk());
        mvc.perform(patch("/solicitacoes/{id}/status", id).contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"CONCLUIDA\"}")).andExpect(status().isOk());
        mvc.perform(get("/solicitacoes/{id}", id)).andExpect(jsonPath("$.status").value("CONCLUIDA"));
        assertThat(auditoria.findAll()).anySatisfy(log -> {
            assertThat(log.getRecurso()).isEqualTo("/solicitacoes/" + id + "/status");
            assertThat(log.getTipoEvento()).isEqualTo(AuditLog.TipoEvento.ALTERACAO_PACIENTE);
            assertThat(log.getMetodoHttp()).isEqualTo("PATCH");
        });
    }

    @Test
    void swaggerPublicaContratoPaginacaoEsquemasEAutenticacao() throws Exception {
        mvc.perform(get("/v3/api-docs")).andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/solicitacoes'].get.parameters[*].name", hasItems("busca", "page", "size", "sort")))
                .andExpect(jsonPath("$.paths['/solicitacoes/{id}'].get.responses['404']").exists())
                .andExpect(jsonPath("$.paths['/solicitacoes/{id}/status'].patch.responses['409']").exists())
                .andExpect(jsonPath("$.paths['/solicitacoes'].get.security[0].bearerAuth").isArray())
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.scheme").value("bearer"))
                .andExpect(jsonPath("$.components.schemas.SolicitacaoDetalheDto").exists())
                .andExpect(jsonPath("$.components.schemas.StatusSolicitacaoRequestDto").exists());
    }
}
