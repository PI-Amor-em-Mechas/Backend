package br.com.amorEmMechas_Formulario.api.para.formulario.controller;

import br.com.amorEmMechas_Formulario.api.para.formulario.repository.arquivo.ArquivoRepository;
import br.com.amorEmMechas_Formulario.api.para.formulario.security.JwtTokenProvider;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "app.dev-token.enabled=true")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ArquivoReviewIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ArquivoRepository arquivoRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private String attendantToken;
    private String medicalToken;

    @BeforeEach
    void setUp() {
        arquivoRepository.deleteAll();
        attendantToken = jwtTokenProvider.generateDevToken("demo.atendente", "ROLE_ATENDENTE");
        medicalToken = jwtTokenProvider.generateDevToken("demo.medico", "ROLE_MEDICO");
    }

    @Test
    void uploadRealSignatureStartsPendingAndReviewerCanApprove() throws Exception {
        int fileId = uploadPdf();

        mockMvc.perform(get("/arquivos/pendentes-validacao")
                        .header("Authorization", "Bearer " + attendantToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/arquivos/pendentes-validacao")
                        .header("Authorization", "Bearer " + medicalToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].mimeType").value("application/pdf"))
                .andExpect(jsonPath("$[0].statusValidacao").value("PENDENTE"));

        mockMvc.perform(get("/arquivos/{id}/download", fileId)
                        .header("Authorization", "Bearer " + attendantToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(patch("/arquivos/{id}/validacao", fileId)
                        .header("Authorization", "Bearer " + medicalToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"decisao\":\"APROVADO\",\"observacao\":\"Conferido pela equipe\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusValidacao").value("APROVADO"))
                .andExpect(jsonPath("$.validadoPor").value("demo.medico"))
                .andExpect(jsonPath("$.validadoEm").exists());

        mockMvc.perform(get("/arquivos/pendentes-validacao")
                        .header("Authorization", "Bearer " + medicalToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void fakePdfAndWrongExtensionAreRejected() throws Exception {
        MockMultipartFile fakePdf = new MockMultipartFile(
                "arquivo", "fake.pdf", "application/pdf", "not a pdf".getBytes());
        mockMvc.perform(multipart("/arquivos").file(fakePdf).param("tipo", "LAUDO_MEDICO"))
                .andExpect(status().isUnsupportedMediaType());

        MockMultipartFile wrongExtension = new MockMultipartFile(
                "arquivo", "document.pdf", "application/pdf", new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 1});
        mockMvc.perform(multipart("/arquivos").file(wrongExtension).param("tipo", "LAUDO_MEDICO"))
                .andExpect(status().isUnsupportedMediaType());

        org.junit.jupiter.api.Assertions.assertEquals(0, arquivoRepository.count());
    }

    @Test
    void rejectionRequiresReasonAndIsAuditable() throws Exception {
        int fileId = uploadPdf();

        mockMvc.perform(patch("/arquivos/{id}/validacao", fileId)
                        .header("Authorization", "Bearer " + medicalToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"decisao\":\"REJEITADO\"}"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(patch("/arquivos/{id}/validacao", fileId)
                        .header("Authorization", "Bearer " + medicalToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"decisao\":\"REJEITADO\",\"observacao\":\"Documento sem identificacao do emissor\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusValidacao").value("REJEITADO"))
                .andExpect(jsonPath("$.validadoPor").value("demo.medico"))
                .andExpect(jsonPath("$.observacaoValidacao").value("Documento sem identificacao do emissor"));
    }

        private int uploadPdf() throws Exception {
        MockMultipartFile pdf = new MockMultipartFile(
                                "arquivo", "laudo-teste.pdf", "application/octet-stream", createPdf());
        MvcResult result = mockMvc.perform(multipart("/arquivos")
                        .file(pdf)
                        .param("tipo", "LAUDO_MEDICO"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mimeType").value("application/pdf"))
                .andExpect(jsonPath("$.statusValidacao").value("PENDENTE"))
                .andReturn();
        return new com.fasterxml.jackson.databind.ObjectMapper()
                .readTree(result.getResponse().getContentAsString())
                .get("id").asInt();
    }

        private byte[] createPdf() throws IOException {
                try (PDDocument document = new PDDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
                        document.addPage(new PDPage());
                        document.save(output);
                        return output.toByteArray();
                }
        }
}