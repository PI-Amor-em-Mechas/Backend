# Exemplo de Entidades JPA para MySQL

## 1. Solicitante Entity

```java
package br.com.amorEmMechas_Formulario.api.para.formulario.entity.solicitante;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "solicitante")
public class Solicitante {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_solicitante")
    private Integer id;
    
    @Column(name = "nome_completo", nullable = false)
    private String nomeCompleto;
    
    @Column(name = "rg", nullable = false, length = 9)
    private String rg;
}
```

## 2. Endereco Entity

```java
package br.com.amorEmMechas_Formulario.api.para.formulario.entity.endereco;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "endereco")
public class Endereco {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_endereco")
    private Integer id;
    
    @Column(nullable = false)
    private String rua;
    
    @Column(length = 4, nullable = false)
    private String numero;
    
    @Column(nullable = false)
    private String bairro;
    
    @Column(nullable = false)
    private String cidade;
    
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private Estado estado;
    
    @Column(length = 8, nullable = false)
    private String cep;
    
    public enum Estado {
        ACRE, ALAGOAS, AMAPA, AMAZONAS, BAHIA, CEARA,
        DISTRITO_FEDERAL, ESPIRITO_SANTO, GOIAS, MARANHAO,
        MATO_GROSSO, MATO_GROSSO_DO_SUL, MINAS_GERAIS, PARA,
        PARAIBA, PARANA, PERNAMBUCO, PIAUI, RIO_DE_JANEIRO,
        RIO_GRANDE_DO_NORTE, RIO_GRANDE_DO_SUL, RONDONIA,
        RORAIMA, SANTA_CATARINA, SAO_PAULO, SERGIPE, TOCANTINS
    }
}
```

## 3. DadosMedicos Entity

```java
package br.com.amorEmMechas_Formulario.api.para.formulario.entity.dadosMedicos;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;

@Data
@Entity
@Table(name = "dados_medicos")
public class DadosMedicos {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private Motivo motivo;
    
    @Column(name = "tipo_cancer")
    @Enumerated(EnumType.STRING)
    private TipoCancer tipoCancer;
    
    @Column(nullable = false)
    private String justificativa;
    
    @Column(name = "inicio_tratamento", nullable = false)
    private LocalDate inicioTratamento;
    
    @Column(name = "tipo_atendimento", nullable = false)
    @Enumerated(EnumType.STRING)
    private TipoAtendimento tipoAtendimento;
    
    @Column(name = "relatorio_medico", nullable = false)
    @Lob
    private byte[] relatorioMedico;
    
    public enum Motivo {
        TRATAMENTO_QUIMIOTERÁPICO, ALOPECIA_AREATA, OUTROS
    }
    
    public enum TipoCancer {
        MAMA, OVARIO, LEUCEMIA, OUTRO
    }
    
    public enum TipoAtendimento {
        PUBLICO_SUS, CONVENIO, PARTICULAR, OUTRO
    }
}
```

## 4. KitAmor Entity

```java
package br.com.amorEmMechas_Formulario.api.para.formulario.entity.kitAmor;

import jakarta.persistence.*;
import br.com.amorEmMechas_Formulario.api.para.formulario.entity.solicitante.Solicitante;
import lombok.Data;

@Data
@Entity
@Table(name = "kit_do_amor")
public class KitAmor {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_kit")
    private Integer id;
    
    @Column(name = "cor_peruca", nullable = false, length = 45)
    private String corPeruca;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fk_solicitante", nullable = false)
    private Solicitante solicitante;
}
```

## 5. Paciente Entity (Atualizado)

