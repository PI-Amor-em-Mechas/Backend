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

## Segurança (HL7 Compliance)

A API implementa o protocolo de segurança HL7 para proteção de dados médicos:

- **Autenticação JWT** — Access token (15 min) + Refresh token (8h)
- **Criptografia AES-256-GCM** — Dados sensíveis (CPF, dados médicos) criptografados em repouso
- **RBAC** — Controle de acesso baseado em roles (ADMIN, MEDICO, ENFERMEIRO, ATENDENTE, USER)
- **Rate Limiting** — 10 requisições/min por IP no endpoint de login
- **Auditoria** — Log completo de eventos de autenticação e acesso a dados sensíveis
- **CORS centralizado** — Configurado no SecurityConfig (sem `@CrossOrigin` nos controllers)
- **Headers de segurança** — HSTS, CSP, X-Content-Type-Options

## LGPD (Lei Geral de Proteção de Dados)

A API está em conformidade com a Lei 13.709/2018:

| Artigo | Requisito | Implementação |
|---|---|---|
| Art. 7/8 | Consentimento | Campo `consentimentoLgpd` obrigatório no cadastro, com timestamp |
| Art. 8 §5 | Revogação | `POST /pacientes/{id}/revogar-consentimento` |
| Art. 18 V | Portabilidade | `GET /pacientes/{id}/exportar` (JSON estruturado) |
| Art. 18 VI | Eliminação | `POST /pacientes/{id}/anonimizar` (anonimização irreversível) |
| Art. 46 | Segurança | AES-256-GCM para CPF e dados médicos sensíveis |
| Art. 12 | Anonimização | Dados pessoais substituídos por `[ANONIMIZADO]` |
| Minimização | Mascaramento | CPF exibido como `***.***.***-XX` nas respostas da API |

## Pré-requisitos

- JDK 21+
- MySQL 8+ rodando em `localhost:3306`
- Database `amor_em_mechas` criada

## Configuração

### Variáveis de Ambiente (Produção)

| Variável | Descrição | Valor padrão (dev) |
|---|---|---|
| `DB_PASSWORD` | Senha do banco MySQL | `123456` |
| `JWT_SECRET` | Chave secreta JWT (mín. 256 bits) | Base64 embutido |
| `PHI_ENCRYPTION_KEY` | Chave AES-256 para dados médicos | Base64 embutido |
| `CORS_ORIGINS` | Origens permitidas (separadas por vírgula) | `http://localhost:3000,http://localhost:4200` |
| `UPLOAD_DIR` | Diretório para armazenamento de arquivos | `uploads` |

### Gerar chaves para produção

```bash
# JWT Secret (256 bits)
openssl rand -base64 32

# PHI Encryption Key (AES-256)
openssl rand -base64 32
```

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
| Formulário inicial | `/formulario-solicitacao-peruca` | `POST` público, sem token |
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
