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
│   ├── dataset
│   │   └── <employee_id>/*.png
│   ├── models
│   │   ├── lbph_model.yml
│   │   └── labels.json
│   ├── punch_images
│   ├── attendance.db
│   └── face_detector.task
└── src
	 ├── app.py
	 ├── capture.py
	 ├── train.py
	 ├── recognize.py
	 ├── db.py
	 ├── config.py
	 └── utils.py
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
python src/app.py
```

Menu:

- `1` Cadastrar colaborador e capturar amostras.
- `2` Treinar modelo LBPH.
- `3` Reconhecer e bater ponto.
- `4` Listar colaboradores.
- `5` Listar registros de ponto.

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
- Tipo automatico:
  - Se ultimo ponto for `IN`, proximo vira `OUT`.
  - Caso contrario, vira `IN`.
- Durante reconhecimento, teclas opcionais:
  - `i` forca proximo registro para `IN`.
  - `o` forca proximo registro para `OUT`.
  - `q` encerra.

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

Biometria facial e dado pessoal sensivel.

Recomendacoes minimas:

- Obter consentimento explicito dos colaboradores.
- Definir base legal, finalidade e prazo de retencao.
- Restringir acesso ao banco e imagens.
- Criptografar dados em repouso e em transito.
- Evitar armazenar imagem bruta quando nao necessario.
- Preferir representacoes vetoriais/embeddings com protecao adequada.
- Nao enviar imagens para internet sem necessidade e sem controles.

Este projeto roda localmente e nao envia imagens para servicos externos por padrao.

## Comandos diretos (opcional)

Tambem e possivel executar modulos separadamente:

```bash
python src/train.py
python src/recognize.py
```

## Observacoes

- `opencv-contrib-python` e obrigatorio para `cv2.face`.
- Em alguns ambientes, instalar `opencv-python` e `opencv-contrib-python` juntos pode gerar conflito de wheels. Se ocorrer, mantenha apenas `opencv-contrib-python` na venv.
