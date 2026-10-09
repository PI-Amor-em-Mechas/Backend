package br.com.amorEmMechas_Formulario.api.para.formulario.controller;

import br.com.amorEmMechas_Formulario.api.para.formulario.entity.madrinha.Madrinha;
import br.com.amorEmMechas_Formulario.api.para.formulario.repository.madrinha.MadrinhaRepository;
import br.com.amorEmMechas_Formulario.api.para.formulario.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "app.dev-token.enabled=true")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MadrinhaStatusIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MadrinhaRepository repository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private String token;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
        token = jwtTokenProvider.generateDevToken("demo.atendente", "ROLE_ATENDENTE");
    }

    @Test
    void createUpdateAndListAcceptOnlySupportedStatuses() throws Exception {
        String createBody = """
                {"nomeCompleto":"TESTE - Madrinha","email":"madrinha.status@example.invalid",
                 "horasVoluntarias":0,"funcao":"TESTE","status":"Ativa"}
                """;
        String response = mockMvc.perform(post("/madrinhas")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("Ativa"))
                .andReturn().getResponse().getContentAsString();
        int id = new com.fasterxml.jackson.databind.ObjectMapper()
                .readTree(response).get("id").asInt();

        String updateBody = """
                {"nomeCompleto":"TESTE - Madrinha","email":"madrinha.status@example.invalid",
                 "horasVoluntarias":0,"funcao":"TESTE","status":"Afastada"}
                """;
        mockMvc.perform(put("/madrinhas/{id}", id)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("Afastada"));

        mockMvc.perform(get("/madrinhas")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].status").value("Afastada"));
    }

    @Test
    void invalidCreateAndUpdateAreRejectedAndLegacyRowsAreNotListed() throws Exception {
        String invalidCreate = """
                {"nomeCompleto":"TESTE - Inválida","email":"invalida@example.invalid",
                 "horasVoluntarias":0,"funcao":"TESTE","status":"TESTE - NAO USAR"}
                """;
        mockMvc.perform(post("/madrinhas")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidCreate))
                .andExpect(status().isBadRequest());

        Madrinha valid = new Madrinha();
        valid.setNomeCompleto("TESTE - Valida");
        valid.setStatus("Ativa");
        valid = repository.save(valid);

        Madrinha legacy = new Madrinha();
        legacy.setNomeCompleto("TESTE - Legada");
        legacy.setStatus("TESTE - NAO USAR");
        legacy = repository.save(legacy);

        mockMvc.perform(put("/madrinhas/{id}", valid.getId())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nomeCompleto\":\"TESTE - Valida\",\"email\":\"valida@example.invalid\",\"horasVoluntarias\":0,\"funcao\":\"TESTE\",\"status\":\"ativa\"}"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/madrinhas")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].status").value("Ativa"));

        mockMvc.perform(get("/madrinhas/{id}", legacy.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isConflict());
    }
}