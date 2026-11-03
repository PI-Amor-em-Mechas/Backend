package br.com.amorEmMechas_Formulario.api.para.formulario.config;

import br.com.amorEmMechas_Formulario.api.para.formulario.security.JwtAuthenticationFilter;
import br.com.amorEmMechas_Formulario.api.para.formulario.security.JwtAuthenticationEntryPoint;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.http.HttpMethod;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * ConfiguraÃ§Ã£o de seguranÃ§a conforme protocolo HL7 para sistemas de saÃºde.
 *
 * Implementa:
 * - AutenticaÃ§Ã£o stateless via JWT (Bearer Token)
 * - Headers de seguranÃ§a rigorosos (HSTS, CSP, X-Content-Type-Options, etc.)
 * - CORS restritivo
 * - ProteÃ§Ã£o contra ataques comuns (XSS, clickjacking, MIME sniffing)
 * - Controle de acesso baseado em roles (RBAC)
 * - SessÃµes stateless (sem estado no servidor)
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    @Value("${hl7.security.cors.allowed-origins}")
    private String allowedOrigins;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter,
                          JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.jwtAuthenticationEntryPoint = jwtAuthenticationEntryPoint;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12); // Fator de custo 12 para maior seguranÃ§a
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // CSRF desabilitado pois a API usa JWT stateless (token no header)
                .csrf(csrf -> csrf.disable())

                // CORS restritivo
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // Headers de seguranÃ§a HL7
                .headers(headers -> headers
                        .frameOptions(frame -> frame.deny())
                        .contentTypeOptions(content -> {}) // X-Content-Type-Options: nosniff
                        .httpStrictTransportSecurity(hsts -> hsts
                                .includeSubDomains(true)
                                .maxAgeInSeconds(31536000) // 1 ano
                        )
                        .cacheControl(cache -> {}) // Cache-Control: no-cache, no-store
                        .addHeaderWriter((request, response) -> {
                            response.setHeader("X-Content-Type-Options", "nosniff");
                            response.setHeader("X-Frame-Options", "DENY");
                            response.setHeader("X-XSS-Protection", "1; mode=block");
                            response.setHeader("Content-Security-Policy",
                                    "default-src 'self'; script-src 'self'; style-src 'self' 'unsafe-inline'");
                            response.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");
                            response.setHeader("Permissions-Policy",
                                    "camera=(), microphone=(), geolocation=()");
                        })
                )

                // AutorizaÃ§Ã£o de endpoints
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/auth/**",
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/webjars/**",
                                "/favicon.ico",
                                "/actuator/health", "/avaliacoes/**"
                        ).permitAll()

                        // === FORMULARIO PUBLICO DE SOLICITACAO (sem login) ===
                        // Cadastro (POST) de tudo que o paciente preenche no formulario inicial.
                        // Atualizar, excluir e consultar continuam exigindo login (regras abaixo).
                        .requestMatchers(HttpMethod.POST,
                                "/formulario-solicitacao-peruca",
                                "/pacientes",
                                "/enderecos",
                                "/enderecos/viacep",
                                "/filhos",
                                "/solicitantes",
                                "/kits",
                                "/dados-medicos",
                                "/arquivos"
                        ).permitAll()

                        // LGPD - Anonimizacao apenas para ADMIN
                        .requestMatchers("/pacientes/*/anonimizar").hasRole("ADMIN")

                        // Endpoints de dados mÃ©dicos e avaliaÃ§Ãµes (fora do POST publico acima)
                        // continuam exigindo role especÃ­fica
                        .requestMatchers("/dados-medicos/**", "/dadosMedicos/**")
                        .hasAnyRole("ADMIN", "MEDICO", "ENFERMEIRO")

                        // Madrinhas - uso interno da equipe, sempre privado
                        .requestMatchers("/madrinhas/**")
                        .hasAnyRole("ADMIN", "MEDICO", "ENFERMEIRO", "ATENDENTE")

                        // Atualizar / excluir / consultar (fora do POST publico acima) exigem login da equipe
                        .requestMatchers(
                                "/pacientes/**",
                                "/enderecos/**",
                                "/filhos/**",
                                "/solicitantes/**",
                                "/kits/**"
                        ).hasAnyRole("ADMIN", "MEDICO", "ENFERMEIRO", "ATENDENTE")

                        // Arquivos (laudos) - listar/baixar exige autenticaÃ§Ã£o; upload jÃ¡ Ã© publico acima
                        .requestMatchers("/arquivos/**").authenticated()

                        .anyRequest().authenticated()
                )

                // SessÃ£o stateless (JWT)
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                // Tratamento de erros de autenticaÃ§Ã£o
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(jwtAuthenticationEntryPoint)
                )

                // Filtro JWT antes do filtro de autenticaÃ§Ã£o padrÃ£o
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of(allowedOrigins.split(",")));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Requested-With"));
        configuration.setExposedHeaders(List.of("Authorization"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public WebSecurityCustomizer webSecurityCustomizer() {
        return (web) -> web.ignoring().requestMatchers(
                "/v3/api-docs/**",
                "/swagger-ui/**",
                "/swagger-ui.html",
                "/webjars/**"
        );
    }
}