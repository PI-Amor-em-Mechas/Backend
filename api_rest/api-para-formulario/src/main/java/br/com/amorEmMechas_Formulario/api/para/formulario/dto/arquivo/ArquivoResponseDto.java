package br.com.amorEmMechas_Formulario.api.para.formulario.dto.arquivo;

import br.com.amorEmMechas_Formulario.api.para.formulario.entity.arquivo.StatusValidacaoDocumento;
import java.time.LocalDateTime;

public class ArquivoResponseDto {

    private Long id;

    private String nome;
    private String nomeOriginal;
    private String mimeType;
    private Long tamanho;
    private StatusValidacaoDocumento statusValidacao;
    private String validadoPor;
    private LocalDateTime validadoEm;
    private String observacaoValidacao;


    public Long getId() {
        return id;
    }

    public void setId(Long id) {
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