```java
package br.com.amorEmMechas_Formulario.api.para.formulario.entity.paciente;

import jakarta.persistence.*;
import br.com.amorEmMechas_Formulario.api.para.formulario.entity.endereco.Endereco;
import br.com.amorEmMechas_Formulario.api.para.formulario.entity.kitAmor.KitAmor;
import br.com.amorEmMechas_Formulario.api.para.formulario.entity.dadosMedicos.DadosMedicos;
import lombok.Data;
import java.time.LocalDate;
import java.util.List;

@Data
@Entity
@Table(name = "paciente")
public class Paciente {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_paciente")
    private Integer id;
    
    @Column(name = "data_pedido", nullable = false)
    private LocalDate dataPedido;
    
    @Column(nullable = false)
    private String email;
    
    @Column(name = "nome_completo", nullable = false)
    private String nomeCompleto;
    
    @Column(nullable = false, length = 11)
    private String celular;
    
    @Column(name = "data_nascimento", nullable = false)
    private LocalDate dataNascimento;
    
    @Column(name = "estado_civil", nullable = false)
    @Enumerated(EnumType.STRING)
    private EstadoCivil estadoCivil;
    
    @Column(name = "qtd_pessoas_casa", nullable = false)
    private Integer qtdPessoasCasa;
    
    @Column(length = 11, nullable = false)
    private String cpf;
    
    @Column(name = "cabelo_antes", nullable = false)
    @Lob
    private byte[] cabeloAntes;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fk_endereco")
    private Endereco endereco;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fk_kit")
    private KitAmor kit;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fk_dados_medicos")
    private DadosMedicos dadosMedicos;
    
    @OneToMany(mappedBy = "paciente", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Filho> filhos;
    
    public enum EstadoCivil {
        SOLTEIRO, CASADO, DIVORCIADO, SEPARADO, VIUVO
    }
}
```

## 6. Filho Entity

```java
package br.com.amorEmMechas_Formulario.api.para.formulario.entity.filho;

import jakarta.persistence.*;
import br.com.amorEmMechas_Formulario.api.para.formulario.entity.paciente.Paciente;
import lombok.Data;

@Data
@Entity
@Table(name = "filho")
public class Filho {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_filho")
    private Integer id;
    
    @Column(nullable = false)
    private Integer idade;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fk_paciente", nullable = false)
    private Paciente paciente;
}
```

## 7. Avaliacao Entity

```java
package br.com.amorEmMechas_Formulario.api.para.formulario.entity.avaliacao;

import jakarta.persistence.*;
import br.com.amorEmMechas_Formulario.api.para.formulario.entity.solicitante.Solicitante;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "avaliacao")
public class Avaliacao {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_avaliacao")
    private Integer id;
    
    @Column(name = "nota_avaliacao", nullable = false)
    private LocalDate notaAvaliacao;
    
    @Column(nullable = false)
    private Boolean consentimento;
    
    @Column(nullable = false)
    private Boolean concluido;
    
    @Column(name = "data_conclusao")
    private LocalDateTime dataConclusao;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fk_solicitante")
    private Solicitante solicitante;
}
```

## 8. Madrinha Entity

```java
package br.com.amorEmMechas_Formulario.api.para.formulario.entity.madrinha;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;

@Data
@Entity
@Table(name = "madrinha")
public class Madrinha {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_madrinha")
    private Integer id;
    
    @Column(name = "nome_completo", nullable = false)
    private String nomeCompleto;
    
    @Column(name = "data_inicio", nullable = false)
    private LocalDate dataInicio;
    
    @Column(name = "imagem_facial", nullable = false)
    @Lob
    private byte[] imagemFacial;
}
```

## Dependência no pom.xml

Adicione estas dependências (se ainda não existem):

```xml
<!-- MySQL -->
<dependency>
    <groupId>com.mysql</groupId>
    <artifactId>mysql-connector-java</artifactId>
    <version>8.0.33</version>
</dependency>

<!-- Lombok (opcional, mas recomendado) -->
<dependency>
    <groupId>org.projectlombok</groupId>
    <artifactId>lombok</artifactId>
    <optional>true</optional>
</dependency>

<!-- Spring Data JPA -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa</artifactId>
</dependency>
```

## Passos para usar:

1. ✅ Já atualizei o `application.properties` com as configurações MySQL
2. Crie as pastas de packages para as entidades
3. Copie cada entidade para seu arquivo correspondente
4. Atualize o `pom.xml` com as dependências
5. Reinicie a aplicação

A propriedade `spring.jpa.hibernate.ddl-auto=update` criará as tabelas automaticamente se não existirem.
