# Face Attendance - YuNet + SFace (OpenCV)

Sistema de registro de ponto (clock-in/clock-out) com reconhecimento facial em tempo real, interface web, reconhecimento de voz offline e conformidade LGPD.

## Visao geral

O projeto implementa:

- Cadastro de colaboradores com coleta de amostras via webcam e armazenamento de embeddings faciais.
- Deteccao de face com **YuNet** (`cv2.FaceDetectorYN`) — modelo ONNX leve e CPU-friendly.
- Reconhecimento facial com **SFace** (`cv2.FaceRecognizerSF`) — embedding 128-D com similaridade de cosseno.
- Persistencia em **MySQL / MariaDB** (via PyMySQL) com schema em portugues — registro de ponto com tipo `IN/OUT`, timestamp, score de confianca e imagem opcional.
- Regra anti-duplicacao por janela de tempo (cooldown).
- Text-To-Speech (TTS) com **Piper** (neural, offline, pt-BR) ou pyttsx3 como fallback.
- Reconhecimento de voz offline com **Vosk**, com frases treinadas e palavra-chave `salvar`.
- **Biometria de voz (2o fator)** com **Resemblyzer** (GE2E / d-vector 256-D) verificando o falante em paralelo ao STT, antes de persistir o comando.
- Autenticacao por perfis (`default` e `admin`) com senha.
- Conformidade LGPD: consentimento, auditoria, exportacao e anonimizacao de dados.

## Estrutura

```
.
├── requirements.txt
├── README.md
├── data/
│   ├── schema.sql                    (schema MySQL — dominio + reconhecimento)
│   ├── schemaMdB.sql                 (schema MariaDB — mesmo dominio, idiomatico)
│   ├── dataset/<employee_id>/*.png   (amostras de rostos alinhados)
│   ├── models/
│   │   ├── face_detection_yunet_2023mar.onnx  (auto-download)
│   │   ├── face_recognition_sface_2021dec.onnx (auto-download)
│   │   ├── labels.json
│   │   └── lbph_model.yml
│   ├── piper/
│   │   ├── pt_BR-faber-medium.onnx
│   │   └── pt_BR-faber-medium.onnx.json
│   ├── vosk-model/                   (modelo Vosk pt-BR)
│   ├── punch_images/
│   ├── exports/
│   ├── secret_key                    (gerado localmente, nao versionar)
│   └── voice_phrases.json
└── src/
    ├── __init__.py
    ├── app.py            (CLI auxiliar: python -m src.app)
    ├── web_app.py        (factory Flask + SocketIO: python -m src.web_app)
    ├── config.py
    ├── db.py
    ├── security.py       (decoradores de perfil/auth)
    ├── utils.py
    ├── train.py
    ├── recognize.py      (loop OpenCV desktop, uso auxiliar)
    ├── capture.py        (captura CLI de amostras)
    ├── services/
    │   ├── face_engine.py  (YuNet detect + SFace embed + index cosine)
    │   ├── voice_engine.py (Resemblyzer encoder + verify_speaker + enroll)
    │   ├── frames.py       (encode/decode/imagem de referencia)
    │   ├── lgpd.py         (consentimento, retencao, export, erase, audit)
    │   ├── model_cache.py  (lock global de rebuild de embeddings)
    │   ├── punch_rules.py  (IN/OUT por dia, cooldown)
    │   └── tts.py          (Piper / pyttsx3)
    ├── routes/
    │   ├── auth.py          (/, /login, /set-profile, /logout, /me)
    │   ├── employees.py     (/employees, /register-person, /employees/update, /employees/delete)
    │   ├── recognition.py   (/recognition-window, /recognize, /recognize-frame, /confirm)
    │   ├── tts.py           (/tts/speak, /tts/info)
    │   ├── voice_phrases.py (/voice-phrases/*, export/import JSON)
    │   ├── voice_biometry.py(/voice-biometry/status, /enroll, /clear)
    │   └── lgpd.py          (/lgpd/privacy-notice, /lgpd/consent/*, /lgpd/export/*, /lgpd/erase/*, /lgpd/retention/*)
    ├── voice/
    │   ├── vosk_engine.py  (STT offline com Vosk)
    │   ├── state.py        (sessao de voz por conexao WebSocket)
    │   └── training.py     (gerenciamento de frases de treino)
    ├── static/
    └── templates/
```

