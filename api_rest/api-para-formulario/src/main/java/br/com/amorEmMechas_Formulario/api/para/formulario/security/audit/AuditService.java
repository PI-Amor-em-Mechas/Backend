package br.com.amorEmMechas_Formulario.api.para.formulario.security.audit;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

/**
 * ServiÃ§o de auditoria HL7.
 * Registra todas as operaÃ§Ãµes relevantes sobre dados mÃ©dicos protegidos (PHI).
 * Conforme HL7 Security Framework: toda aÃ§Ã£o sobre dados de saÃºde deve ser rastreÃ¡vel.
 */
@Service
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    public AuditService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Async
    public void registrarEvento(String usuario, AuditLog.TipoEvento tipoEvento,
                                String recurso, boolean sucesso, HttpServletRequest request) {
        AuditLog log = new AuditLog(usuario, tipoEvento, recurso, sucesso);

        if (request != null) {
            log.setMetodoHttp(request.getMethod());
            log.setEnderecoIp(obterIpReal(request));
            log.setUserAgent(truncate(request.getHeader("User-Agent"), 500));
        }

        auditLogRepository.save(log);
    }

    @Async
    public void registrarEvento(String usuario, AuditLog.TipoEvento tipoEvento,
                                String recurso, boolean sucesso,
                                String detalhes, HttpServletRequest request) {
        AuditLog log = new AuditLog(usuario, tipoEvento, recurso, sucesso);
        log.setDetalhes(truncate(detalhes, 1000));

        if (request != null) {
            log.setMetodoHttp(request.getMethod());
            log.setEnderecoIp(obterIpReal(request));
            log.setUserAgent(truncate(request.getHeader("User-Agent"), 500));
        }

        auditLogRepository.save(log);
    }

    public void registrarAcesso(AuditLog.TipoEvento tipoEvento, String recurso, HttpServletRequest request) {
        String usuario = obterUsuarioAtual();
        registrarEvento(usuario, tipoEvento, recurso, true, request);
    }

    public void registrarFalha(AuditLog.TipoEvento tipoEvento, String recurso,
                               String detalhes, HttpServletRequest request) {
        String usuario = obterUsuarioAtual();
        registrarEvento(usuario, tipoEvento, recurso, false, detalhes, request);
    }

    private String obterUsuarioAtual() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated()) {
            return auth.getName();
        }
        return "anonimo";
    }

    private String obterIpReal(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private String truncate(String value, int maxLength) {
        if (value == null) return null;
        return value.length() > maxLength ? value.substring(0, maxLength) : value;
    }
}
