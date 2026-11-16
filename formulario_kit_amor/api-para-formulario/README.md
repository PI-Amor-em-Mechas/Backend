# Amor em Mechas - API REST

API REST para gerenciamento do formulário de cadastro de pacientes do projeto **Amor em Mechas**, uma ONG que confecciona perucas para pacientes em tratamento oncológico.

## Tecnologias

| Tecnologia | Versão |
|---|---|
| Java | 21 |
| Spring Boot | 3.5.6 |
| Spring Security | JWT Stateless |
| MySQL | 8+ |
| Maven | Wrapper incluso |
| Swagger/OpenAPI | springdoc 2.8.16 |
| jjwt | 0.12.6 |

## Segurança (controles técnicos alinhados ao escopo analisado)

A API implementa o protocolo de segurança HL7 para proteção de dados médicos:

- **Autenticação JWT** — Access token (15 min) + Refresh token (8h)
- **Criptografia AES-256-GCM** — Dados sensíveis (CPF, dados médicos) criptografados em repouso
- **RBAC** — Controle de acesso baseado em roles (ADMIN, MEDICO, ENFERMEIRO, ATENDENTE, USER)
- **Rate Limiting** — 10 requisições/min por IP no endpoint de login
- **Auditoria** — Log completo de eventos de autenticação e acesso a dados sensíveis
- **CORS centralizado** — Configurado no SecurityConfig (sem `@CrossOrigin` nos controllers)
- **Headers de segurança** — HSTS, CSP, X-Content-Type-Options

## LGPD (Lei Geral de Proteção de Dados)

Os controles abaixo foram implementados/alinhados ao escopo analisado. Isso não constitui declaração de conformidade jurídica:

| Artigo | Requisito | Implementação |
|---|---|---|
| Art. 7/8 | Consentimento | Campo `consentimentoLgpd` obrigatório no cadastro, com timestamp |
| Art. 8 §5 | Revogação | `POST /pacientes/{id}/revogar-consentimento` |
| Art. 18 V | Portabilidade | `GET /pacientes/{id}/exportar` (JSON estruturado) |
| Art. 18 VI | Eliminação | `POST /pacientes/{id}/anonimizar` (anonimização irreversível) |
| Art. 46 | Segurança | AES-256-GCM para CPF e dados médicos sensíveis |
| Art. 12 | Anonimização | Dados pessoais substituídos por `[ANONIMIZADO]` |
| Minimização | Mascaramento | CPF exibido como `***.***.***-XX` nas respostas da API |

## API de solicitações de peruca

Disponível no Swagger em `http://localhost:8080/swagger-ui.html` e no OpenAPI
em `/v3/api-docs`, tag **Solicitacoes**. Utilize **Authorize** com o access token
JWT. As três rotas exigem `ADMIN`, `MEDICO`, `ENFERMEIRO` ou `ATENDENTE`.

| Método | Rota | Função |
|---|---|---|
| GET | `/solicitacoes` | Lista paginada de ativas; `page` (base zero), `size`, `sort`, `busca` por nome/e-mail |
| GET | `/solicitacoes/{id}` | Paciente, contato, endereço, família, dados médicos, nome do solicitante e kits; CPF completo para a equipe autorizada |
| PATCH | `/solicitacoes/{id}/status` | Encerra pedido ativo com `{"status":"CONCLUIDA"}` ou `{"status":"CANCELADA"}` |

O ID da solicitação corresponde ao ID do paciente, mantendo o fluxo de cadastro
existente, sem duplicar pedidos. Um cadastro novo inicia `ATIVA`. Registros
legados com coluna nula também são interpretados como `ATIVA`, inclusive
cadastros incompletos/aguardando laudo. Pedidos anonimizados são excluídos da
lista, dos detalhes e das alterações.

Somente `GET /solicitacoes/{id}` retorna o CPF completo para o modal da equipe.
As demais rotas de pacientes continuam mascarando o CPF. A criptografia no
banco e a auditoria de acesso permanecem habilitadas.

