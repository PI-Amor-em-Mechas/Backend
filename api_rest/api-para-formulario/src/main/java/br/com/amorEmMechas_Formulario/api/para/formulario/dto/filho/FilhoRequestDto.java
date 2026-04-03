package br.com.amorEmMechas_Formulario.api.para.formulario.dto.filho;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class FilhoRequestDto {


    @NotNull(message = "Idade não pode ser nula")
    private Integer idade;

    private Integer qtdFilho;
    @NotNull(message = "não pode ser nulo paciente")
    private Integer pacienteId;

    public Integer getIdade() {
        return idade;
    }

    public Integer getQtdFilho() {
        return qtdFilho;
    }

    public void setQtdFilho(Integer qtdFilho) {
        this.qtdFilho = qtdFilho;
    }

    public Integer getPacienteId() {
        return pacienteId;
    }

    public void setPacienteId(Integer pacienteId) {
        this.pacienteId = pacienteId;
    }

    public void setIdade(Integer idade) {
        this.idade = idade;
    }
}
