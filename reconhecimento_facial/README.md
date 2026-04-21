# Face Attendance - MediaPipe + OpenCV LBPH

Aplicacao de ponto (clock-in/clock-out) com reconhecimento facial em tempo real.

## Visao geral

Este projeto implementa:

- Cadastro de colaboradores (`id` + `name`) e coleta de amostras de rosto via webcam.
- Deteccao de face com MediaPipe Tasks Face Detector (`mediapipe.tasks.python.vision`).
- Reconhecimento facial com OpenCV LBPH (`cv2.face`).
- Registro de ponto em SQLite com tipo `IN/OUT`, timestamp, confianca e imagem opcional.
- Regra anti-duplicacao por janela de tempo (cooldown).

## Estrutura

```
.
├── requirements.txt
├── README.md
├── data
│   ├── dataset/<employee_id>/*.png
│   ├── models/            (lbph_model.yml, labels.json)
│   ├── punch_images/
│   ├── exports/
│   ├── secret_key         (gerado localmente, nao versionar)
│   ├── attendance.db
│   └── face_detector.task
└── src
     ├── __init__.py
     ├── app.py            (CLI: python -m src.app)
     ├── web_app.py        (factory + SocketIO: python -m src.web_app)
     ├── config.py
     ├── db.py
     ├── security.py       (auth/profile decorators)
     ├── utils.py
     ├── train.py
     ├── recognize.py      (loop OpenCV desktop)
     ├── capture.py        (CLI captura de amostras)
     ├── services/
     │   ├── frames.py       (encode/decode/reference image)
     │   ├── lgpd.py         (consent, retention, export, erase, audit)
     │   ├── model_cache.py  (singleton LBPH + detector + locks)
     │   └── punch_rules.py  (IN/OUT por dia, cooldown)
     ├── routes/
     │   ├── auth.py         (/, /login, /set-profile, /logout, /me)
     │   ├── employees.py    (/employees, /register-person, /train-model)
     │   ├── recognition.py  (/recognize, /recognize-frame, /confirm)
     │   ├── voice_phrases.py(/voice-phrases/*)
     │   └── lgpd.py         (/lgpd/privacy-notice, /lgpd/export, /lgpd/erase, /lgpd/retention, /lgpd/audit)
     ├── voice/              (Vosk: engine, state, training)
     ├── static/
     └── templates/
```

## Requisitos

- Python 3.10+
- Webcam local
- CPU (sem GPU obrigatoria)

## Instalacao

```bash
python -m venv .venv
```

Windows (PowerShell):

```bash
.venv\Scripts\Activate.ps1
```

Linux/macOS:

```bash
source .venv/bin/activate
```

Instale dependencias:

```bash
pip install -r requirements.txt
```

## Modelo do Face Detector (MediaPipe)

O app usa `mediapipe.tasks.python.vision.FaceDetector` e precisa do arquivo local `data/face_detector.task`.

- O projeto tenta baixar automaticamente na primeira execucao.
- URL configurada em `src/config.py` (`FACE_DETECTOR_MODEL_URL`).
- Se o download automatico falhar, baixe manualmente um modelo compatovel do Face Detector e salve em:

```text
data/face_detector.task
```

## Como executar

Fluxo recomendado:

1. Cadastrar colaborador e coletar amostras.
2. Treinar modelo LBPH.
3. Rodar reconhecimento para bater ponto.
4. Consultar registros.

Inicie o menu principal:

```bash
python -m src.app
```

Menu:

- `1` Cadastrar colaborador e capturar amostras.
- `2` Treinar modelo LBPH.
- `3` Reconhecer e bater ponto.
- `4` Listar colaboradores.
- `5` Listar registros de ponto.

## Interface web para reconhecimento

Agora tambem existe uma interface HTML amigavel para reconhecimento com confirmacao:

- Botao `Iniciar Reconhecimento` para capturar pela webcam.
- Exibicao da foto detectada e nome da pessoa reconhecida.
- Solicitacao de confirmacao antes de salvar o ponto.

Executar servidor web:

```bash
python -m src.web_app
```

Depois abra no navegador:

```text
http://127.0.0.1:5000
```

## Pipeline tecnico

1. Webcam -> frame BGR (OpenCV).
2. MediaPipe Face Detector -> bounding box + keypoints.
3. Seleciona maior face (caso multipla deteccao).
4. Crop do rosto -> preprocessamento (`gray`, `resize 200x200`, `equalizeHist` opcional).
5. LBPH `predict` -> label + confidence.
6. Decisao:
	- `confidence <= threshold` -> conhecido.
	- `confidence > threshold` -> desconhecido.
7. Registro no SQLite:
	- `employee_id`, `ts`, `type` (`IN/OUT`), `confidence`, `image_path` opcional.

## Banco SQLite

Arquivo: `data/attendance.db`

Tabelas:

- `employees(id TEXT PK, name TEXT, created_at TEXT)`
- `punches(id INTEGER PK AUTOINCREMENT, employee_id TEXT FK, ts TEXT, type TEXT, confidence REAL, image_path TEXT NULL)`

## Regras de ponto

- Anti-duplicacao: nao registra novo ponto do mesmo colaborador dentro da janela configurada (`PUNCH_DUPLICATE_WINDOW_SECONDS`, default 60s).
- Tipo automatico: baseado na contagem de pontos **do dia atual** (fuso local).
  - Contagem par -> proximo e `IN`.
  - Contagem impar -> proximo e `OUT`.
- Sem intervencao manual: a regra e determinista e um novo dia sempre comeca com `IN`.

## Calibracao do confidence (LBPH)

No LBPH do OpenCV, o `confidence` e uma distancia (menor = mais parecido).

- Regra usada no projeto: `conhecido` quando `confidence <= LBPH_CONFIDENCE_THRESHOLD`.
- Valor inicial sugerido: `65.0` (em `src/config.py`).

Como calibrar:

1. Rode com usuarios reais e observe os valores exibidos no overlay.
2. Colete distribuicao de:
	- acertos (mesma pessoa) -> tende a menor distancia.
	- falsos positivos (pessoa errada) -> tende a maior distancia.
3. Ajuste o threshold para reduzir falso positivo sem perder muito recall.

## Qualidade e robustez

- Tratamento para camera indisponivel.
- Caso sem face detectada.
- Caso multiplas faces: pega a maior.
- Processamento a cada `N` frames para melhor FPS (`PROCESS_EVERY_N_FRAMES`).
- Logs com `logging`.

## LGPD e seguranca (importante)

Biometria facial e dado pessoal sensivel. Este projeto ja inclui:

- **Consentimento explicito** por colaborador (tabela `consents`), com versao
  corrente em `config.CONSENT_VERSION`. O cadastro pelo web exige marcar o
  checkbox de consentimento; o CLI exige confirmacao textual.
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
