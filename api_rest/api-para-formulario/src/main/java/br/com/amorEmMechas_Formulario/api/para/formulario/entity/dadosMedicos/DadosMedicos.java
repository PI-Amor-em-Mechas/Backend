package br.com.amorEmMechas_Formulario.api.para.formulario.entity.dadosMedicos;


import jakarta.persistence.*;

import java.time.LocalDate;

@Entity
@Table(name = "DadosMedicos")
public class DadosMedicos {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private String motivo;
    private String tipoCancer;
    private String justificativa;
    private LocalDate dtInicioTratamento;
    private String tipoAtendimento;
    @Lob
    private byte[] relatorioMedico;

    public LocalDate getDtInicioTratamento() {
        return dtInicioTratamento;
    }

    public void setDtInicioTratamento(LocalDate dtInicioTratamento) {
        this.dtInicioTratamento = dtInicioTratamento;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getJustificativa() {
        return justificativa;
    }

    public void setJustificativa(String justificativa) {
        this.justificativa = justificativa;
    }

    public String getMotivo() {
        return motivo;
    }

    public void setMotivo(String motivo) {
        this.motivo = motivo;
    }

    public byte[] getRelatorioMedico() {
        return relatorioMedico;
    }

    public void setRelatorioMedico(byte[] relatorioMedico) {
        this.relatorioMedico = relatorioMedico;
    }

    public String getTipoAtendimento() {
        return tipoAtendimento;
    }

    public void setTipoAtendimento(String tipoAtendimento) {
        this.tipoAtendimento = tipoAtendimento;
    }

    public String getTipoCancer() {
        return tipoCancer;
    }

    public void setTipoCancer(String tipoCancer) {
        this.tipoCancer = tipoCancer;
    }
}
