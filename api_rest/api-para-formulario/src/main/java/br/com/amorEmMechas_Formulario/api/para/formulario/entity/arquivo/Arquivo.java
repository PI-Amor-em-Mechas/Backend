package br.com.amorEmMechas_Formulario.api.para.formulario.entity.arquivo;


import jakarta.persistence.*;
import java.time.LocalDateTime;


@Entity
public class Arquivo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private String nomeOriginal;

    private String nome;

    private String mimeType;

    private Long tamanho;

    private String tipo;

    private String caminhoArquivo;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_validacao", nullable = false, length = 20)
    private StatusValidacaoDocumento statusValidacao = StatusValidacaoDocumento.PENDENTE;

    @Column(name = "validado_por", length = 100)
    private String validadoPor;

    @Column(name = "validado_em")
    private LocalDateTime validadoEm;

    @Column(name = "observacao_validacao", length = 1000)
    private String observacaoValidacao;

    public String getCaminhoArquivo() {
        return caminhoArquivo;
    }

    public void setCaminhoArquivo(String caminhoArquivo) {
        this.caminhoArquivo = caminhoArquivo;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getNomeOriginal() {
        return nomeOriginal;
    }

    public void setNomeOriginal(String nomeOriginal) {
        this.nomeOriginal = nomeOriginal;
    }

    public Long getTamanho() {
        return tamanho;
    }

    public void setTamanho(Long tamanho) {
        this.tamanho = tamanho;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public StatusValidacaoDocumento getStatusValidacao() {
        return statusValidacao;
    }

    public void setStatusValidacao(StatusValidacaoDocumento statusValidacao) {
        this.statusValidacao = statusValidacao;
    }

    public String getValidadoPor() {
        return validadoPor;
    }

    public void setValidadoPor(String validadoPor) {
        this.validadoPor = validadoPor;
    }

    public LocalDateTime getValidadoEm() {
        return validadoEm;
    }

    public void setValidadoEm(LocalDateTime validadoEm) {
        this.validadoEm = validadoEm;
    }

    public String getObservacaoValidacao() {
        return observacaoValidacao;
    }

    public void setObservacaoValidacao(String observacaoValidacao) {
        this.observacaoValidacao = observacaoValidacao;
    }
}
