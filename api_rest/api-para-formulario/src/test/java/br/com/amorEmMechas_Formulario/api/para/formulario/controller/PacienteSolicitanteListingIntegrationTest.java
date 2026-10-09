package br.com.amorEmMechas_Formulario.api.para.formulario.controller;

import br.com.amorEmMechas_Formulario.api.para.formulario.entity.paciente.Paciente;
import br.com.amorEmMechas_Formulario.api.para.formulario.entity.solicitante.Solicitante;
import br.com.amorEmMechas_Formulario.api.para.formulario.entity.avaliacao.Avaliacao;
import br.com.amorEmMechas_Formulario.api.para.formulario.repository.avaliacao.AvaliacaoRepository;
import br.com.amorEmMechas_Formulario.api.para.formulario.repository.paciente.PacienteRepository;
import br.com.amorEmMechas_Formulario.api.para.formulario.repository.solicitante.SolicitanteRepository;
import br.com.amorEmMechas_Formulario.api.para.formulario.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "app.dev-token.enabled=true")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PacienteSolicitanteListingIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PacienteRepository pacienteRepository;

    @Autowired
    private SolicitanteRepository solicitanteRepository;

    @Autowired
    private AvaliacaoRepository avaliacaoRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private String atendenteToken;

    @BeforeEach
    void setUp() {
        avaliacaoRepository.deleteAll();
        pacienteRepository.deleteAll();
        solicitanteRepository.deleteAll();
        atendenteToken = jwtTokenProvider.generateDevToken("demo.atendente", "ROLE_ATENDENTE");
    }

    @Test
    void listaSolicitantesEDevolvePaginaVaziaQuandoNaoHaPacientes() throws Exception {
        Solicitante solicitante = new Solicitante();
        solicitante.setNomeCompleto("Solicitante de teste");
        solicitante.setRg("RG-TESTE-001");
        solicitanteRepository.save(solicitante);

        mockMvc.perform(get("/solicitantes")
                        .header("Authorization", "Bearer " + atendenteToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].nomeCompleto").value("Solicitante de teste"));

        mockMvc.perform(get("/pacientes?page=0&size=20")
                        .header("Authorization", "Bearer " + atendenteToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)))
                .andExpect(jsonPath("$.totalElements").value(0))
                .andExpect(jsonPath("$.totalPages").value(0));
    }

    @Test
    void paginaPacientesIncluiPacienteVinculadoAoSolicitante() throws Exception {
        Solicitante solicitante = new Solicitante();
        solicitante.setNomeCompleto("Solicitante de teste");
        solicitante.setRg("RG-TESTE-002");
        solicitante = solicitanteRepository.save(solicitante);

        Paciente paciente = new Paciente();
        paciente.setNomeCompleto("Paciente de teste");
        paciente.setEmail("paciente.teste@example.invalid");
        paciente.setDtPedido(LocalDate.of(2026, 1, 15));
        paciente.setCel("11999990000");
        paciente.setDtNasc(LocalDate.of(1990, 5, 20));
        paciente.setQtdFilhos(0);
        paciente.setQtdPessoasEmCasa(1);
        paciente.setConsentimentoLgpd(true);
        paciente.setDadosAnonimizados(false);
        paciente.setSolicitante(solicitante);
        pacienteRepository.save(paciente);

        mockMvc.perform(get("/pacientes?page=0&size=20")
                        .header("Authorization", "Bearer " + atendenteToken)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].nomeCompleto").value("Paciente de teste"))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.totalPages").value(1));
    }

    @Test
    void listagemDeAvaliacoesLeIdComNomeFisicoDoBanco() throws Exception {
        Solicitante solicitante = new Solicitante();
        solicitante.setNomeCompleto("TESTE - Solicitante");
        solicitante.setRg("TESTE-AVALIACAO-001");
        Solicitante savedSolicitante = solicitanteRepository.save(solicitante);

        Avaliacao avaliacao = new Avaliacao();
        avaliacao.setSolicitante(savedSolicitante);
        avaliacao.setConsentimento(false);
        avaliacao.setConcluido(false);
        avaliacaoRepository.save(avaliacao);

        mockMvc.perform(get("/avaliacoes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].concluido").value(false));
    }
}