## Requisitos

- Python 3.10+
- Webcam local
- CPU (sem GPU obrigatoria)
- **MySQL 8+** ou **MariaDB 10.4+** acessivel (local ou remoto)

## Banco de dados (MySQL / MariaDB)

O projeto usa **PyMySQL** como driver. Antes de subir a aplicacao, importe o
schema apropriado no seu servidor:

```bash
# MySQL
mysql -u <user> -p < data/schema.sql

# MariaDB (versao com CHECK constraints e ROW_FORMAT=DYNAMIC explicito)
mysql -u <user> -p < data/schemaMdB.sql
```

Ambos os schemas criam o database `amor_em_mechas` com o dominio de negocio
(pacientes, madrinhas, kits) + as tabelas do reconhecimento facial.
O `init_db()` apenas valida que as tabelas existem; ele nao cria nada.

Configure as credenciais via variaveis de ambiente:

| Variavel      | Padrao            | Descricao                       |
|---------------|-------------------|---------------------------------|
| `DB_HOST`     | `127.0.0.1`       | Host do MySQL/MariaDB           |
| `DB_PORT`     | `3306`            | Porta                           |
| `DB_USER`     | `root`            | Usuario                         |
| `DB_PASSWORD` | _(vazio)_         | Senha                           |
| `DB_NAME`     | `amor_em_mechas`  | Nome do database                |
| `DB_CHARSET`  | `utf8mb4`         | Charset da conexao              |

## Instalacao

```bash
python -m venv venv
```

Windows (PowerShell):

```bash
venv\Scripts\Activate.ps1
```

Linux/macOS:

```bash
source venv/bin/activate
```

Instale dependencias:

```bash
pip install -r requirements.txt
```

### PyTorch (CPU)

O `torch` (transitivo de `resemblyzer`) deve ser instalado via wheel CPU
oficial para evitar baixar a build com CUDA:

```bash
pip install torch --index-url https://download.pytorch.org/whl/cpu
```

### `webrtcvad` no Windows

O `resemblyzer` depende de `webrtcvad`, que compila a partir do fonte e exige
o **Microsoft C++ Build Tools**. Para evitar isso, o `requirements.txt` usa
`webrtcvad-wheels`, que fornece wheels pre-compiladas para Windows expondo o
mesmo modulo `webrtcvad`. Em Linux/macOS, qualquer das duas opcoes funciona.

## Modelos (auto-download)

Os modelos ONNX sao baixados automaticamente na primeira execucao:

| Modelo | Arquivo | Uso |
|--------|---------|-----|
| YuNet | `data/models/face_detection_yunet_2023mar.onnx` | Deteccao de face |
| SFace | `data/models/face_recognition_sface_2021dec.onnx` | Embedding facial |

Se o download automatico falhar, baixe manualmente de:
- YuNet: `https://github.com/opencv/opencv_zoo/tree/main/models/face_detection_yunet`
- SFace: `https://github.com/opencv/opencv_zoo/tree/main/models/face_recognition_sface`

## TTS (Text-To-Speech)

O sistema usa **Piper** como engine principal (TTS neural offline, pt-BR) e **pyttsx3** como fallback.

O modelo padrao incluido e `pt_BR-faber-medium`. Para usar outro:

```bash
# Exemplo via huggingface-cli
huggingface-cli download rhasspy/piper-voices \
    pt/pt_BR/faber/medium/pt_BR-faber-medium.onnx \
    pt/pt_BR/faber/medium/pt_BR-faber-medium.onnx.json \
    --local-dir data/piper
```

Variaveis de ambiente para TTS:

| Variavel | Padrao | Descricao |
|----------|--------|-----------|
| `TTS_ENGINE` | `auto` | `piper`, `pyttsx3` ou `auto` |
| `PIPER_MODELS_DIR` | `data/piper` | Diretorio com modelos .onnx |
| `PIPER_VOICE` | _(auto-detect)_ | Nome ou caminho do modelo .onnx |

## Reconhecimento de Voz (Vosk)

O sistema usa **Vosk** para STT offline via WebSocket (SocketIO). O modelo deve estar em `data/vosk-model/`.

