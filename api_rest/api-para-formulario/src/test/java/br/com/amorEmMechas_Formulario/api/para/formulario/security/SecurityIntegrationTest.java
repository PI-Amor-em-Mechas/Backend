package br.com.amorEmMechas_Formulario.api.para.formulario.security;

import br.com.amorEmMechas_Formulario.api.para.formulario.entity.usuario.Usuario;
import br.com.amorEmMechas_Formulario.api.para.formulario.repository.usuario.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @BeforeEach
    void setUp() {
        usuarioRepository.deleteAll();

        Usuario admin = new Usuario();
        admin.setUsername("admin");
        admin.setPassword(passwordEncoder.encode("admin123"));
        admin.setRole("ROLE_ADMIN");
        usuarioRepository.save(admin);

        Usuario user = new Usuario();
        user.setUsername("usuario");
        user.setPassword(passwordEncoder.encode("user123"));
        user.setRole("ROLE_USER");
        usuarioRepository.save(user);

        Usuario medico = new Usuario();
        medico.setUsername("medico");
        medico.setPassword(passwordEncoder.encode("medico123"));
        medico.setRole("ROLE_MEDICO");
        usuarioRepository.save(medico);
    }

    @Test
    @DisplayName("Login com credenciais validas retorna JWT")
    void loginComCredenciaisValidas() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"admin123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(jsonPath("$.refreshToken").exists())
                .andExpect(jsonPath("$.tokenType").value("Bearer"));
    }

    @Test
    @DisplayName("Login com credenciais invalidas retorna 401")
    void loginComCredenciaisInvalidas() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"senhaErrada\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Acesso sem token a endpoint protegido retorna 401")
    void acessoSemTokenRetorna401() throws Exception {
        mockMvc.perform(get("/pacientes"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Envio do formulario sem token chega a validacao")
    void envioFormularioSemTokenChegaAValidacao() throws Exception {
        mockMvc.perform(post("/formulario-solicitacao-peruca")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Metodo GET do formulario continua protegido")
    void metodoGetDoFormularioContinuaProtegido() throws Exception {
        String token = jwtTokenProvider.generateTokenFromUsername("medico");

        mockMvc.perform(get("/formulario-solicitacao-peruca"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Acesso com token invalido retorna 401")
    void acessoComTokenInvalidoRetorna401() throws Exception {
        mockMvc.perform(get("/pacientes")
                        .header("Authorization", "Bearer token.invalido.aqui"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Acesso com token valido de MEDICO a /dados-medicos retorna 200")
    void medicoAcessaDadosMedicos() throws Exception {
        String token = jwtTokenProvider.generateTokenFromUsername("medico");

        mockMvc.perform(get("/dados-medicos")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Acesso com token de USER a /dados-medicos retorna 403")
    void userNaoAcessaDadosMedicos() throws Exception {
        String token = jwtTokenProvider.generateTokenFromUsername("usuario");

        mockMvc.perform(get("/dados-medicos")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Registro de role elevada sem ser admin retorna 403")
    void registroRoleElevadaSemAdminRetorna403() throws Exception {
        mockMvc.perform(post("/auth/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"novoMedico\",\"password\":\"senha123\",\"role\":\"ROLE_MEDICO\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Registro de role elevada com admin retorna 201")
    void registroRoleElevadaComAdminRetorna201() throws Exception {
        String token = jwtTokenProvider.generateTokenFromUsername("admin");

        mockMvc.perform(post("/auth/registro-admin")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"novoMedico\",\"password\":\"senha123\",\"role\":\"ROLE_MEDICO\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("ROLE_MEDICO"));
    }

    @Test
    @DisplayName("Registro de role basica sem autenticacao retorna 201")
    void registroRoleBasicaSemAuth() throws Exception {
        mockMvc.perform(post("/auth/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"novoUser\",\"password\":\"senha123\",\"role\":\"ROLE_USER\"}"))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("Refresh token gera novo access token")
    void refreshTokenGeraNovoAccessToken() throws Exception {
        String refreshToken = jwtTokenProvider.generateRefreshToken("admin");

        mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + refreshToken + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists());
    }

    @Test
    @DisplayName("Refresh com access token (tipo errado) retorna 401")
    void refreshComAccessTokenRetorna401() throws Exception {
        String accessToken = jwtTokenProvider.generateTokenFromUsername("admin");

        mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + accessToken + "\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Endpoints Swagger sao publicos")
    void swaggerPublico() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk());
    }
}
