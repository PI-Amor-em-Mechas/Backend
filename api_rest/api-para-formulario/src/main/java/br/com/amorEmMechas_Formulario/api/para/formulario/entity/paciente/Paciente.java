package br.com.amorEmMechas_Formulario.api.para.formulario.entity.paciente;


import br.com.amorEmMechas_Formulario.api.para.formulario.entity.dadosMedicos.DadosMedicos;
import br.com.amorEmMechas_Formulario.api.para.formulario.entity.endereco.Endereco;
import br.com.amorEmMechas_Formulario.api.para.formulario.entity.kitAmor.KitAmor;
import jakarta.persistence.*;

import java.time.LocalDate;

@Entity
@Table(name = "Paciente")
public class Paciente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    private String nomeCompleto;
    private String email;
    private LocalDate dtPedido;
    private String cel;
    private LocalDate dtNasc;
    private String estadoCivil;
    private Boolean temFilhos;
    private Integer qtdPessoasEmCasa;
    private String cpf;

    @Lob
    private byte[] cabeloAntes;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "fkEndereco")
    private Endereco endereco;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "fkKit")
    private KitAmor kit;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "fkDadosMedico")
    private DadosMedicos dadosMedicos;

    public byte[] getCabeloAntes() {
        return cabeloAntes;
    }

    public void setCabeloAntes(byte[] cabeloAntes) {
        this.cabeloAntes = cabeloAntes;
    }

    public String getCel() {
        return cel;
    }

    public void setCel(String cel) {
        this.cel = cel;
    }

    public String getCpf() {
        return cpf;
    }

    public void setCpf(String cpf) {
        this.cpf = cpf;
    }

    public DadosMedicos getDadosMedicos() {
        return dadosMedicos;
    }

    public void setDadosMedicos(DadosMedicos dadosMedicos) {
        this.dadosMedicos = dadosMedicos;
    }

    public LocalDate getDtNasc() {
        return dtNasc;
    }

    public void setDtNasc(LocalDate dtNasc) {
        this.dtNasc = dtNasc;
    }

    public LocalDate getDtPedido() {
        return dtPedido;
    }

    public void setDtPedido(LocalDate dtPedido) {
        this.dtPedido = dtPedido;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Endereco getEndereco() {
        return endereco;
    }

    public void setEndereco(Endereco endereco) {
        this.endereco = endereco;
    }

    public String getEstadoCivil() {
        return estadoCivil;
    }

    public void setEstadoCivil(String estadoCivil) {
        this.estadoCivil = estadoCivil;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public KitAmor getKit() {
        return kit;
    }

    public void setKit(KitAmor kit) {
        this.kit = kit;
    }

    public String getNomeCompleto() {
        return nomeCompleto;
    }

    public void setNomeCompleto(String nomeCompleto) {
        this.nomeCompleto = nomeCompleto;
    }

    public Integer getQtdPessoasEmCasa() {
        return qtdPessoasEmCasa;
    }

    public void setQtdPessoasEmCasa(Integer qtdPessoasEmCasa) {
        this.qtdPessoasEmCasa = qtdPessoasEmCasa;
    }

    public Boolean getTemFilhos() {
        return temFilhos;
    }

    public void setTemFilhos(Boolean temFilhos) {
        this.temFilhos = temFilhos;
    }
}