A coluna JPA `Paciente.status_solicitacao` deve existir no schema provisionado;
o backend usa `ddl-auto=validate` e não altera o banco durante a inicialização.
Para ambientes que controlam DDL externamente, aplique antes do deploy:
`ALTER TABLE paciente ADD COLUMN status_solicitacao VARCHAR(20) NULL;`
(ajuste a capitalização da tabela ao banco). Não é necessário alterar valores
legados: `NULL` significa ativo por compatibilidade.

O encerramento é transacional com bloqueio de escrita: o primeiro atendimento
persiste a decisão; um segundo retorna `409`. Não permite reabertura nem exclui
pacientes ou altera entregas. Respostas: `400` status inválido, `401` sem JWT,
`403` perfil sem permissão, `404` pedido inexistente/anonimizado, `409` encerrado.
Acesso e alterações participam da auditoria existente de pacientes.
A lista retorna `content`, `totalElements`, `totalPages` e demais metadados de
`Page`; a ordem padrão é `dtPedido DESC, id DESC`.

Testes específicos: `.\mvnw.cmd "-Dtest=Solicitacao*Test" test`.

## Revisao humana de documentos

Nao ha OCR, comparacao automatica ou dependencia de Tesseract.

- `POST /arquivos`: multipart `arquivo`, `tipo` e `pacienteId`.
  Revisores (`ADMIN`, `MEDICO` ou `ENFERMEIRO`) podem enviar arquivos
  diretamente. O fluxo público deve enviar o `X-Application-Token` retornado
  pelo formulário inicial; esse token é temporário, vinculado ao paciente e
  limitado a `RELATORIO_MEDICO`. Upload público sem contexto é rejeitado.
- `GET /arquivos/pendentes-validacao`: fila para os mesmos perfis revisores,
  excluindo `FOTO_CABELO`. Arquivos legados sem status sao pendentes.
- `PATCH /arquivos/{id}/validacao`: `decisao` (`APROVADO` ou `REJEITADO`) e
  `observacao` (ate 2000 caracteres; obrigatoria para rejeicao).
  Persiste decisao, data e usuario revisor. Segunda revisao retorna `409`.
- `GET /arquivos/{id}` retorna status, observacao, `dataUpload` e `validadoEm`.
  Downloads continuam autenticados; documentos nao aprovados so podem ser
  baixados pela equipe revisora.
- Criar ou atualizar dados medicos exige laudo aprovado e associado ao mesmo
  paciente. Um laudo pendente/rejeitado retorna `409`, sem persistir alteracoes.

Uploads permitem PDF, JPG/JPEG e PNG, ate 10 MiB. O servidor normaliza nomes,
confere extensao/MIME, inicio/fim do PDF e decodificacao de imagens. Nao ha
antivirus nem inspecao estrutural completa de PDFs. A revisao humana nao e
substituto dessas camadas.

Em bancos antigos, as colunas de revisao e `paciente_id` em `Arquivo` precisam
ser provisionadas por uma alteracao de schema controlada. Ambientes com
migracoes externas devem provisionar essas colunas
antes de executar a nova versao. Arquivos antigos sem associacao nao podem ser
usados em novos dados medicos: envie o laudo com `pacienteId` pela equipe.

O campo `complemento` de endereco e exposto no contrato da API e persistido pela
aplicacao. Em ambientes que controlam DDL externamente, a migracao da coluna
correspondente deve ser planejada e validada antes do deploy; esta alteracao nao
executa DDL em banco compartilhado.

## Pré-requisitos

- JDK 21+
- MySQL 8+ rodando em `localhost:3306`
- Database `amor_em_mechas` criada

## Configuração

### Variáveis de Ambiente (Produção)