- Frases treinadas sao gerenciadas pelo endpoint `/voice-phrases/`.
- A palavra-chave `salvar` ao final de uma frase confirma e persiste o comando de voz.
- Frases sao salvas na tabela `voice_commands` e adicionadas ao treino do reconhecedor.

## Biometria de voz (2o fator — Resemblyzer)

Apos a verificacao facial confirmar a identidade, a sessao de voz captura o
PCM16-LE @ 16 kHz em paralelo ao Vosk e calcula um d-vector (256-D) com
**Resemblyzer** (GE2E). Antes de persistir o `comando_voz`, o vetor e comparado
por similaridade de cosseno com o **centroid** dos voiceprints cadastrados do
colaborador. Se a similaridade fica abaixo de `VOICE_BIOMETRY_THRESHOLD`, o
comando e rejeitado (evento WebSocket `voice_rejected`).

### Enrollment

Cadastre **>= `VOICE_BIOMETRY_MIN_ENROLL_SAMPLES` amostras** (3 por padrao) por
colaborador, cada uma com pelo menos `VOICE_BIOMETRY_MIN_SECONDS` segundos
(1,5s por padrao; idealmente 5-10s).

#### Pela interface (recomendado)

Abra **`/register-window`** (perfil admin). Para cada colaborador da lista de
"Gerenciamento de Usuarios" sao exibidos:

- **Status:** `Voiceprints: N / minimo` com indicador `(pronto)` ou `(faltam amostras)`.
- **Botao "Gravar voz (5s)":** ativa o microfone, faz a contagem regressiva de
  5s e envia o PCM16-LE @ 16 kHz para `POST /voice-biometry/enroll`.
- **Botao "Limpar voz":** chama `POST /voice-biometry/clear` para remover todos
  os voiceprints do colaborador (uso administrativo / LGPD).

Repita o "Gravar voz" pelo menos 3 vezes por pessoa, em momentos/frases
diferentes, para um centroid mais robusto.

#### Pela API

```http
POST /voice-biometry/enroll
Content-Type: application/json

{
  "employee_id": "123",
  "pcm_base64": "<PCM16-LE mono @ 16 kHz em base64>"
}
```

Alternativamente, envie o array de bytes em `"pcm": [..]`. Status e contagem
de voiceprints:

```http
GET  /voice-biometry/status/<employee_id>
POST /voice-biometry/clear   # body: {"employee_id": "..."} (admin)
```

O endpoint `GET /employees` (admin) tambem devolve, para cada colaborador,
`voiceprint_count`, `voiceprint_min_samples`, `voiceprint_ready` e o bloco
global `voice_biometry` com a config corrente (`enforced`, `min_samples`,
`min_seconds`, `threshold`).

### Feedback na janela de reconhecimento

Em **`/recognition-window`**, depois do reconhecimento facial:

- O painel de voz exibe `Voiceprints: N` no status inicial. Se a biometria
  estiver `enforced=true` **e** o colaborador nao tiver voiceprints
  cadastrados, o aviso aparece em vermelho ("comandos serao rejeitados").
- Em `voice_saved` o status mostra `[biometria: score >= threshold]`.
- Em `voice_rejected` (novo evento WebSocket) o status mostra
  `Score=X, limiar=Y, (motivo)` em vermelho e o comando nao e gravado.

### Schema (tabela nova)

O schema (`data/schema.sql` e `data/schemaMdB.sql`) inclui `embedding_voz`
(256-D float32 serializado, FK CASCADE para `colaborador`). Aplique no banco
antes de subir a aplicacao.

### Variaveis de ambiente da biometria de voz

| Variavel                              | Padrao | Descricao                                                                |
|---------------------------------------|--------|--------------------------------------------------------------------------|
| `VOICE_BIOMETRY_ENFORCE`              | `true` | Se `false`, apenas audita o resultado e nao rejeita comandos             |
| `VOICE_BIOMETRY_THRESHOLD`            | `0.70` | Similaridade de cosseno minima para aceitar o falante                    |
| `VOICE_BIOMETRY_MIN_SECONDS`          | `1.5`  | Duracao minima de audio acumulado para tentar verificacao                |
| `VOICE_BIOMETRY_MAX_BUFFER_SECONDS`   | `30`   | Tamanho maximo do buffer PCM por sessao (janela deslizante)              |
| `VOICE_BIOMETRY_MIN_ENROLL_SAMPLES`   | `3`    | Quantidade minima de amostras para considerar o cadastro pronto          |
| `VOICE_BIOMETRY_ALLOW_UNENROLLED`     | `true` | Aceita comandos quando o colaborador nao tem voiceprint (rollout gradual)|

