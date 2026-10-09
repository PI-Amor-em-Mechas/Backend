package br.com.amorEmMechas_Formulario.api.para.formulario.entity.filho;


import br.com.amorEmMechas_Formulario.api.para.formulario.entity.paciente.Paciente;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

@Entity
@Table(name = "Filho")
public class Filho {


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_filho")
    private Integer id;

    private Integer idade;

    @ManyToOne
    @JoinColumn(name = "paciente_id" )
    @JsonIgnore
    private Paciente paciente;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Paciente getPaciente() {
        return paciente;
    }

    public void setPaciente(Paciente paciente) {
        this.paciente = paciente;
    }

    public Integer getIdade() {
        return idade;
    }

    public void setIdade(Integer idade) {
        this.idade = idade;
    }


}
