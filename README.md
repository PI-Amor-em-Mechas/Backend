# Backend - Amor em Mechas

Repositório com dois projetos backend do sistema Amor em Mechas:

- API REST para formulário de pacientes (Java + Spring Boot + JPA).
- Módulo de reconhecimento facial para registro de ponto (Python + OpenCV + MediaPipe).

## Estrutura do repositório

```text
Backend/
├── api_rest/
│   └── api-para-formulario/
└── reconhecimento_facial/
```

## Tecnologias

### API REST

- Java 21
- Spring Boot
- Spring Data JPA
- Bean Validation
- Maven
- MySQL

### Reconhecimento Facial

- Python 3.10+
- OpenCV + OpenCV Contrib
- MediaPipe
- NumPy
- SQLite (registro local de ponto)

## Pre-requisitos

- Git
- Java 21
- Maven (ou usar `mvnw` do projeto)
- Python 3.10+
- MySQL rodando localmente na porta padrao `3306`
- Webcam (para o modulo de reconhecimento facial)

## 1) Como executar a API REST

Diretorio do projeto:

```bash
cd api_rest/api-para-formulario
```

### Banco MySQL esperado

A aplicacao esta configurada para:

- host: `localhost`
- porta: `3306`
- database: `amor_em_mechas`
- usuario: `root`
- senha: vazia

Config atual em `src/main/resources/application.properties`:

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/amor_em_mechas
spring.datasource.username=root
spring.datasource.password=
spring.jpa.hibernate.ddl-auto=update
```

### Criar o banco

Execute no MySQL:

```sql
CREATE DATABASE IF NOT EXISTS amor_em_mechas;
```

### Dependencia do driver MySQL

Se a API falhar ao iniciar por falta de driver JDBC, adicione no `pom.xml`:

```xml
<dependency>
    <groupId>com.mysql</groupId>
    <artifactId>mysql-connector-j</artifactId>
    <scope>runtime</scope>
</dependency>
```

### Subir a API

Com Maven Wrapper:

```bash
./mvnw spring-boot:run
```

No Windows (PowerShell/CMD):

```bash
mvnw.cmd spring-boot:run
```

A API sobe em:

```text
http://localhost:8080
```

## 2) Como executar o reconhecimento facial

Diretorio do projeto:

```bash
cd reconhecimento_facial
```

### Criar ambiente virtual e instalar dependencias

```bash
python -m venv .venv
```

Windows (PowerShell):

```bash
.venv\Scripts\Activate.ps1
pip install -r requirements.txt
```

Linux/macOS:

```bash
source .venv/bin/activate
pip install -r requirements.txt
```

### Executar aplicacao

```bash
python src/app.py
```

## Fluxo recomendado do reconhecimento facial

1. Cadastrar colaborador e coletar amostras.
2. Treinar o modelo LBPH.
3. Rodar reconhecimento para bater ponto.
4. Consultar os registros.

## Problemas comuns

### Porta 8080 ocupada

- Pare o processo anterior da API.
- Ou altere `server.port` no `application.properties`.

### Erro de conexao no MySQL

- Verifique se o MySQL esta ativo em `localhost:3306`.
- Verifique usuario e senha no `application.properties`.
- Confirme se o banco `amor_em_mechas` existe.

### Erro no modulo Python por modelo do detector

- O arquivo `data/face_detector.task` precisa existir.
- Se o download automatico falhar, faca download manual e salve nesse caminho.

## Observacoes

- Este repositório e um monorepo com dois projetos independentes.
- O README interno de `reconhecimento_facial` traz detalhes adicionais do modulo Python.