### Calibracao recomendada

1. Inicie com `VOICE_BIOMETRY_ENFORCE=false` e cadastre 3+ amostras por colaborador.
2. Acompanhe os logs em `log_auditoria` (acao `voice.biometry.verify`) para
   medir distribuicao de `score` por colaborador (mesma pessoa) vs.
   tentativas legitimas de outros (falsos positivos).
3. Ajuste `VOICE_BIOMETRY_THRESHOLD` para equilibrar FAR/FRR (tipico em pt-BR:
   0.65-0.75). Em seguida ative `VOICE_BIOMETRY_ENFORCE=true`.

### LGPD

Voiceprints sao **dado biometrico sensivel** (LGPD art. 5, II + art. 11). A
anonimizacao (`POST /lgpd/erase/<id>`) tambem remove `embedding_voz` em cascata,
e o consentimento existente cobre tambem o uso para autenticacao por voz.

## Como executar

Inicie o servidor web (modo principal):

```bash
python -m src.web_app
```

Abra no navegador:

```
http://127.0.0.1:5000
```

## Perfis de acesso

Na tela de login, selecione o perfil:

- **Padrao**: acesso ao reconhecimento facial e registro de ponto.
- **Admin**: acesso completo — cadastro de colaboradores, treinamento, LGPD.

A senha do perfil admin e configurada via variavel de ambiente:

```bash
ADMIN_PROFILE_PASSWORD=minha_senha_segura
```

Valor padrao (apenas desenvolvimento): `admin123`.

## Pipeline tecnico

1. Frame capturado pela webcam ou enviado via data URI pelo navegador.
2. **YuNet** detecta faces e retorna bounding box + 5 landmarks.
3. Face maior selecionada (caso multipla deteccao).
4. **SFace** alinha e extrai embedding 128-D.
5. Similaridade de cosseno contra matriz de embeddings armazenados no banco.
6. Decisao:
   - `score >= SFACE_COSINE_THRESHOLD` (0.363) -> conhecido.
   - `score < threshold` -> desconhecido.
7. Fluxo de confirmacao:
   - Resultado retornado ao frontend com token temporario (2 min).
   - Usuario confirma -> ponto registrado em `ponto` (MySQL/MariaDB).
   - Cooldown verificado antes de gerar token.

## Banco de dados — tabelas do reconhecimento facial

Nomes em portugues no schema; o `src/db.py` mantem aliases em ingles nos
`SELECT`s para preservar a interface usada pelas rotas e templates.

| Tabela              | Descricao                                                                 |
|---------------------|---------------------------------------------------------------------------|
| `colaborador`       | Colaboradoras (`id`, `nome`, `criado_em`, `anonimizado_em`)               |
| `ponto`             | Registros de ponto (`colaborador_id`, `data_hora`, `tipo IN/OUT`, `confianca`, `caminho_imagem`) |
| `embedding_facial`  | Embeddings SFace por colaboradora (BLOB `float32`, `dimensao` 128)        |
| `embedding_voz`     | Voiceprints Resemblyzer/GE2E (BLOB `float32`, `dimensao` 256) — 2o fator  |
| `comando_voz`       | Comandos de voz registrados (`colaborador_id`, `texto`, `criado_em`)      |
| `consentimento`     | Consentimento LGPD por colaboradora/versao                                |
| `log_auditoria`     | Trilha de auditoria de acoes sensiveis (`autor`, `acao`, `alvo`, `detalhes`) |

A tabela `madrinha` do dominio de negocio possui `colaborador_id` opcional
como FK para vinculo com o cadastro biometrico.

## Regras de ponto

- **Anti-duplicacao**: nao registra novo ponto dentro da janela configurada (`PUNCH_DUPLICATE_WINDOW_SECONDS`, default `60s`).
- **Tipo automatico** baseado na contagem de pontos do dia atual (fuso local):
  - Contagem par -> proximo e `IN`.
  - Contagem impar -> proximo e `OUT`.
- Um novo dia sempre comeca com `IN`.

## Calibracao do score (SFace)

