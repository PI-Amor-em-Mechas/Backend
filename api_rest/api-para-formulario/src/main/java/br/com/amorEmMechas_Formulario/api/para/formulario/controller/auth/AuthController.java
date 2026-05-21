package br.com.amorEmMechas_Formulario.api.para.formulario.controller.auth;

import br.com.amorEmMechas_Formulario.api.para.formulario.dto.auth.LoginRequestDTO;
import br.com.amorEmMechas_Formulario.api.para.formulario.dto.auth.RegistroRequestDTO;
import br.com.amorEmMechas_Formulario.api.para.formulario.entity.usuario.Usuario;
import br.com.amorEmMechas_Formulario.api.para.formulario.repository.usuario.UsuarioRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    /**
     * Login: autentica o usuário e cria uma sessão com cookie HttpOnly.
     * Body JSON: { "username": "admin", "password": "senha123" }
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequestDTO loginRequest,
                                   HttpServletRequest request,
                                   HttpServletResponse response) {
        try {
            Authentication auth = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.getUsername(),
                            loginRequest.getPassword()
                    )
            );

            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(auth);
            SecurityContextHolder.setContext(context);

            HttpSessionSecurityContextRepository repo = new HttpSessionSecurityContextRepository();
            repo.saveContext(context, request, response);

            request.getSession(true);

            return ResponseEntity.ok(Map.of(
                    "mensagem", "Login realizado com sucesso",
                    "usuario", auth.getName()
            ));

        } catch (BadCredentialsException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("mensagem", "Usuário ou senha inválidos"));
        }
    }

    /**
     * Registro: cria um novo usuário.
     * Body JSON: { "username": "admin", "password": "senha123", "role": "ROLE_ADMIN" }
     */
    @PostMapping("/registro")
    public ResponseEntity<?> registro(@RequestBody RegistroRequestDTO registroRequest) {
        if (usuarioRepository.existsByUsername(registroRequest.getUsername())) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("mensagem", "Username já está em uso"));
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

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("mensagem", "Usuário criado com sucesso", "usuario", novoUsuario.getUsername()));
    }
}

