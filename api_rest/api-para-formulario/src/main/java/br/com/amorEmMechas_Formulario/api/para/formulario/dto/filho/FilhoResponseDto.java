package br.com.amorEmMechas_Formulario.api.para.formulario.dto.filho;

import br.com.amorEmMechas_Formulario.api.para.formulario.entity.filho.Filho;

public class FilhoResponseDto {
    private Integer id;
    private Integer idade;
    private Integer pacienteId;
    private Integer qtdFilho;

    public FilhoResponseDto(Filho filho, Integer qtdFilho) {
        this.id = filho.getId();
        this.idade = filho.getIdade();
        this.pacienteId = filho.getPaciente().getId();
        this.qtdFilho = qtdFilho;
    }

    public FilhoResponseDto() {

    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getIdade() {
        return idade;
    }

    public void setIdade(Integer idade) {
        this.idade = idade;
    }

    public Integer getPacienteId() {
        return pacienteId;
    }

    public void setPacienteId(Integer pacienteId) {
        this.pacienteId = pacienteId;
    }

    public Integer getQtdFilho() {
        return qtdFilho;
    }

    public void setQtdFilho(Integer qtdFilho) {
        this.qtdFilho = qtdFilho;
    }
}