| Variável | Descrição | Valor padrão (dev) |
|---|---|---|
| `DB_HOST` | Host do banco MariaDB/MySQL | `127.0.0.1` |
| `DB_PORT` | Porta do banco MariaDB/MySQL | `3306` |
| `DB_NAME` | Nome do database | `amor_em_mechas` |
| `DB_USER` | Usuário do banco | _(obrigatório)_ |
| `DB_USERNAME` | Nome legado aceito quando `DB_USER` não está definido | _(sem padrão)_ |
| `DB_PASSWORD` | Senha do banco MySQL | obrigatória |
| `SPRING_DATASOURCE_URL` | URL JDBC completa; tem precedência sobre `DB_*` | _(opcional)_ |
| `SPRING_DATASOURCE_USERNAME` | Usuário JDBC; tem precedência sobre `DB_USER`/`DB_USERNAME` | _(opcional)_ |
| `SPRING_DATASOURCE_PASSWORD` | Senha JDBC; tem precedência sobre `DB_PASSWORD` | _(opcional)_ |
| `JWT_SECRET` | Chave secreta JWT (mín. 256 bits) | obrigatória |
| `PHI_ENCRYPTION_KEY` | Chave AES-256 para dados médicos | obrigatória |
| `CORS_ORIGINS` | Origens permitidas (separadas por vírgula) | `http://localhost:3000,http://localhost:4200` |
| `UPLOAD_DIR` | Diretório para armazenamento de arquivos | `uploads` |

O driver atualmente utilizado é `mysql-connector-j`. A URL JDBC é montada a
partir de `DB_HOST`, `DB_PORT` e `DB_NAME`, mantendo `serverTimezone` de
São Paulo e `utf8`. Variáveis `SPRING_DATASOURCE_*` continuam funcionando e
sobrescrevem as propriedades equivalentes.

O Hibernate usa `ddl-auto=validate`: a inicialização valida o schema existente,
mas não cria, altera ou remove tabelas. O database deve ser provisionado
externamente.

### Gerar chaves para produção

```bash
# JWT Secret (256 bits)
openssl rand -base64 32

# PHI Encryption Key (AES-256)
openssl rand -base64 32
```

### Teste local com túnel SSH

Com o MariaDB privado na VM Azure, abra o túnel em um terminal separado (sem
colocar a senha em arquivos do projeto):

```powershell
ssh -N -L 13306:127.0.0.1:3306 <usuario>@<VM_AZURE>
```

No terminal usado para iniciar o Spring, defina as variáveis da sessão:

```powershell
$env:DB_HOST = "127.0.0.1"
$env:DB_PORT = "13306"
$env:DB_NAME = "amor_em_mechas"
$env:DB_USER = "amor_app"
$env:DB_PASSWORD = "<SECRET_LOCAL_NAO_VERSIONADO>"
```

Depois, inicie sem o perfil `dev` para usar o MariaDB:

```powershell
.\mvnw.cmd spring-boot:run
```

O perfil `dev` continua destinado aos testes locais com H2. Não é necessário
alterar código para usar o endereço futuro da VNet: basta definir `DB_HOST`,
`DB_PORT` e os demais segredos no ambiente de execução.

## Executar

```bash
# Compilar
./mvnw clean compile

# Rodar testes (101 testes)
./mvnw test

# Iniciar aplicação
./mvnw spring-boot:run
```

A aplicação inicia em `http://localhost:8080`.

### Fluxo recomendado para desenvolvedores (Windows)

Os scripts abaixo automatizam a inicialização local e a verificação dos principais endpoints. Execute o PowerShell a partir da pasta `api-para-formulario`:

```powershell
# Subir a API com H2 em memoria e executar os testes antes
.\scripts\start-dev.ps1

# Subir em outra porta, sem repetir os testes
.\scripts\start-dev.ps1 -Port 8081 -SkipTests

# Em outro terminal, testar a API em execucao
.\scripts\smoke-test.ps1

# Testar uma instancia em outra porta
.\scripts\smoke-test.ps1 -BaseUrl http://localhost:8081
```

O script `start-dev.ps1` usa o perfil `dev`, portanto não exige MySQL: os dados ficam em um banco H2 temporário e são perdidos ao encerrar a aplicação. Ele verifica o JDK, executa `mvnw.cmd test` e só inicia a API se os testes passarem. Use `-SkipTests` apenas quando a aplicação já tiver sido validada.

