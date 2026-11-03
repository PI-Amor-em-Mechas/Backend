package br.com.amorEmMechas_Formulario.api.para.formulario.controller.auth;

import br.com.amorEmMechas_Formulario.api.para.formulario.dto.auth.LoginRequestDTO;
import br.com.amorEmMechas_Formulario.api.para.formulario.dto.auth.RegistroRequestDTO;
import br.com.amorEmMechas_Formulario.api.para.formulario.entity.usuario.Usuario;
import br.com.amorEmMechas_Formulario.api.para.formulario.repository.usuario.UsuarioRepository;
import br.com.amorEmMechas_Formulario.api.para.formulario.security.JwtTokenProvider;
import br.com.amorEmMechas_Formulario.api.para.formulario.security.audit.AuditLog;
import br.com.amorEmMechas_Formulario.api.para.formulario.security.audit.AuditService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Set;

/**
 * Controller de autenticacao conforme HL7 Security.
 * Implementa autenticacao via JWT com auditoria completa de login/logout.
 */
@RestController
@RequestMapping("/auth")
public class AuthController {

    private static final Set<String> ROLES_ELEVADAS = Set.of(
            "ROLE_ADMIN", "ROLE_MEDICO", "ROLE_ENFERMEIRO"
    );

    private final AuthenticationManager authenticationManager;
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuditService auditService;

        @Value("${app.dev-token.enabled:false}")
        private boolean devTokenEnabled;

    public AuthController(AuthenticationManager authenticationManager,
                          UsuarioRepository usuarioRepository,
                          PasswordEncoder passwordEncoder,
                          JwtTokenProvider jwtTokenProvider,
                          AuditService auditService) {
        this.authenticationManager = authenticationManager;
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.auditService = auditService;
    }

    /**
     * Login: autentica o usuário e retorna tokens JWT (access + refresh).
     * Body JSON: { "username": "admin", "password": "senha123" }
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequestDTO loginRequest,
                                   HttpServletRequest request) {
        try {
            Authentication auth = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.getUsername(),
                            loginRequest.getPassword()
                    )
            );

            String accessToken = jwtTokenProvider.generateToken(auth);
            String refreshToken = jwtTokenProvider.generateRefreshToken(auth.getName());

            // Registrar evento de login bem-sucedido na auditoria HL7
            auditService.registrarEvento(
                    auth.getName(),
                    AuditLog.TipoEvento.LOGIN,
                    "/auth/login",
                    true,
                    request
            );

            return ResponseEntity.ok(Map.of(
                    "mensagem", "Login realizado com sucesso",
                    "usuario", auth.getName(),
                    "accessToken", accessToken,
                    "refreshToken", refreshToken,
                    "tokenType", "Bearer"
            ));

        } catch (BadCredentialsException e) {
            // Registrar tentativa de login falha na auditoria HL7
            auditService.registrarEvento(
                    loginRequest.getUsername(),
                    AuditLog.TipoEvento.LOGIN_FALHA,
                    "/auth/login",
                    false,
                    "Credenciais inválidas",
                    request
            );

            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("mensagem", "Usuário ou senha inválidos"));
        }
    }

        @PostMapping("/dev-token")
        public ResponseEntity<?> devToken(@RequestBody Map<String, String> requestBody) {
                if (!devTokenEnabled) {
                        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                                        .body(Map.of("mensagem", "Gerador de token de desenvolvimento desabilitado"));
                }

                String username = requestBody.getOrDefault("username", "dev.user").trim();
                String role = requestBody.getOrDefault("role", "ROLE_ATENDENTE").trim();
                if (username.isBlank() || !role.matches("ROLE_(ADMIN|MEDICO|ENFERMEIRO|ATENDENTE|USER)")) {
                        return ResponseEntity.badRequest()
                                        .body(Map.of("mensagem", "Username invalido ou role nao permitida"));
                }

                String accessToken = jwtTokenProvider.generateDevToken(username, role);
                String refreshToken = jwtTokenProvider.generateRefreshToken(username);
                return ResponseEntity.ok(Map.of(
                                "mensagem", "Token de desenvolvimento gerado sem consultar o banco",
                                "usuario", username,
                                "role", role,
                                "accessToken", accessToken,
                                "refreshToken", refreshToken,
                                "tokenType", "Bearer"
                ));
        }

    /**
     * Refresh Token: gera novo access token a partir de um refresh token válido.
     */
    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(@RequestBody Map<String, String> requestBody) {
        String refreshToken = requestBody.get("refreshToken");

        if (refreshToken == null || !jwtTokenProvider.validateToken(refreshToken)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("mensagem", "Refresh token inválido ou expirado"));
        }

