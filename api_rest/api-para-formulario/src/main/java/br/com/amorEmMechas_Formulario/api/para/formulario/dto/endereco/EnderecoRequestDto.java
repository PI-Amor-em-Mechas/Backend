package br.com.amorEmMechas_Formulario.api.para.formulario.dto.endereco;

import jakarta.validation.constraints.NotBlank;

public class EnderecoRequestDto {

    @NotBlank(message = "CEP não pode estar vazio")
    private String cep;

    @NotBlank(message = "A rua pode estar vazio")
    private String rua;

    @NotBlank(message = "Número não pode estar vazio")
    private String numero;

    private String complemento;

    @NotBlank(message = "Bairro não pode estar vazio")
    private String bairro;

    @NotBlank(message = "Cidade não pode estar vazia")
    private String cidade;

    @NotBlank(message = "Estado não pode estar vazio")
    private String estado;

    public String getCep() {
        return cep;
    }

    public void setCep(String cep) {
        this.cep = cep;
    }

    public String getRua() {
        return rua;
    }

    public void setRua(String rua) {
        this.rua = rua;
    }

    public String getNumero() {
        return numero;
    }

    public void setNumero(String numero) {
        this.numero = numero;
    }

    public String getComplemento() {
        return complemento;
    }

    public void setComplemento(String complemento) {
        this.complemento = complemento;
    }

    public String getBairro() {
        return bairro;
    }

    public void setBairro(String bairro) {
        this.bairro = bairro;
    }

    public String getCidade() {
        return cidade;
    }

    public void setCidade(String cidade) {
        this.cidade = cidade;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }
}