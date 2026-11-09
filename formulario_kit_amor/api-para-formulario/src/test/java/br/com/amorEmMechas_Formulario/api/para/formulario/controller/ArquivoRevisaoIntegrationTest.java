package br.com.amorEmMechas_Formulario.api.para.formulario.controller;

import br.com.amorEmMechas_Formulario.api.para.formulario.entity.arquivo.Arquivo;
import br.com.amorEmMechas_Formulario.api.para.formulario.entity.arquivo.StatusValidacao;
import br.com.amorEmMechas_Formulario.api.para.formulario.entity.paciente.Paciente;
import br.com.amorEmMechas_Formulario.api.para.formulario.repository.arquivo.ArquivoRepository;
import br.com.amorEmMechas_Formulario.api.para.formulario.repository.paciente.PacienteRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = "hl7.security.encryption.key=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=")
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@WithMockUser(username = "revisor", roles = "ADMIN")
class ArquivoRevisaoIntegrationTest {
    @TempDir static Path uploads;
    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("app.upload.dir", () -> uploads.toString());
    }

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired ArquivoRepository arquivos;
    @Autowired PacienteRepository pacientes;

    private Paciente paciente() {
        Paciente paciente = new Paciente();
        paciente.setNomeCompleto("Paciente sintetica");
        return pacientes.saveAndFlush(paciente);
    }

    private MockMultipartFile imagem() throws Exception {
        var buffer = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB), "png", buffer);
        return new MockMultipartFile("arquivo", "laudo.png", "image/png", buffer.toByteArray());
    }

    private long enviar(Integer pacienteId) throws Exception {
        var request = multipart("/arquivos").file(imagem()).param("tipo", "RELATORIO_MEDICO");
        if (pacienteId != null) request.param("pacienteId", pacienteId.toString());
        var response = mvc.perform(request).andExpect(status().isOk())
                .andExpect(jsonPath("$.statusValidacao").value("PENDENTE"))
                .andExpect(jsonPath("$.dataUpload").isNotEmpty()).andReturn();
        return json.readTree(response.getResponse().getContentAsString()).get("id").asLong();
    }

    private Arquivo registro(StatusValidacao status, Paciente paciente) {
        Arquivo arquivo = new Arquivo();
        arquivo.setTipo("RELATORIO_MEDICO");
        arquivo.setStatusValidacao(status);
        arquivo.setPaciente(paciente);
        return arquivos.saveAndFlush(arquivo);
    }

    @Test
    void uploadRevisaoDownloadEPersistenciaSemComparacaoAutomatica() throws Exception {
        Paciente paciente = paciente();
        long id = enviar(paciente.getId());
        mvc.perform(get("/arquivos/pendentes-validacao")).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(id));
        mvc.perform(get("/arquivos/{id}", id)).andExpect(status().isOk())
                .andExpect(jsonPath("$.statusComparacao").doesNotExist());
        mvc.perform(get("/arquivos/{id}/download", id)).andExpect(status().isOk())
                .andExpect(content().contentType("image/png"));
        mvc.perform(patch("/arquivos/{id}/validacao", id).contentType(MediaType.APPLICATION_JSON)
                .content("{\"decisao\":\"APROVADO\",\"observacao\":\"Conferido manualmente\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.statusValidacao").value("APROVADO"))
                .andExpect(jsonPath("$.validadoEm").isNotEmpty());
        Arquivo salvo = arquivos.findById(id).orElseThrow();
        assertThat(salvo.getPaciente().getId()).isEqualTo(paciente.getId());
        assertThat(salvo.getValidadoPor()).isEqualTo("revisor");
        mvc.perform(get("/arquivos/pendentes-validacao")).andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
        mvc.perform(get("/arquivos/{id}/download", id).with(user("atendente").roles("ATENDENTE")))
                .andExpect(status().isOk());
    }

    @Test
    void rejeicaoExigeObservacaoEPreservaDecisaoContraSegundaRevisao() throws Exception {
        long id = enviar(null);
        mvc.perform(patch("/arquivos/{id}/validacao", id).contentType(MediaType.APPLICATION_JSON)
                .content("{\"decisao\":\"REJEITADO\",\"observacao\":\"  \"}")).andExpect(status().isBadRequest());
        assertThat(arquivos.findById(id).orElseThrow().getStatusValidacao()).isEqualTo(StatusValidacao.PENDENTE);
        mvc.perform(patch("/arquivos/{id}/validacao", id).contentType(MediaType.APPLICATION_JSON)
                .content("{\"decisao\":\"REJEITADO\",\"observacao\":\"Laudo ilegivel\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.observacaoValidacao").value("Laudo ilegivel"));
        mvc.perform(patch("/arquivos/{id}/validacao", id).contentType(MediaType.APPLICATION_JSON)
                .content("{\"decisao\":\"APROVADO\"}")).andExpect(status().isConflict());
    }

    @Test
    void revisoresMedicoEEnfermeiroPodemDecidir() throws Exception {
        for (String role : new String[]{"MEDICO", "ENFERMEIRO"}) {
            long id = enviar(null);
            mvc.perform(patch("/arquivos/{id}/validacao", id).with(user("equipe").roles(role))
                    .contentType(MediaType.APPLICATION_JSON).content("{\"decisao\":\"APROVADO\"}"))
                    .andExpect(status().isOk());
        }
    }

    @Test
    @WithMockUser(roles = "ATENDENTE")
    void atendenteNaoRevisaNemAssociaOuBaixaPendentes() throws Exception {
        long id = registro(StatusValidacao.PENDENTE, null).getId();
        mvc.perform(get("/arquivos/pendentes-validacao")).andExpect(status().isForbidden());
        mvc.perform(patch("/arquivos/{id}/validacao", id).contentType(MediaType.APPLICATION_JSON)
                .content("{\"decisao\":\"APROVADO\"}")).andExpect(status().isForbidden());
        mvc.perform(get("/arquivos/{id}/download", id)).andExpect(status().isForbidden());
        mvc.perform(multipart("/arquivos").file(imagem()).param("tipo", "RELATORIO_MEDICO")
                .param("pacienteId", paciente().getId().toString())).andExpect(status().isForbidden());
    }

    @Test
    @WithAnonymousUser
    void uploadAvulsoPublicoMasRevisaoEConsultaExigemSessao() throws Exception {
        long id = enviar(null);
        mvc.perform(get("/arquivos/{id}", id)).andExpect(status().isUnauthorized());
        mvc.perform(get("/arquivos/pendentes-validacao")).andExpect(status().isUnauthorized());
    }

    @Test
    void rejeitaArquivoInvalidoEAcimaDoLimiteSemPersistir() throws Exception {
        long total = arquivos.count();
        for (var file : new MockMultipartFile[]{
                new MockMultipartFile("arquivo", "laudo.pdf", "application/pdf", "<html>fake</html>".getBytes()),
                new MockMultipartFile("arquivo", "laudo.pdf", "application/pdf", "%PDF-1.7".getBytes()),
                new MockMultipartFile("arquivo", "foto.png", "image/png", new byte[0]),
                new MockMultipartFile("arquivo", "foto.png", "image/png", new byte[]{(byte) 137, 80, 78, 71, 13, 10, 26, 10}),
                new MockMultipartFile("arquivo", "foto.jpg", "image/jpeg", imagem().getBytes()),
                new MockMultipartFile("arquivo", "script.html", "text/html", "<html/>".getBytes())}) {
            mvc.perform(multipart("/arquivos").file(file).param("tipo", "RELATORIO_MEDICO"))
                    .andExpect(status().isUnsupportedMediaType());
        }
        mvc.perform(multipart("/arquivos").file(new MockMultipartFile("arquivo", "grande.pdf",
                        "application/pdf", new byte[10 * 1024 * 1024 + 1])).param("tipo", "RELATORIO_MEDICO"))
                .andExpect(status().isPayloadTooLarge());
        assertThat(arquivos.count()).isEqualTo(total);
    }

    @Test
    void aceitaExatamenteDezMiBERegistraTamanhoReal() throws Exception {
        byte[] conteudo = java.util.Arrays.copyOf(imagem().getBytes(), 10 * 1024 * 1024);
        mvc.perform(multipart("/arquivos").file(new MockMultipartFile("arquivo",
                        "limite.png", "image/png", conteudo)).param("tipo", "RELATORIO_MEDICO"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.tamanho").value(10 * 1024 * 1024))
                .andExpect(jsonPath("$.statusValidacao").value("PENDENTE"));
    }

    @Test
    void legadoSemStatusEntraNaFilaMasFotoDeCabeloNao() throws Exception {
        Arquivo legado = registro(null, null);
        Arquivo cabelo = registro(StatusValidacao.PENDENTE, null);
        cabelo.setTipo("FOTO_CABELO");
        arquivos.saveAndFlush(cabelo);
        mvc.perform(get("/arquivos/pendentes-validacao")).andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(legado.getId()))
                .andExpect(jsonPath("$[0].statusValidacao").value("PENDENTE"));
    }

    @Test
    void dadosMedicosBloqueiamLaudoPendenteRejeitadoOuDeOutraPaciente() throws Exception {
        Paciente paciente = paciente();
        Arquivo arquivo = registro(StatusValidacao.PENDENTE, paciente);
        String body = """
                {"motivo":"Tratamento","tipoCancer":"Nao se aplica","justificativa":"Teste",
                "dtInicioTratamento":"2026-01-01","tipoAtendimento":"SUS","pacienteId":%d,"relatorioMedicoId":%d}
                """.formatted(paciente.getId(), arquivo.getId());
        for (StatusValidacao estado : new StatusValidacao[]{StatusValidacao.PENDENTE, StatusValidacao.REJEITADO}) {
            arquivo.setStatusValidacao(estado);
            arquivos.saveAndFlush(arquivo);
            mvc.perform(post("/dados-medicos").contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isConflict());
        }
        arquivo.setStatusValidacao(StatusValidacao.APROVADO);
        arquivo.setPaciente(paciente());
        arquivos.saveAndFlush(arquivo);
        mvc.perform(post("/dados-medicos").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
        arquivo.setPaciente(paciente);
        arquivos.saveAndFlush(arquivo);
        mvc.perform(post("/dados-medicos").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());
    }

    @Test
    void decisaoInvalidaAusenteOuArquivoInexistenteNaoAlteraFila() throws Exception {
        for (String body : new String[]{"{}", "{\"decisao\":\"PENDENTE\"}",
                "{\"decisao\":\"REJEITADO\",\"observacao\":\"" + "x".repeat(2001) + "\"}"}) {
            mvc.perform(patch("/arquivos/999999/validacao").contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest());
        }
        mvc.perform(patch("/arquivos/999999/validacao").contentType(MediaType.APPLICATION_JSON)
                .content("{\"decisao\":\"APROVADO\"}")).andExpect(status().isNotFound());
    }
}
