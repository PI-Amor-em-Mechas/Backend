# Face Attendance - YuNet + SFace (OpenCV)

Sistema de registro de ponto (clock-in/clock-out) com reconhecimento facial em tempo real, interface web, reconhecimento de voz offline e conformidade LGPD.

## Visao geral

O projeto implementa:

- Cadastro de colaboradores com coleta de amostras via webcam e armazenamento de embeddings faciais.
- Deteccao de face com **YuNet** (`cv2.FaceDetectorYN`) — modelo ONNX leve e CPU-friendly.
- Reconhecimento facial com **SFace** (`cv2.FaceRecognizerSF`) — embedding 128-D com similaridade de cosseno.
- Registro de ponto em SQLite com tipo `IN/OUT`, timestamp, score de confianca e imagem opcional.
- Regra anti-duplicacao por janela de tempo (cooldown).
- Text-To-Speech (TTS) com **Piper** (neural, offline, pt-BR) ou pyttsx3 como fallback.
- Reconhecimento de voz offline com **Vosk**, com frases treinadas e palavra-chave `salvar`.
- Autenticacao por perfis (`default` e `admin`) com senha.
- Conformidade LGPD: consentimento, auditoria, exportacao e anonimizacao de dados.

## Estrutura

```
.
├── requirements.txt
├── README.md
├── data/
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
│   ├── attendance.db
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
   - Usuario confirma -> ponto registrado no SQLite.
   - Cooldown verificado antes de gerar token.

## Banco SQLite

Arquivo: `data/attendance.db`

Tabelas:

| Tabela | Descricao |
|--------|-----------|
| `employees` | Colaboradores (`id`, `name`, `created_at`, `anonymized_at`) |
| `punches` | Registros de ponto (`employee_id`, `ts`, `type IN/OUT`, `confidence`, `image_path`) |
| `face_embeddings` | Embeddings SFace por colaborador (BLOB `float32`, dim 128) |
| `voice_commands` | Comandos de voz registrados por colaborador |
| `consents` | Consentimento LGPD por colaborador/versao |
| `audit_log` | Trilha de auditoria de acoes sensiveis |

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