O script `smoke-test.ps1` testa health check, Swagger, OpenAPI, login inválido, geração de token de desenvolvimento, formulário público, endpoints protegidos e autorização por role. No final, também imprime todos os paths publicados pelo OpenAPI.

Caso o PowerShell bloqueie scripts locais, permita somente nesta sessão:

```powershell
Set-ExecutionPolicy -Scope Process -ExecutionPolicy Bypass
```

### Dashboard MVP e Token Lab

Com a API em execução, sirva o dashboard em uma origem permitida pelo CORS:

```bash
cd dashboard-mvp
python -m http.server 3000
```

Acesse `http://localhost:3000` e abra a aba **Token Lab**. Com o perfil `dev`, a tela usa `/auth/dev-token`, que assina o JWT sem consultar o MySQL, exibe a validade do token e permite testar uma rota protegida com o header `Authorization: Bearer`. Ela é destinada somente a desenvolvimento e não deve receber credenciais reais.

Para executar sem MySQL, inicie a API com o perfil H2 em memória:

```bash
./mvnw spring-boot:run "-Dspring-boot.run.profiles=dev"
```

## Documentação da API

Swagger UI disponível em: `http://localhost:8080/swagger-ui.html`

O Swagger permite filtrar por tag, consultar schemas dos DTOs e executar requisições. A especificação OpenAPI em JSON fica disponível em `http://localhost:8080/v3/api-docs` e pode ser importada no Postman, Insomnia ou outras ferramentas.

### Mapa rápido de endpoints

| Grupo | Base | Acesso |
|---|---|---|
| Autenticação | `/auth` | Público; `/auth/registro-admin` exige ADMIN |
| Formulário inicial | `/formulario-solicitacao-peruca` | `POST` público; retorna `applicationToken` temporário |
| Pacientes | `/pacientes` | JWT + ADMIN, MEDICO, ENFERMEIRO ou ATENDENTE |
| Dados médicos | `/dados-medicos` | JWT + ADMIN, MEDICO ou ENFERMEIRO |
| Avaliações | `/avaliacoes` | JWT + ADMIN, MEDICO ou ENFERMEIRO |
| Endereços | `/enderecos` | JWT |
| Arquivos | `/arquivos` | JWT |
| Solicitantes | `/solicitantes` | JWT |
| Madrinhas | `/madrinhas` | JWT |
| Kits do Amor | `/kits` | JWT |
| Filhos | `/filhos` | JWT |

O formulário inicial é público porque a usuária final ainda não possui conta. O cadastro administrativo em `/pacientes` continua protegido. O formulário exige consentimento LGPD, valida os campos e grava o CPF criptografado.

O campo `applicationToken` da resposta do formulario deve ser enviado no
header `X-Application-Token` ao fazer o upload do laudo medico, junto com o
`pacienteId` correspondente. O token nao concede autenticacao administrativa,
nao substitui o JWT de usuario e nao permite listar, consultar, baixar ou
revisar arquivos.

### Token de desenvolvimento

No perfil `dev`, gere um token sem consultar o banco:

```powershell
$body = @{ username = "dev.user"; role = "ROLE_ATENDENTE" } | ConvertTo-Json
$token = (Invoke-RestMethod `
	http://localhost:8080/auth/dev-token `
	-Method Post `
	-ContentType "application/json" `
	-Body $body).accessToken
```

Use-o nos endpoints protegidos:

```powershell
Invoke-RestMethod `
	http://localhost:8080/pacientes `
	-Headers @{ Authorization = "Bearer $token" }