        String tokenType = jwtTokenProvider.getTokenType(refreshToken);
        if (!"refresh".equals(tokenType)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("mensagem", "Token fornecido não é um refresh token"));
        }

        String username = jwtTokenProvider.getUsernameFromToken(refreshToken);
        String newAccessToken = jwtTokenProvider.generateTokenFromUsername(username);

        return ResponseEntity.ok(Map.of(
                "accessToken", newAccessToken,
                "tokenType", "Bearer"
        ));
    }

    /**
     * Registro: cria um novo usuario com role basica (ROLE_USER ou ROLE_ATENDENTE).
     * Para roles elevadas (ADMIN, MEDICO, ENFERMEIRO), use /auth/registro-admin (requer ROLE_ADMIN).
     */
    @PostMapping("/registro")
    public ResponseEntity<?> registro(@Valid @RequestBody RegistroRequestDTO registroRequest,
                                      HttpServletRequest request) {
        if (usuarioRepository.existsByUsername(registroRequest.getUsername())) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("mensagem", "Username ja esta em uso"));
        }

        String role = registroRequest.getRole();
        if (role == null || role.isBlank()) {
            role = "ROLE_USER";
        }

        // Bloquear criacao de roles elevadas sem autorizacao
        if (ROLES_ELEVADAS.contains(role)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("mensagem", "Registro de roles elevadas requer autorizacao de administrador. Use /auth/registro-admin"));
        }

        Usuario novoUsuario = new Usuario();
        novoUsuario.setUsername(registroRequest.getUsername());
        novoUsuario.setPassword(passwordEncoder.encode(registroRequest.getPassword()));
        novoUsuario.setRole(role);

        usuarioRepository.save(novoUsuario);

        auditService.registrarEvento(
                registroRequest.getUsername(),
                AuditLog.TipoEvento.LOGIN,
                "/auth/registro",
                true,
                "Novo usuario registrado com role: " + role,
                request
        );

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("mensagem", "Usuario criado com sucesso", "usuario", novoUsuario.getUsername()));
    }

    /**
     * Registro administrativo: permite criar usuarios com qualquer role.
     * Requer autenticacao com ROLE_ADMIN.
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/registro-admin")
    public ResponseEntity<?> registroAdmin(@Valid @RequestBody RegistroRequestDTO registroRequest,
                                           HttpServletRequest request) {
        if (usuarioRepository.existsByUsername(registroRequest.getUsername())) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("mensagem", "Username ja esta em uso"));
        }

        String role = registroRequest.getRole();
        if (role == null || role.isBlank()) {
            role = "ROLE_USER";
        }

        Usuario novoUsuario = new Usuario();
        novoUsuario.setUsername(registroRequest.getUsername());
        novoUsuario.setPassword(passwordEncoder.encode(registroRequest.getPassword()));
        novoUsuario.setRole(role);

        usuarioRepository.save(novoUsuario);

        auditService.registrarEvento(
                registroRequest.getUsername(),
                AuditLog.TipoEvento.LOGIN,
                "/auth/registro-admin",
                true,
                "Usuario registrado por admin com role: " + role,
                request
        );

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("mensagem", "Usuario criado com sucesso", "usuario", novoUsuario.getUsername(), "role", role));
    }
}
