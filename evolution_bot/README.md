# Evolution Bot (Java 21)

Bot de atendimento WhatsApp com Evolution API, Spring Boot e MySQL.

## Stack

- Java 21
- Spring Boot 3.3
- Spring Web + Spring Data JPA
- Flyway
- MySQL 8
- Docker / Docker Compose

## Estrutura

- `src/main/java/com/evolutionbot/config`: configurações e properties
- `src/main/java/com/evolutionbot/domain`: entidades JPA
- `src/main/java/com/evolutionbot/repository`: repositórios
- `src/main/java/com/evolutionbot/service`: regras de atendimento e integração
- `src/main/java/com/evolutionbot/web`: controllers REST
- `src/main/resources/db/migration`: migrations Flyway

---

## Deploy na VM Debian (com Docker)

### 1. Copiar o projeto para a VM

No Windows (PowerShell), a partir da pasta do projeto:

```powershell
scp -r . artur@201.13.60.130:~/evolution_bot/
```

### 2. Na VM, configurar o `.env`

```bash
cd ~/evolution_bot
cp .env.example .env
nano .env
```

Preencha com seus valores reais da Evolution API:

```env
DB_PASSWORD=SuaSenhaSegura123
EVOLUTION_BASE_URL=http://172.17.0.1:8081
EVOLUTION_API_KEY=sua-api-key-aqui
EVOLUTION_INSTANCE=sua-instancia
EVOLUTION_WEBHOOK_TOKEN=seu-token-webhook
```

> **Nota:** Se a Evolution API roda na mesma VM via Docker, use `http://172.17.0.1:PORTA` (gateway do Docker bridge). Se roda no host, use `http://localhost:PORTA`.

### 3. Subir com Docker Compose

```bash
cd ~/evolution_bot
docker compose up -d --build
```

Primeira execução: o build demora alguns minutos (baixa Maven + dependências).

### 4. Verificar logs

```bash
docker compose logs -f app
```

Quando aparecer `Started EvolutionBotApplication`, está pronto.

### 5. Testar

```bash
curl http://localhost:8080/api/attendance/send \
  -H "Content-Type: application/json" \
  -d '{"phoneNumber":"5511999999999","text":"teste"}'
```

---

## Configurar Webhook na Evolution API

Configure o webhook da sua instância Evolution API para:

```
URL: http://172.20.208.128:8080/webhooks/evolution
Header: x-webhook-token = <valor do EVOLUTION_WEBHOOK_TOKEN>
Eventos: messages.upsert
```

Se acessando externamente (fora da VM):
```
URL: http://201.13.60.130:8080/webhooks/evolution
```

---

## Endpoints

| Método | Rota | Descrição |
|--------|------|-----------|
| POST | `/webhooks/evolution` | Recebe eventos da Evolution API (header `x-webhook-token`) |
| POST | `/api/attendance/handoff/{phone}` | Transfere para atendimento humano |
| POST | `/api/attendance/resume/{phone}` | Devolve para bot |
| POST | `/api/attendance/send` | Envio ativo de mensagem |

---

## Desenvolvimento local (sem Docker)

Variáveis de ambiente ou `application.properties` apontando para o MySQL da VM:

```bash
export DB_URL=jdbc:mysql://201.13.60.130:3306/evolution_bot?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
export DB_USERNAME=root
export DB_PASSWORD=SuaSenhaSegura123
mvn spring-boot:run
```

## To Do - Implementação Real

### Fase 1 - Ambiente e execução local

- [ ] Instalar Docker Desktop e validar comando `docker --version`.
- [ ] Subir MySQL com `docker compose up -d`.
- [ ] Confirmar conexão na porta 3306.
- [ ] Subir aplicação com `mvn spring-boot:run` sem erros de Flyway/JPA.

### Fase 2 - Configuração de integração Evolution API

- [ ] Preencher `EVOLUTION_BASE_URL`.
- [ ] Preencher `EVOLUTION_API_KEY`.
- [ ] Preencher `EVOLUTION_INSTANCE`.
- [ ] Preencher `EVOLUTION_WEBHOOK_TOKEN`.
- [ ] Configurar webhook da Evolution para `POST /webhooks/evolution` com header `x-webhook-token`.
- [ ] Validar recebimento de eventos reais (mensagem inbound).

### Fase 3 - Fluxo de atendimento

- [ ] Revisar intenções de negócio (financeiro, suporte, comercial).
- [ ] Ajustar respostas automáticas para tom da marca.
- [ ] Definir critérios claros para `handoff` para humano.
- [ ] Validar fluxo completo: inbound -> resposta bot -> handoff -> resume.

### Fase 4 - Segurança e governança

- [ ] Proteger endpoints internos `/api/attendance/**` com autenticação.
- [ ] Implementar mascaramento de dados sensíveis em logs.
- [ ] Definir política de retenção de mensagens (LGPD).
- [ ] Separar variáveis por ambiente (dev, homolog, prod).

### Fase 5 - Resiliência e observabilidade

- [ ] Adicionar timeout e retry para chamadas da Evolution API.
- [ ] Melhorar chave de idempotência para evitar duplicidade de processamento.
- [ ] Criar logs estruturados com `conversationId` e `messageId`.
- [ ] Expor endpoint de healthcheck (`/actuator/health`).

### Fase 6 - Qualidade e testes

- [ ] Criar testes de integração para webhook.
- [ ] Criar testes para endpoints de handoff/resume.
- [ ] Criar teste para envio ativo (`/api/attendance/send`).
- [ ] Rodar build + testes automaticamente antes de merge.

### Fase 7 - Deploy e operação

- [ ] Definir pipeline de CI/CD.
- [ ] Publicar ambiente de produção com segredos seguros.
- [ ] Executar smoke test pós deploy.
- [ ] Configurar monitoramento de erro e disponibilidade.
