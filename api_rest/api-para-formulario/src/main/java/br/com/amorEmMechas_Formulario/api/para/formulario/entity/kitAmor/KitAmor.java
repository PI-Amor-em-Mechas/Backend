package br.com.amorEmMechas_Formulario.api.para.formulario.entity.kitAmor;


import br.com.amorEmMechas_Formulario.api.para.formulario.entity.paciente.Paciente;
import br.com.amorEmMechas_Formulario.api.para.formulario.entity.solicitante.Solicitante;
import jakarta.persistence.*;

@Entity
@Table(name = "KitAmor")
public class KitAmor {


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    private String corPeruca;

    @ManyToOne
    @JoinColumn(name = "solicitante_id")
    private Solicitante solicitante;

    @ManyToOne
    @JoinColumn(name = "paciente_id")
    private Paciente paciente;


    public String getCorPeruca() {
        return corPeruca;
    }

    public void setCorPeruca(String corPeruca) {
        this.corPeruca = corPeruca;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Solicitante getSolicitante() {
        return solicitante;
    }

    public void setSolicitante(Solicitante solicitante) {
        this.solicitante = solicitante;
    }

    public Paciente getPaciente() {
        return paciente;
    }

    public void setPaciente(Paciente paciente) {
        this.paciente = paciente;
    }
}