O SFace usa similaridade de cosseno (maior = mais parecido, intervalo `[0, 1]`).

- Regra: `conhecido` quando `score >= SFACE_COSINE_THRESHOLD`.
- Valor padrao: `0.363` (configuravel via `SFACE_COSINE_THRESHOLD`).
- Tambem suporta distancia L2 (`SFACE_L2_THRESHOLD = 1.128`).

Como calibrar:

1. Rode com usuarios reais e observe os scores retornados pela API.
2. Colete distribuicao de acertos (mesma pessoa) e falsos positivos.
3. Ajuste o threshold via variavel de ambiente para equilibrar precisao e recall.

## Variaveis de ambiente

| Variavel | Padrao | Descricao |
|----------|--------|-----------|
| `ADMIN_PROFILE_PASSWORD` | `admin123` | Senha do perfil admin |
| `FLASK_SECRET_KEY` | _(arquivo `data/secret_key`)_ | Chave secreta Flask |
| `CAMERA_INDEX` | `0` | Indice da webcam |
| `SFACE_COSINE_THRESHOLD` | `0.363` | Threshold de reconhecimento (cosseno) |
| `SFACE_L2_THRESHOLD` | `1.128` | Threshold alternativo (L2) |
| `YUNET_SCORE_THRESHOLD` | `0.6` | Confianca minima de deteccao YuNet |
| `PUNCH_DUPLICATE_WINDOW_SECONDS` | `60` | Janela anti-duplicacao em segundos |
| `SAVE_PUNCH_IMAGE` | `false` | Salvar imagem no registro de ponto |
| `TTS_ENGINE` | `auto` | Engine TTS: `piper`, `pyttsx3` ou `auto` |
| `PIPER_MODELS_DIR` | `data/piper` | Diretorio de modelos Piper |
| `PIPER_VOICE` | _(auto-detect)_ | Modelo Piper a usar |
| `LOG_LEVEL` | `INFO` | Nivel de log |
| `VOICE_MAX_PHRASES` | `0` (ilimitado) | Limite de frases de voz armazenadas |
| `VOICE_BIOMETRY_ENFORCE` | `true` | Aplicar biometria de voz como 2o fator |
| `VOICE_BIOMETRY_THRESHOLD` | `0.70` | Cosseno minimo para aceitar o falante |
| `VOICE_BIOMETRY_MIN_SECONDS` | `1.5` | Duracao minima de audio para verificacao |
| `VOICE_BIOMETRY_MAX_BUFFER_SECONDS` | `30` | Janela do buffer PCM por sessao |
| `VOICE_BIOMETRY_MIN_ENROLL_SAMPLES` | `3` | Amostras minimas no enrollment |
| `VOICE_BIOMETRY_ALLOW_UNENROLLED` | `true` | Aceita comandos sem voiceprint cadastrado |
| `DB_HOST` | `127.0.0.1` | Host do MySQL/MariaDB |
| `DB_PORT` | `3306` | Porta do banco |
| `DB_USER` | `root` | Usuario do banco |
| `DB_PASSWORD` | _(vazio)_ | Senha do banco |
| `DB_NAME` | `amor_em_mechas` | Nome do database |
| `DB_CHARSET` | `utf8mb4` | Charset da conexao |

## LGPD e seguranca (importante)

Biometria facial e dado pessoal sensivel (Art. 5, II, LGPD). O projeto inclui:

- **Consentimento explicito** por colaborador (tabela `consents`), com versao
  corrente em `config.CONSENT_VERSION`. O cadastro pela interface web exige
  marcar o checkbox de consentimento.
- **Trilha de auditoria** (`audit_log`) para todas as acoes sensiveis (login,
  cadastro, exclusao, export, consentimento).
- **Exportacao de dados** (`/lgpd/export/<id>`) em JSON pelo perfil admin.
- **Anonimizacao** (`/lgpd/erase/<id>`): apaga embeddings, amostras e anonimiza
  o registro do colaborador no banco.
- **Politica de retencao** (`/lgpd/retention/apply`): remove automaticamente
  registros apos periodo configurado.
- Autenticacao por perfis com comparacao de senha via `secrets.compare_digest`
  (mitigacao de timing attacks).
