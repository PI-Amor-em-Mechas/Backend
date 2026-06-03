# Evolution Bot (Java 21)

Estrutura base para bot de atendimento WhatsApp com Evolution API, Spring Boot e MySQL.

## Stack

- Java 21
- Spring Boot 3.3
- Spring Web + Spring Data JPA
- Flyway
- MySQL 8

## Estrutura

- `src/main/java/com/evolutionbot/config`: configurações e properties
- `src/main/java/com/evolutionbot/domain`: entidades JPA
- `src/main/java/com/evolutionbot/repository`: repositórios
- `src/main/java/com/evolutionbot/service`: regras de atendimento e integração
- `src/main/java/com/evolutionbot/web`: controllers REST
- `src/main/resources/db/migration`: migrations Flyway

## Subir MySQL

```bash
docker compose up -d
```

## Variáveis principais

- `DB_URL`
- `DB_USERNAME`
- `DB_PASSWORD`
- `EVOLUTION_BASE_URL`
- `EVOLUTION_API_KEY`
- `EVOLUTION_INSTANCE`
- `EVOLUTION_WEBHOOK_TOKEN`

## Rodar aplicação

```bash
mvn spring-boot:run
```

## Endpoints iniciais

- `POST /webhooks/evolution` - recebe eventos da Evolution API (header `x-webhook-token`)
- `POST /api/attendance/handoff/{phoneNumber}` - muda para atendimento humano
- `POST /api/attendance/resume/{phoneNumber}` - devolve para bot
- `POST /api/attendance/send` - envio ativo de mensagem

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
