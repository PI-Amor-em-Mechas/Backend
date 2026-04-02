package br.com.amorEmMechas_Formulario.api.para.formulario.dto.solicitante;

public class SolicitanteResponseDto {


    private Integer id;
    private String nomeCompleto;
    private String rg;

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

    public String getRg() {
        return rg;
    }

    public void setRg(String rg) {
        this.rg = rg;
    }
}
