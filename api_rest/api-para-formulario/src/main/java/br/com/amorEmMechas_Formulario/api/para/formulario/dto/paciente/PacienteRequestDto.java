package br.com.amorEmMechas_Formulario.api.para.formulario.dto.paciente;

import br.com.amorEmMechas_Formulario.api.para.formulario.dto.dadosMedicos.DadosMedicosRequestDto;
import br.com.amorEmMechas_Formulario.api.para.formulario.dto.endereco.EnderecoRequestDto;
import br.com.amorEmMechas_Formulario.api.para.formulario.dto.filho.FilhoRequestDto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

public class PacienteRequestDto {

    @NotBlank(message = "Nome não pode estar vazio")
    private String nomeCompleto;

    @NotBlank(message = "Email não pode estar vazio")
    private String email;

    @NotNull(message = "Data do pedido não pode estar vazia")
    private LocalDate dtPedido;

    @NotBlank(message = "Celular não pode estar vazio")
    private String cel;

    @NotNull(message = "Data de nascimento não pode estar vazia")
    private LocalDate dtNasc;

    @NotBlank(message = "Estado civil não pode estar vazio")
    private String estadoCivil;

    @NotNull(message = "temFilhos não pode estar vazio")
    private Boolean temFilhos;

    @NotNull(message = "Quantidade de pessoas em casa não pode estar vazia")
    private Integer qtdPessoasEmCasa;

    @NotBlank(message = "CPF não pode estar vazio")
    private String cpf;

    private String cabeloAntesBase64;



    @Valid
    @NotNull(message = "Endereço não pode ser nulo")
    private EnderecoRequestDto endereco;

    @Valid
    @NotNull(message = "Dados médicos não podem ser nulos")
    private DadosMedicosRequestDto dadosMedicos;


    private List<FilhoRequestDto> filhos;

    public String getNomeCompleto() {
        return nomeCompleto;
    }

    public void setNomeCompleto(String nomeCompleto) {
        this.nomeCompleto = nomeCompleto;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public LocalDate getDtPedido() {
        return dtPedido;
    }

    public void setDtPedido(LocalDate dtPedido) {
        this.dtPedido = dtPedido;
    }

    public String getCel() {
        return cel;
    }

    public void setCel(String cel) {
        this.cel = cel;
    }

    public LocalDate getDtNasc() {
        return dtNasc;
    }

    public void setDtNasc(LocalDate dtNasc) {
        this.dtNasc = dtNasc;
    }

    public String getEstadoCivil() {
        return estadoCivil;
    }

    public void setEstadoCivil(String estadoCivil) {
        this.estadoCivil = estadoCivil;
    }

    public Boolean getTemFilhos() {
        return temFilhos;
    }

    public void setTemFilhos(Boolean temFilhos) {
        this.temFilhos = temFilhos;
    }

    public Integer getQtdPessoasEmCasa() {
        return qtdPessoasEmCasa;
    }

    public void setQtdPessoasEmCasa(Integer qtdPessoasEmCasa) {
        this.qtdPessoasEmCasa = qtdPessoasEmCasa;
    }

    public String getCpf() {
        return cpf;
    }

    public void setCpf(String cpf) {
        this.cpf = cpf;
    }

    public String getCabeloAntesBase64() {
        return cabeloAntesBase64;
    }

    public void setCabeloAntesBase64(String cabeloAntesBase64) {
        this.cabeloAntesBase64 = cabeloAntesBase64;
    }

    public EnderecoRequestDto getEndereco() {
        return endereco;
    }

    public void setEndereco(EnderecoRequestDto endereco) {
        this.endereco = endereco;
    }

    public DadosMedicosRequestDto getDadosMedicos() {
        return dadosMedicos;
    }

    public void setDadosMedicos(DadosMedicosRequestDto dadosMedicos) {
        this.dadosMedicos = dadosMedicos;
    }

    public List<FilhoRequestDto> getFilhos() {
        return filhos;
    }

    public void setFilhos(List<FilhoRequestDto> filhos) {
        this.filhos = filhos;
    }
}