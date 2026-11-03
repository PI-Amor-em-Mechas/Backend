package br.com.amorEmMechas_Formulario.api.para.formulario.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Filtro de rate limiting para proteção contra ataques de força bruta.
 * Conforme HL7 Security: endpoints de autenticação devem limitar tentativas
 * para prevenir acesso não autorizado a dados médicos.
 *
 * Limite: 10 requisições por minuto por IP no endpoint /auth/login.
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private static final int MAX_REQUESTS_PER_MINUTE = 10;
    private static final long WINDOW_MS = 60_000; // 1 minuto

    private final Map<String, RateLimitEntry> requestCounts = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String path = request.getRequestURI();

        // Aplicar rate limit apenas no endpoint de login
        if ("/auth/login".equals(path)) {
            String clientIp = obterIpReal(request);
            RateLimitEntry entry = requestCounts.compute(clientIp, (key, existing) -> {
                long now = System.currentTimeMillis();
                if (existing == null || now - existing.windowStart > WINDOW_MS) {
                    return new RateLimitEntry(now);
                }
                existing.count.incrementAndGet();
                return existing;
            });

            if (entry.count.get() > MAX_REQUESTS_PER_MINUTE) {
                response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                response.setContentType("application/json");
                response.getWriter().write(
                        "{\"mensagem\":\"Muitas tentativas de login. Aguarde 1 minuto.\",\"codigo\":429}"
                );
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private String obterIpReal(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private static class RateLimitEntry {
        final long windowStart;
        final AtomicInteger count;

        RateLimitEntry(long windowStart) {
            this.windowStart = windowStart;
            this.count = new AtomicInteger(1);
        }
    }
}