```

O endpoint `/auth/dev-token` deve permanecer habilitado somente no perfil de desenvolvimento.

## Endpoints

### Autenticação (`/auth`)

| Método | Endpoint | Descrição | Acesso |
|---|---|---|---|
| POST | `/auth/login` | Login (retorna JWT) | Público |
| POST | `/auth/refresh` | Renovar access token | Público |
| POST | `/auth/registro` | Registro de usuário (roles básicas) | Público |
| POST | `/auth/registro-admin` | Registro com qualquer role | ADMIN |

### Pacientes (`/pacientes`)

| Método | Endpoint | Descrição | Acesso |
|---|---|---|---|
| GET | `/pacientes` | Listar pacientes (paginado, com filtros) | ADMIN, MEDICO, ENFERMEIRO, ATENDENTE |
| GET | `/pacientes/{id}` | Buscar por ID | ADMIN, MEDICO, ENFERMEIRO, ATENDENTE |
| POST | `/pacientes` | Cadastrar paciente | ADMIN, MEDICO, ENFERMEIRO, ATENDENTE |
| PUT | `/pacientes/{id}` | Atualizar paciente | ADMIN, MEDICO, ENFERMEIRO, ATENDENTE |
| DELETE | `/pacientes/{id}` | Remover paciente | ADMIN, MEDICO, ENFERMEIRO, ATENDENTE |
| POST | `/pacientes/{id}/anonimizar` | LGPD: Anonimizar dados | ADMIN |
| POST | `/pacientes/{id}/revogar-consentimento` | LGPD: Revogar consentimento | ADMIN, MEDICO, ENFERMEIRO, ATENDENTE |
| GET | `/pacientes/{id}/exportar` | LGPD: Exportar dados (portabilidade) | ADMIN, MEDICO, ENFERMEIRO, ATENDENTE |

**Filtros disponíveis:** `?estado=SP&dataInicio=2024-01-01&dataFim=2024-12-31&page=0&size=20`

### Formulario de solicitacao (`/formulario-solicitacao-peruca`)

| Método | Endpoint | Descrição | Acesso |
|---|---|---|---|
| POST | `/formulario-solicitacao-peruca` | Enviar uma nova solicitacao de peruca | Público |

O primeiro envio não exige conta ou token, pois é realizado pela usuária final. O endpoint utiliza a mesma validação, consentimento LGPD e criptografia de CPF do cadastro de pacientes. As operações internas continuam protegidas em `/pacientes/**`.

### Dados Médicos (`/dados-medicos`)

| Método | Endpoint | Descrição | Acesso |
|---|---|---|---|
| GET | `/dados-medicos` | Listar todos | ADMIN, MEDICO, ENFERMEIRO |
| GET | `/dados-medicos/{id}` | Buscar por ID | ADMIN, MEDICO, ENFERMEIRO |
| POST | `/dados-medicos` | Cadastrar | ADMIN, MEDICO, ENFERMEIRO |
| PUT | `/dados-medicos/{id}` | Atualizar | ADMIN, MEDICO, ENFERMEIRO |
| DELETE | `/dados-medicos/{id}` | Remover | ADMIN, MEDICO, ENFERMEIRO |

### Avaliações (`/avaliacoes`)

| Método | Endpoint | Descrição | Acesso |
|---|---|---|---|
| GET | `/avaliacoes` | Listar todas | ADMIN, MEDICO, ENFERMEIRO |
| GET | `/avaliacoes/{id}` | Buscar por ID | ADMIN, MEDICO, ENFERMEIRO |
| POST | `/avaliacoes` | Criar avaliação | ADMIN, MEDICO, ENFERMEIRO |
| DELETE | `/avaliacoes/{id}` | Remover | ADMIN, MEDICO, ENFERMEIRO |

### Endereços (`/enderecos`)

| Método | Endpoint | Descrição | Acesso |
|---|---|---|---|
| GET | `/enderecos` | Listar todos | Autenticado |
| GET | `/enderecos/{id}` | Buscar por ID | Autenticado |
| POST | `/enderecos` | Cadastrar | Autenticado |
| POST | `/enderecos/viacep` | Autocompletar via CEP | Autenticado |
| PUT | `/enderecos/{id}` | Atualizar | Autenticado |
| DELETE | `/enderecos/{id}` | Remover | Autenticado |

### Arquivos (`/arquivos`)

| Método | Endpoint | Descrição | Acesso |
|---|---|---|---|
| GET | `/arquivos` | Listar arquivos | Autenticado |
| GET | `/arquivos/{id}` | Buscar metadados | Autenticado |
| GET | `/arquivos/{id}/download` | Download do arquivo | Autenticado |
| POST | `/arquivos` | Upload (multipart) | Autenticado |

### Solicitantes (`/solicitantes`)

| Método | Endpoint | Descrição | Acesso |
|---|---|---|---|
| GET | `/solicitantes` | Listar todos | Autenticado |
| GET | `/solicitantes/{id}` | Buscar por ID | Autenticado |
| POST | `/solicitantes` | Cadastrar | Autenticado |
| PUT | `/solicitantes/{id}` | Atualizar | Autenticado |
| DELETE | `/solicitantes/{id}` | Remover | Autenticado |

### Madrinhas (`/madrinhas`)

| Método | Endpoint | Descrição | Acesso |
|---|---|---|---|
| GET | `/madrinhas` | Listar (filtro por status) | Autenticado |
| GET | `/madrinhas/{id}` | Buscar por ID | Autenticado |
| POST | `/madrinhas` | Cadastrar | Autenticado |
| PUT | `/madrinhas/{id}` | Atualizar | Autenticado |
| DELETE | `/madrinhas/{id}` | Remover | Autenticado |

### Kit do Amor (`/kits`)

| Método | Endpoint | Descrição | Acesso |
|---|---|---|---|
| GET | `/kits` | Listar (filtros opcionais) | Autenticado |
| GET | `/kits/{id}` | Buscar por ID | Autenticado |
| POST | `/kits` | Cadastrar | Autenticado |
| PUT | `/kits/{id}` | Atualizar | Autenticado |
| DELETE | `/kits/{id}` | Remover | Autenticado |

### Filhos (`/filhos`)

| Método | Endpoint | Descrição | Acesso |
|---|---|---|---|
| GET | `/filhos` | Listar todos | Autenticado |
| GET | `/filhos/{id}` | Buscar por ID | Autenticado |
| POST | `/filhos/unique` | Cadastrar um | Autenticado |
| POST | `/filhos/many` | Cadastrar múltiplos | Autenticado |
| PUT | `/filhos/unique/{id}` | Atualizar um | Autenticado |
| PUT | `/filhos/many` | Atualizar múltiplos | Autenticado |
| DELETE | `/filhos/{id}` | Remover | Autenticado |

## Estrutura do Projeto

```
src/main/java/br/com/amorEmMechas_Formulario/api/para/formulario/
├── config/          # SecurityConfig, SwaggerConfig
├── controller/      # REST Controllers
│   ├── arquivo/
│   ├── auth/
│   ├── avaliacao/
│   ├── dadosMedicos/
│   ├── endereco/
│   ├── filho/
│   ├── kitAmor/
│   ├── madrinha/
│   ├── paciente/
│   ├── solicitante/
│   └── formulario/
├── dto/             # Request/Response DTOs com validação
├── entity/          # Entidades JPA
├── exception/       # GlobalExceptionHandler
├── mapper/          # Entity ↔ DTO mappers
├── repository/      # Spring Data JPA Repositories
├── security/        # JWT, AES-256, Rate Limiting, Auditoria
│   ├── audit/       # AuditLog, AuditService, AuditFilter
│   ├── JwtTokenProvider.java
│   ├── JwtAuthenticationFilter.java
│   ├── JwtAuthenticationEntryPoint.java
│   ├── PhiEncryptionUtil.java
│   └── RateLimitFilter.java
└── service/         # Lógica de negócio
```

```
scripts/
├── start-dev.ps1    # Sobe a API com perfil dev e H2
└── smoke-test.ps1   # Testa endpoints e lista o OpenAPI
```

## Testes

```bash
./mvnw test
```

- **101 testes** (unitários + integração)
- Testes unitários de serviço (Mockito)
- Testes de integração de segurança (JWT, roles, RBAC)
- Testes de validação de DTOs
- Banco H2 em memória para testes (perfil `test`)

## Monitoramento

Actuator endpoints disponíveis:

- `GET /actuator/health` — Status da aplicação (público)
- `GET /actuator/info` — Informações da aplicação (autenticado)
