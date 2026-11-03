package br.com.amorEmMechas_Formulario.api.para.formulario.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Filtro de autenticaÃ§Ã£o JWT para conformidade HL7.
 * Intercepta requisiÃ§Ãµes e valida o token Bearer no header Authorization.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserDetailsService userDetailsService;

    @Value("${app.dev-token.enabled:false}")
    private boolean devTokenEnabled;

    public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider, UserDetailsService userDetailsService) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String jwt = extractJwtFromRequest(request);

        if (StringUtils.hasText(jwt) && jwtTokenProvider.validateToken(jwt)) {
            String tokenType = jwtTokenProvider.getTokenType(jwt);

            // Somente tokens de acesso sÃ£o vÃ¡lidos para autenticaÃ§Ã£o de requisiÃ§Ãµes
            if ("access".equals(tokenType)) {
                String username = jwtTokenProvider.getUsernameFromToken(jwt);
                UserDetails userDetails = loadUserDetails(jwt, username);

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                userDetails,
                                null,
                                userDetails.getAuthorities()
                        );

                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        }

        filterChain.doFilter(request, response);
    }

    private UserDetails loadUserDetails(String jwt, String username) {
        if (devTokenEnabled && Boolean.TRUE.equals(jwtTokenProvider.getClaim(jwt, "dev", Boolean.class))) {
            String role = jwtTokenProvider.getClaim(jwt, "role", String.class);
            return org.springframework.security.core.userdetails.User.withUsername(username)
                    .password("")
                    .authorities(role)
                    .build();
        }
        return userDetailsService.loadUserByUsername(username);
    }

    private String extractJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
