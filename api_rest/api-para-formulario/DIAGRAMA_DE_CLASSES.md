# Diagrama de classes

Este diagrama representa as entidades JPA do backend e os relacionamentos
declarados nas classes do pacote `entity` e em `security.audit`.

```mermaid
classDiagram
    direction LR

    class Paciente {
        +Integer id
        +String nomeCompleto
        +String email
        +LocalDate dtPedido
        +String cel
        +LocalDate dtNasc
        +String estadoCivil
        +Boolean temFilhos
        +Integer qtdFilhos
        +Integer qtdPessoasEmCasa
        +String cpf
        +Boolean consentimentoLgpd
        +LocalDateTime dtConsentimento
        +String finalidadeTratamento
        +Boolean dadosAnonimizados
        +LocalDateTime dtAnonimizacao
    }

    class Filho {
        +Integer id
        +Integer idade
        +Paciente paciente
    }

    class DadosMedicos {
        +Integer id
        +String motivo
        +String tipoCancer
        +String justificativa
        +LocalDate dtInicioTratamento
        +String tipoAtendimento
        +Arquivo arquivo
        +Paciente paciente
    }

    class Arquivo {
        +Integer id
        +String nomeOriginal
        +String nome
        +String mimeType
        +Long tamanho
        +String tipo
        +String caminhoArquivo
    }

    class Endereco {
        +Integer id
        +String rua
        +String numero
        +String bairro
        +String cidade
        +String estado
        +String cep
    }

    class Solicitante {
        +Integer id
        +String nomeCompleto
        +String rg
    }

    class Avaliacao {
        +Integer id
        +Solicitante solicitante
        +Integer notaFormulario
        +Boolean consentimento
        +Boolean concluido
        +LocalDate dtConclusao
    }

    class KitAmor {
        +Integer id
        +String corPeruca
        +Solicitante solicitante
        +Paciente paciente
    }

    class Madrinha {
        +Integer id
        +String nomeCompleto
        +String email
        +Integer horasVoluntarias
        +String funcao
        +LocalDate dataCadastro
        +String status
    }

    class Usuario {
        +Long id
        +String username
        +String password
        +String role
        +getAuthorities() Collection
        +isEnabled() boolean
    }

    class AuditLog {
        +Long id
        +LocalDateTime timestamp
        +String usuario
        +TipoEvento tipoEvento
        +String recurso
        +String metodoHttp
        +String enderecoIp
        +String userAgent
        +String statusResposta
        +String detalhes
        +boolean sucesso
    }

    class TipoEvento {
        <<enumeration>>
        LOGIN
        LOGOUT
        LOGIN_FALHA
        ACESSO_DADOS_MEDICOS
        ACESSO_PACIENTE
        DOWNLOAD_ARQUIVO
        UPLOAD_ARQUIVO
        ACESSO_NEGADO
        TOKEN_EXPIRADO
        TOKEN_INVALIDO
    }

    Paciente "1" *-- "0..*" Filho : filhos
    Paciente "1" *-- "0..*" DadosMedicos : dadosMedicos
    Paciente "0..1" --> "1" Arquivo : cabeloAntes
    Paciente "1" --> "1" Endereco : endereco
    Paciente "0..*" --> "1" Solicitante : solicitante
    DadosMedicos "0..1" --> "1" Arquivo : arquivo
    Avaliacao "0..1" --> "1" Solicitante : solicitante
    KitAmor "0..*" --> "1" Solicitante : solicitante
    KitAmor "0..*" --> "1" Paciente : paciente
    AuditLog --> TipoEvento : usa

    class UserDetails {
        <<interface>>
    }

    Usuario ..|> UserDetails : implementa
```

## Observacoes

- As cardinalidades foram inferidas das anotacoes JPA presentes no codigo.
- `Paciente` possui `@OneToMany` para `Filho` e `DadosMedicos`; as setas
  representam tambem o sentido da referencia mantida pelo modelo.
- `KitAmor`, `Avaliacao` e `DadosMedicos` possuem referencias para outras
  entidades, mas as classes relacionadas nao declaram necessariamente o lado
  inverso.
- `Madrinha` nao possui relacionamento JPA com as demais entidades.
- `Usuario` e `AuditLog` sao entidades independentes no mapeamento JPA. O
  campo textual `AuditLog.usuario` registra o identificador do usuario, sem
  uma associacao `@ManyToOne`.
- O diagrama nao inclui DTOs, controllers, services e repositories para manter
  o foco no modelo persistente.