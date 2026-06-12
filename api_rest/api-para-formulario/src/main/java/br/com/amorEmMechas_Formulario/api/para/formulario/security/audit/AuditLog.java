package br.com.amorEmMechas_Formulario.api.para.formulario.security.audit;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Entidade de log de auditoria conforme HL7 Security Framework.
 * Registra todos os acessos a dados médicos protegidos (PHI).
 * Requisito: Rastreabilidade completa de quem acessou, quando e o quê.
 */
@Entity
@Table(name = "hl7_audit_log", indexes = {
        @Index(name = "idx_audit_timestamp", columnList = "timestamp"),
        @Index(name = "idx_audit_usuario", columnList = "usuario"),
        @Index(name = "idx_audit_recurso", columnList = "recurso"),
        @Index(name = "idx_audit_evento", columnList = "tipoEvento")
})
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    @Column(nullable = false, length = 100)
    private String usuario;

    @Column(nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private TipoEvento tipoEvento;

    @Column(nullable = false, length = 255)
    private String recurso;

    @Column(length = 50)
    private String metodoHttp;

    @Column(length = 45)
    private String enderecoIp;

    @Column(length = 500)
    private String userAgent;

    @Column(length = 20)
    private String statusResposta;

    @Column(length = 1000)
    private String detalhes;

    @Column(nullable = false)
    private boolean sucesso;

    public enum TipoEvento {
        LOGIN,
        LOGOUT,
        LOGIN_FALHA,
        ACESSO_DADOS_MEDICOS,
        CRIACAO_LAUDO,
        ALTERACAO_LAUDO,
        EXCLUSAO_LAUDO,
        ACESSO_PACIENTE,
        ALTERACAO_PACIENTE,
        DOWNLOAD_ARQUIVO,
        UPLOAD_ARQUIVO,
        ACESSO_NEGADO,
        TOKEN_EXPIRADO,
        TOKEN_INVALIDO
    }

    // Construtores
    public AuditLog() {
        this.timestamp = LocalDateTime.now();
    }

    public AuditLog(String usuario, TipoEvento tipoEvento, String recurso, boolean sucesso) {
        this();
        this.usuario = usuario;
        this.tipoEvento = tipoEvento;
        this.recurso = recurso;
        this.sucesso = sucesso;
    }

    // Getters e Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

    public String getUsuario() { return usuario; }
    public void setUsuario(String usuario) { this.usuario = usuario; }

    public TipoEvento getTipoEvento() { return tipoEvento; }
    public void setTipoEvento(TipoEvento tipoEvento) { this.tipoEvento = tipoEvento; }

    public String getRecurso() { return recurso; }
    public void setRecurso(String recurso) { this.recurso = recurso; }

    public String getMetodoHttp() { return metodoHttp; }
    public void setMetodoHttp(String metodoHttp) { this.metodoHttp = metodoHttp; }

    public String getEnderecoIp() { return enderecoIp; }
    public void setEnderecoIp(String enderecoIp) { this.enderecoIp = enderecoIp; }

    public String getUserAgent() { return userAgent; }
    public void setUserAgent(String userAgent) { this.userAgent = userAgent; }

    public String getStatusResposta() { return statusResposta; }
    public void setStatusResposta(String statusResposta) { this.statusResposta = statusResposta; }

    public String getDetalhes() { return detalhes; }
    public void setDetalhes(String detalhes) { this.detalhes = detalhes; }

    public boolean isSucesso() { return sucesso; }
    public void setSucesso(boolean sucesso) { this.sucesso = sucesso; }
}
