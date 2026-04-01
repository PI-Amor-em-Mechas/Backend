package br.com.amorEmMechas_Formulario.api.para.formulario.dto.paciente;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public class PacienteRequestDto {

    @NotBlank(message = "Nome não pode estar vazio")
    private String nomeCompleto;

    @NotBlank(message = "Email não pode estar vazio")
    private String email;

    @NotNull(message = "Data do Pedido não pode estar vazio")
    private LocalDate dtPedido;

    @NotBlank(message = "Celular não pode estar vazio")
    private String cel;

    @NotNull(message = "Data de Nascimento não pode estar vazio")
    private LocalDate dtNasc;

    @NotBlank(message = "Estado civil não pode estar vazio")
    private String estadoCivil;

    @NotNull(message = "temFilhos não pode estar vazio")
    private Boolean temFilhos;

    @NotNull(message = "Quantidade de pessoas em casa não pode estar vazio")
    private Integer qtdPessoasEmCasa;

    @NotBlank(message = "Cpf não pode estar vazio")
    private String cpf;

    public String getCel() {
        return cel;
    }

    public void setCel(String cel) {
        this.cel = cel;
    }

    public String getCpf() {
        return cpf;
    }

    public void setCpf(String cpf) {
        this.cpf = cpf;
    }

    public LocalDate getDtNasc() {
        return dtNasc;
    }

    public void setDtNasc(LocalDate dtNasc) {
        this.dtNasc = dtNasc;
    }

    public LocalDate getDtPedido() {
        return dtPedido;
    }

    public void setDtPedido(LocalDate dtPedido) {
        this.dtPedido = dtPedido;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getEstadoCivil() {
        return estadoCivil;
    }

    public void setEstadoCivil(String estadoCivil) {
        this.estadoCivil = estadoCivil;
    }

    public String getNomeCompleto() {
        return nomeCompleto;
    }

    public void setNomeCompleto(String nomeCompleto) {
        this.nomeCompleto = nomeCompleto;
    }

    public Integer getQtdPessoasEmCasa() {
        return qtdPessoasEmCasa;
    }

    public void setQtdPessoasEmCasa(Integer qtdPessoasEmCasa) {
        this.qtdPessoasEmCasa = qtdPessoasEmCasa;
    }

    public Boolean getTemFilhos() {
        return temFilhos;
    }

    public void setTemFilhos(Boolean temFilhos) {
        this.temFilhos = temFilhos;
    }
}
