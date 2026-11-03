package br.com.amorEmMechas_Formulario.api.para.formulario.security.audit;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;

/**
 * Filtro de auditoria HL7 que intercepta acessos a recursos de dados mÃ©dicos.
 * Registra automaticamente acessos a endpoints sensÃ­veis (pacientes, laudos, dados mÃ©dicos).
 */
@Component
@Order(2)
public class AuditFilter extends OncePerRequestFilter {

    private final AuditService auditService;

    public AuditFilter(AuditService auditService) {
        this.auditService = auditService;
    }

    private static final Set<String> PATHS_SENSIVEIS = Set.of(
            "/pacientes", "/dadosMedicos", "/avaliacoes", "/arquivos", "/filhos"
    );

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        filterChain.doFilter(request, response);

        // ApÃ³s a resposta, registrar auditoria para paths sensÃ­veis
        String path = request.getRequestURI();
        if (isPathSensivel(path) && isUsuarioAutenticado()) {
            AuditLog.TipoEvento tipo = determinarTipoEvento(request.getMethod(), path);
            auditService.registrarAcesso(tipo, path, request);
        }
    }

    private boolean isPathSensivel(String path) {
        return PATHS_SENSIVEIS.stream().anyMatch(path::contains);
    }

    private boolean isUsuarioAutenticado() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName());
    }

    private AuditLog.TipoEvento determinarTipoEvento(String metodo, String path) {
        if (path.contains("/dadosMedicos") || path.contains("/avaliacoes")) {
            return switch (metodo) {
                case "POST" -> AuditLog.TipoEvento.CRIACAO_LAUDO;
                case "PUT", "PATCH" -> AuditLog.TipoEvento.ALTERACAO_LAUDO;
                case "DELETE" -> AuditLog.TipoEvento.EXCLUSAO_LAUDO;
                default -> AuditLog.TipoEvento.ACESSO_DADOS_MEDICOS;
            };
        }

        if (path.contains("/pacientes")) {
            return switch (metodo) {
                case "PUT", "PATCH" -> AuditLog.TipoEvento.ALTERACAO_PACIENTE;
                default -> AuditLog.TipoEvento.ACESSO_PACIENTE;
            };
        }

        if (path.contains("/arquivos")) {
            if ("POST".equals(metodo)) return AuditLog.TipoEvento.UPLOAD_ARQUIVO;
            return AuditLog.TipoEvento.DOWNLOAD_ARQUIVO;
        }

        return AuditLog.TipoEvento.ACESSO_DADOS_MEDICOS;
    }
}
