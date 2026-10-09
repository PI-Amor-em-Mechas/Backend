package br.com.amorEmMechas_Formulario.api.para.formulario.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "app.dev-token.enabled=true")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DevTokenSecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("Token demo de atendente acessa madrinhas antes e depois do refresh")
    void tokenDemoAtendenteAcessaMadrinhasAposRefresh() throws Exception {
        MvcResult issued = mockMvc.perform(post("/auth/dev-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"demo.atendente\",\"role\":\"ROLE_ATENDENTE\"}"))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode issuedTokens = objectMapper.readTree(issued.getResponse().getContentAsString());

        mockMvc.perform(get("/madrinhas")
                        .header("Authorization", "Bearer " + issuedTokens.get("accessToken").asText()))
                .andExpect(status().isOk());

        MvcResult refreshed = mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.createObjectNode()
                                .put("refreshToken", issuedTokens.get("refreshToken").asText()).toString()))
                .andExpect(status().isOk())
                .andReturn();
        String refreshedAccessToken = objectMapper.readTree(refreshed.getResponse().getContentAsString())
                .get("accessToken").asText();

        mockMvc.perform(get("/madrinhas")
                        .header("Authorization", "Bearer " + refreshedAccessToken))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Madrinhas diferencia falta de autenticacao de role insuficiente")
    void madrinhasRetorna401SemTokenE403ParaRoleInsuficiente() throws Exception {
        mockMvc.perform(get("/madrinhas"))
                .andExpect(status().isUnauthorized());

        MvcResult issued = mockMvc.perform(post("/auth/dev-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"demo.usuario\",\"role\":\"ROLE_USER\"}"))
                .andExpect(status().isOk())
                .andReturn();
        String accessToken = objectMapper.readTree(issued.getResponse().getContentAsString())
                .get("accessToken").asText();

        mockMvc.perform(get("/madrinhas")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isForbidden());
    }
}