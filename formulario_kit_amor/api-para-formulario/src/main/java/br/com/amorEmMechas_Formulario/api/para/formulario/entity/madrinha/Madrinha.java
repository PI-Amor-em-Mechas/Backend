package br.com.amorEmMechas_Formulario.api.para.formulario.entity.madrinha;

import jakarta.persistence.*;

import java.time.LocalDate;

@Entity
@Table(name = "Madrinha")
public class Madrinha {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_madrinha")
    private Integer id;

    private String nomeCompleto;
    private String email;
    private Integer horasVoluntarias;
    private String funcao;
    private LocalDate dataCadastro;
    private String status;

    @Column(name = "data_inicio")
    private LocalDate dataInicio;

    @Lob
    @Column(name = "imagem_facial", columnDefinition = "longblob")
    private byte[] imagemFacial;

    @Column(name = "colaborador_id")
    private String colaboradorId;

    @PrePersist
    private void preencherCamposObrigatoriosDoSchema() {
        if (dataInicio == null) {
            dataInicio = dataCadastro != null ? dataCadastro : LocalDate.now();
        }
        if (imagemFacial == null) {
            imagemFacial = new byte[0];
        }
    }

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

    public String getColaboradorId() {
        return colaboradorId;
    }

    public void setColaboradorId(String colaboradorId) {
        this.colaboradorId = colaboradorId;
    }
}
