package br.com.amorEmMechas_Formulario.api.para.formulario.dto.paciente;

import br.com.amorEmMechas_Formulario.api.para.formulario.dto.dadosMedicos.DadosMedicosResponseDto;
import br.com.amorEmMechas_Formulario.api.para.formulario.dto.endereco.EnderecoResponseDto;
import br.com.amorEmMechas_Formulario.api.para.formulario.dto.filho.FilhoResponseDto;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDate;
import java.util.List;

public class PacienteResponseDto {

    private Integer id;
    private String nomeCompleto;
    private String email;
    private LocalDate dtPedido;
    private String cabeloAntes;
    private String cel;
    private LocalDate dtNasc;
    private String estadoCivil;
    private Boolean temFilhos;
    private Integer qtdPessoasEmCasa;
    private String cpf;

    private EnderecoResponseDto endereco;
    private DadosMedicosResponseDto dadosMedicos;

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private List<FilhoResponseDto> filhos;

    private Integer qtdFilho;


    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

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

    public String getCabeloAntes() {
        return cabeloAntes;
    }

    public void setCabeloAntes(String cabeloAntes) {
        this.cabeloAntes = cabeloAntes;
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

    public EnderecoResponseDto getEndereco() {
        return endereco;
    }

    public void setEndereco(EnderecoResponseDto endereco) {
        this.endereco = endereco;
    }

    public DadosMedicosResponseDto getDadosMedicos() {
        return dadosMedicos;
    }

    public void setDadosMedicos(DadosMedicosResponseDto dadosMedicos) {
        this.dadosMedicos = dadosMedicos;
    }

    public List<FilhoResponseDto> getFilhos() {
        return filhos;
    }

    public void setFilhos(List<FilhoResponseDto> filhos) {
        this.filhos = filhos;
    }

    public Integer getQtdFilho() {
        return qtdFilho;
    }

    public void setQtdFilho(Integer qtdFilho) {
        this.qtdFilho = qtdFilho;
    }
}
