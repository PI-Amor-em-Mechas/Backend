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

## Próximos passos

- Refinar parsing dos payloads conforme eventos habilitados na sua instância Evolution.
- Adicionar autenticação para endpoints internos (`/api/attendance/**`).
- Criar fila para envios e reprocessamento de falhas.
