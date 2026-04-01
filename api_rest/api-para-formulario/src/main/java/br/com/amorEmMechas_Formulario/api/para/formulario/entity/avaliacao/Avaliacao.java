package br.com.amorEmMechas_Formulario.api.para.formulario.entity.avaliacao;


import br.com.amorEmMechas_Formulario.api.para.formulario.entity.paciente.Paciente;
import jakarta.persistence.*;

import java.time.LocalDate;

@Entity
@Table(name = "Avaliacao")
public class Avaliacao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @OneToOne
    @JoinColumn(name = "fkPaciente")
    private Paciente paciente;

    private Integer notaFormulario;

    private Boolean consentimento;

    private Boolean concluido;

    private LocalDate dtConclusao;

    public Boolean getConcluido() {
        return concluido;
    }

    public void setConcluido(Boolean concluido) {
        this.concluido = concluido;
    }

    public Boolean getConsentimento() {
        return consentimento;
    }

    public void setConsentimento(Boolean consentimento) {
        this.consentimento = consentimento;
    }

    public LocalDate getDtConclusao() {
        return dtConclusao;
    }

    public void setDtConclusao(LocalDate dtConclusao) {
        this.dtConclusao = dtConclusao;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getNotaFormulario() {
        return notaFormulario;
    }

    public void setNotaFormulario(Integer notaFormulario) {
        this.notaFormulario = notaFormulario;
    }

    public Paciente getPaciente() {
        return paciente;
    }

    public void setPaciente(Paciente paciente) {
        this.paciente = paciente;
    }
}
