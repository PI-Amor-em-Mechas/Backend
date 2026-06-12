package br.com.amorEmMechas_Formulario.api.para.formulario.controller;

import br.com.amorEmMechas_Formulario.api.para.formulario.entity.usuario.Usuario;
import br.com.amorEmMechas_Formulario.api.para.formulario.repository.usuario.UsuarioRepository;
import br.com.amorEmMechas_Formulario.api.para.formulario.security.JwtTokenProvider;
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

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ValidationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private String adminToken;

    @BeforeEach
    void setUp() {
        usuarioRepository.deleteAll();

        Usuario admin = new Usuario();
        admin.setUsername("admin");
        admin.setPassword(passwordEncoder.encode("admin123"));
        admin.setRole("ROLE_ADMIN");
        usuarioRepository.save(admin);

        adminToken = jwtTokenProvider.generateTokenFromUsername("admin");
    }

    @Test
    @DisplayName("Paciente com nome vazio retorna 400 com campo invalido")
    void pacienteNomeVazioRetorna400() throws Exception {
        String json = """
                {
                    "nomeCompleto": "",
                    "email": "teste@email.com",
                    "cel": "11999999999",
                    "dtNasc": "1990-01-01",
                    "estadoCivil": "solteiro",
                    "temFilhos": false,
                    "qtdPessoasEmCasa": 2,
                    "qtdFilhos": 0,
                    "cpf": "12345678900",
                    "enderecoId": 1,
                    "solicitanteId": 1
                }
                """;

        mockMvc.perform(post("/pacientes")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.nomeCompleto").exists());
    }

    @Test
    @DisplayName("Paciente sem email retorna 400")
    void pacienteSemEmailRetorna400() throws Exception {
        String json = """
                {
                    "nomeCompleto": "Ana Silva",
                    "email": "",
                    "cel": "11999999999",
                    "dtNasc": "1990-01-01",
                    "estadoCivil": "solteiro",
                    "temFilhos": false,
                    "qtdPessoasEmCasa": 2,
                    "qtdFilhos": 0,
                    "cpf": "12345678900",
                    "enderecoId": 1,
                    "solicitanteId": 1
                }
                """;

        mockMvc.perform(post("/pacientes")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.email").exists());
    }

    @Test
    @DisplayName("Paciente com email invalido retorna 400")
    void pacienteEmailInvalidoRetorna400() throws Exception {
        String json = """
                {
                    "nomeCompleto": "Ana Silva",
                    "email": "email-invalido",
                    "cel": "11999999999",
                    "dtNasc": "1990-01-01",
                    "estadoCivil": "solteiro",
                    "temFilhos": false,
                    "qtdPessoasEmCasa": 2,
                    "qtdFilhos": 0,
                    "cpf": "12345678900",
                    "enderecoId": 1,
                    "solicitanteId": 1
                }
                """;

        mockMvc.perform(post("/pacientes")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.email").exists());
    }

    @Test
    @DisplayName("Paciente sem data de nascimento retorna 400")
    void pacienteSemDtNascRetorna400() throws Exception {
        String json = """
                {
                    "nomeCompleto": "Ana Silva",
                    "email": "ana@email.com",
                    "cel": "11999999999",
                    "estadoCivil": "solteiro",
                    "temFilhos": false,
                    "qtdPessoasEmCasa": 2,
                    "qtdFilhos": 0,
                    "cpf": "12345678900",
                    "enderecoId": 1,
                    "solicitanteId": 1
                }
                """;

        mockMvc.perform(post("/pacientes")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.dtNasc").exists());
    }

    @Test
    @DisplayName("Paciente sem CPF retorna 400")
    void pacienteSemCpfRetorna400() throws Exception {
        String json = """
                {
                    "nomeCompleto": "Ana Silva",
                    "email": "ana@email.com",
                    "cel": "11999999999",
                    "dtNasc": "1990-01-01",
                    "estadoCivil": "solteiro",
                    "temFilhos": false,
                    "qtdPessoasEmCasa": 2,
                    "qtdFilhos": 0,
                    "cpf": "",
                    "enderecoId": 1,
                    "solicitanteId": 1
                }
                """;

        mockMvc.perform(post("/pacientes")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.cpf").exists());
    }

    @Test
    @DisplayName("Login sem username retorna 401 (credenciais invalidas)")
    void loginSemUsernameRetorna401() throws Exception {
        String json = """
                {
                    "username": "",
                    "password": "senha123"
                }
                """;

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isUnauthorized());
    }
}
