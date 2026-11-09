package br.com.amorEmMechas_Formulario.api.para.formulario.entity.arquivo;


import jakarta.persistence.*;
import br.com.amorEmMechas_Formulario.api.para.formulario.entity.paciente.Paciente;
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
    private StatusValidacao statusValidacao = StatusValidacao.PENDENTE;

    @Column(length = 2000)
    private String observacaoValidacao;
    private LocalDateTime dataUpload;
    private LocalDateTime validadoEm;
    private String validadoPor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "paciente_id")
    private Paciente paciente;

    public StatusValidacao getStatusValidacao() {
        return statusValidacao == null ? StatusValidacao.PENDENTE : statusValidacao;
    }

    public void setStatusValidacao(StatusValidacao statusValidacao) {
        this.statusValidacao = statusValidacao;
    }

    public String getObservacaoValidacao() { return observacaoValidacao; }
    public void setObservacaoValidacao(String observacaoValidacao) { this.observacaoValidacao = observacaoValidacao; }
    public LocalDateTime getDataUpload() { return dataUpload; }
    public void setDataUpload(LocalDateTime dataUpload) { this.dataUpload = dataUpload; }
    public LocalDateTime getValidadoEm() { return validadoEm; }
    public void setValidadoEm(LocalDateTime validadoEm) { this.validadoEm = validadoEm; }
    public String getValidadoPor() { return validadoPor; }
    public void setValidadoPor(String validadoPor) { this.validadoPor = validadoPor; }
    public Paciente getPaciente() { return paciente; }
    public void setPaciente(Paciente paciente) { this.paciente = paciente; }

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
}