- `SESSION_COOKIE_HTTPONLY=True` e `SESSION_COOKIE_SAMESITE=Lax`.
- `data/secret_key` gerado localmente com `secrets.token_bytes` — **nao versionar**.
- **Retencao automatica** de imagens de ponto (`data/punch_images/`) com base
  em `DATA_RETENTION_DAYS` (default 90). Aplicada no boot e a cada 6h em
  background, e disponivel via endpoint `/lgpd/retention/apply`.
- **Direito de acesso / portabilidade**: `GET /lgpd/export/<employee_id>`
  retorna um JSON com todos os dados daquele titular.
- **Direito ao esquecimento**: `POST /lgpd/erase/<employee_id>` remove
  amostras de dataset, imagens de ponto, comandos de voz, revoga consentimentos
  e **anonimiza** o cadastro mantendo apenas o historico de pontos sem PII
  (para cumprir obrigacoes contabeis/legais por `ANONYMIZED_PUNCHES_RETENTION_DAYS`).
- **Aviso de privacidade** publico: `GET /lgpd/privacy-notice`.
- **Log de auditoria** (`audit_log`) para acoes sensiveis (login, cadastro,
  treino, export, erase, revogacao). Consulta via `GET /lgpd/audit`.
- **Imagens de ponto desativadas por padrao** (`SAVE_PUNCH_IMAGE=false`),
  reduzindo o volume de biometria armazenada.
- **SECRET_KEY persistida** em `data/secret_key` (modo 0600 em SO compativel) —
  sessoes sobrevivem a reinicios. Pode ser sobrescrita por `FLASK_SECRET_KEY`.
- **Senha admin sem default**: defina `ADMIN_PROFILE_PASSWORD` via variavel de
  ambiente (comparacao resistente a timing attacks). Sem ela, o login admin
  fica desabilitado.

### Variaveis de ambiente relevantes

| Variavel                           | Default            | Descricao                              |
|------------------------------------|--------------------|----------------------------------------|
| `ADMIN_PROFILE_PASSWORD`           | _(vazio)_          | Senha do perfil admin                  |
| `FLASK_SECRET_KEY`                 | _arquivo_          | Chave da sessao Flask                  |
| `SAVE_PUNCH_IMAGE`                 | `false`            | Persistir imagens do ponto?            |
| `CONSENT_VERSION`                  | `1.0`              | Versao do termo vigente                |
| `DATA_RETENTION_DAYS`              | `90`               | Retencao de imagens de ponto           |
| `AUDIT_LOG_RETENTION_DAYS`         | `365`              | Retencao do log de auditoria           |
| `ANONYMIZED_PUNCHES_RETENTION_DAYS`| `1825` (5 anos)    | Retencao do historico anonimizado      |
| `LOCAL_TIMEZONE`                   | `America/Sao_Paulo`| Timezone para a regra IN/OUT diaria    |

## Regra IN/OUT automatica

O sistema determina `IN` ou `OUT` **automaticamente pela contagem de pontos do
colaborador no dia corrente** (fuso horario `LOCAL_TIMEZONE`):

- 0, 2, 4, ... pontos no dia -> proximo registro e `IN` (entrada).
- 1, 3, 5, ... pontos no dia -> proximo registro e `OUT` (saida).

Vantagens frente a alternancia baseada apenas no ultimo ponto:

- Um novo dia sempre comeca com `IN`, mesmo que o colaborador tenha esquecido
  de bater o par do dia anterior.
- Suporta multiplos pares no mesmo dia (entrada/saida/entrada/saida).
- O tipo e recomputado no momento do commit (`/confirm`) para evitar race
  conditions entre reconhecimento e confirmacao.

Teclas manuais `i`/`o` foram removidas do loop CLI — a regra e totalmente
automatica. Um cooldown (`PUNCH_DUPLICATE_WINDOW_SECONDS`) continua evitando
registros duplicados na mesma janela de segundos.

## Comandos diretos (opcional)

Execute modulos como parte do pacote `src`:

```bash
python -m src.app          # menu CLI
python -m src.web_app      # servidor web
python -m src.train        # treina somente
python -m src.recognize    # loop OpenCV somente
```

## Observacoes

- `opencv-contrib-python` e obrigatorio para `cv2.face`.
- Em alguns ambientes, instalar `opencv-python` e `opencv-contrib-python` juntos pode gerar conflito de wheels. Se ocorrer, mantenha apenas `opencv-contrib-python` na venv.
