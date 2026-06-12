# Amor em Mechas - API REST

API REST para gerenciamento do formulário de cadastro de pacientes do projeto **Amor em Mechas**, uma ONG que confecciona perucas para pacientes em tratamento oncológico.

## Tecnologias

| Tecnologia | Versão |
|---|---|
| Java | 17 |
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

- JDK 17+
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

## Documentação da API

Swagger UI disponível em: `http://localhost:8080/swagger-ui.html`

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
│   └── solicitante/
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
