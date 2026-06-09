package br.com.amorEmMechas_Formulario.api.para.formulario.dto.madrinha;

import java.time.LocalDate;

public class MadrinhaResponseDto {

    private Integer id;
    private String nomeCompleto;
    private String email;
    private Integer horasVoluntarias;
    private String funcao;
    private LocalDate dataCadastro;
    private String status;

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

    public Integer getHorasVoluntarias() {
        return horasVoluntarias;
    }

    public void setHorasVoluntarias(Integer horasVoluntarias) {
        this.horasVoluntarias = horasVoluntarias;
    }

    public String getFuncao() {
        return funcao;
    }

    public void setFuncao(String funcao) {
        this.funcao = funcao;
    }

    public LocalDate getDataCadastro() {
        return dataCadastro;
    }

    public void setDataCadastro(LocalDate dataCadastro) {
        this.dataCadastro = dataCadastro;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
