package br.com.amorEmMechas_Formulario.api.para.formulario.dto.filho;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class FilhoRequestDto {


    @NotNull(message = "Idade não pode ser vazia")
    private Integer idade;




    public Integer getIdade() {
        return idade;
    }

    public void setIdade(Integer idade) {
        this.idade = idade;
    }
}
