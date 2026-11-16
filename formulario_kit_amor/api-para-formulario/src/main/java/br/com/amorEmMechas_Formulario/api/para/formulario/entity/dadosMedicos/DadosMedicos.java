package br.com.amorEmMechas_Formulario.api.para.formulario.entity.dadosMedicos;


import br.com.amorEmMechas_Formulario.api.para.formulario.entity.arquivo.Arquivo;
import br.com.amorEmMechas_Formulario.api.para.formulario.entity.paciente.Paciente;
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

    @Column(name = "inicio_tratamento", nullable = false)
    private LocalDate inicioTratamento;

    @Lob
    @Column(name = "relatorio_medico", columnDefinition = "longblob", nullable = false)
    private byte[] relatorioMedico;

    @OneToOne
    @JoinColumn(name = "arquivo_id")
    private Arquivo arquivo;

    @ManyToOne
    @JoinColumn(name = "paciente_id")
    private Paciente paciente;



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

    public Paciente getPaciente() {
        return paciente;
    }

    public void setPaciente(Paciente paciente) {
        this.paciente = paciente;
    }

    public Arquivo getArquivo() {
        return arquivo;
    }

    public void setArquivo(Arquivo arquivo) {
        this.arquivo = arquivo;
    }

    @PrePersist
    @PreUpdate
    private void sincronizarColunasLegadas() {
        inicioTratamento = dtInicioTratamento != null ? dtInicioTratamento : LocalDate.of(1900, 1, 1);
        if (relatorioMedico == null) {
            relatorioMedico = new byte[0];
        }
    }
}
