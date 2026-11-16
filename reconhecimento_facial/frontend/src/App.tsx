import { useEffect, useRef, useState } from "react";
import { io, type Socket } from "socket.io-client";
import { api, apiMessage } from "./api";
import type {
  Employee,
  PrivacyNotice,
  Profile,
  RecognitionResult,
  VoiceBiometryConfig,
} from "./types";

type View = "home" | "recognition" | "admin";

function createEmployeeId() {
  return `pessoa-${crypto.randomUUID()}`;
}

function getCameraStream(constraints: MediaStreamConstraints) {
  return new Promise<MediaStream>((resolve, reject) => {
    let settled = false;
    const timer = window.setTimeout(() => {
      if (settled) return;
      settled = true;
      reject(new DOMException("Tempo limite para acessar a câmera.", "TimeoutError"));
    }, 5000);
    navigator.mediaDevices.getUserMedia(constraints).then((stream) => {
      window.clearTimeout(timer);
      if (settled) stream.getTracks().forEach((track) => track.stop());
      else {
        settled = true;
        resolve(stream);
      }
    }).catch((error: unknown) => {
      window.clearTimeout(timer);
      if (!settled) {
        settled = true;
        reject(error);
      }
    });
  });
}

function Login({ onLogin }: { onLogin: (profile: Profile) => void }) {
  const [profile, setProfile] = useState<Profile>("default");
  const [password, setPassword] = useState("");
  const [message, setMessage] = useState("");
  const [busy, setBusy] = useState(false);

  async function submit(event: React.FormEvent) {
    event.preventDefault();
    if (profile === "admin" && !password) {
      setMessage("Informe a senha do administrador.");
      return;
    }
    setBusy(true);
    setMessage("");
    try {
      const { data } = await api.post("/set-profile", { profile, password });
      if (data.status !== "ok") throw new Error(data.message);
      onLogin(profile);
    } catch (error) {
      setMessage(apiMessage(error, "Falha ao autenticar."));
    } finally {
      setBusy(false);
    }
  }

  return (
    <main className="auth-shell">
      <section className="auth-card">
        <div className="brand-mark">AM</div>
        <p className="eyebrow">AMOR EM MECHAS</p>
        <h1>Controle de ponto com presença.</h1>
        <p className="muted">Entre para registrar a jornada com reconhecimento facial.</p>
        <div className="segmented" role="tablist">
          {(["default", "admin"] as Profile[]).map((item) => (
            <button
              className={profile === item ? "selected" : ""}
              key={item}
              onClick={() => setProfile(item)}
              type="button"
            >
              {item === "default" ? "Colaborador" : "Administrador"}
            </button>
          ))}
        </div>
        <form onSubmit={submit}>
          {profile === "admin" && (
            <label>
              Senha do administrador
              <input autoFocus type="password" value={password} onChange={(event) => setPassword(event.target.value)} />
            </label>
          )}
          <button className="button primary full" disabled={busy} type="submit">
            {busy ? "Entrando..." : "Entrar no sistema"}
          </button>
        </form>
        {message && <p className="feedback error">{message}</p>}
        <p className="auth-footnote">Dados biométricos tratados conforme as regras de privacidade da aplicação.</p>
      </section>
    </main>
  );
}

function CameraRecognition({ onBack }: { onBack: () => void }) {
  const videoRef = useRef<HTMLVideoElement>(null);
  const canvasRef = useRef<HTMLCanvasElement>(null);
  const streamRef = useRef<MediaStream | null>(null);
  const socketRef = useRef<Socket | null>(null);
  const audioContextRef = useRef<AudioContext | null>(null);
  const audioSourceRef = useRef<MediaStreamAudioSourceNode | null>(null);
  const audioProcessorRef = useRef<AudioWorkletNode | null>(null);
  const [result, setResult] = useState<RecognitionResult | null>(null);
  const [message, setMessage] = useState("Posicione o rosto dentro do quadro.");
  const [busy, setBusy] = useState(false);
  const [voiceText, setVoiceText] = useState("");
  const [listening, setListening] = useState(false);

  useEffect(() => {
    let active = true;
    navigator.mediaDevices.getUserMedia({ video: { width: 960, height: 540 }, audio: true })
      .then((stream) => {
        if (!active) return stream.getTracks().forEach((track) => track.stop());
        streamRef.current = stream;
        if (videoRef.current) videoRef.current.srcObject = stream;
      })
      .catch(() => setMessage("Permita o acesso à câmera e ao microfone para continuar."));
    return () => {
      active = false;
      streamRef.current?.getTracks().forEach((track) => track.stop());
      audioProcessorRef.current?.disconnect();
      audioSourceRef.current?.disconnect();
      void audioContextRef.current?.close();
      socketRef.current?.disconnect();
    };
  }, []);

  function captureFrame() {
    const video = videoRef.current;
    const canvas = canvasRef.current;
    if (!video || !canvas || video.readyState < 2) throw new Error("A câmera ainda não está pronta.");
    canvas.width = video.videoWidth;
    canvas.height = video.videoHeight;
    canvas.getContext("2d")?.drawImage(video, 0, 0);
    return canvas.toDataURL("image/jpeg", 0.86);
  }

  async function recognize() {
    setBusy(true);
    setResult(null);
    try {
      const { data } = await api.post<RecognitionResult>("/recognize-frame", { image: captureFrame() });
      setResult(data);
      setMessage(data.message);
    } catch (error) {
      setMessage(apiMessage(error, "Não foi possível processar a imagem."));
    } finally {
      setBusy(false);
    }
  }

  async function confirm(confirmValue: boolean) {
    if (!result?.token) return;
    setBusy(true);
    try {
      const { data } = await api.post<RecognitionResult>("/confirm", { token: result.token, confirm: confirmValue });
      setMessage(data.message);
      setResult(null);
    } catch (error) {
      setMessage(apiMessage(error, "Não foi possível confirmar o registro."));
    } finally {
      setBusy(false);
    }
  }

  async function toggleVoice() {
    if (listening) {
      socketRef.current?.emit("force_save");
      setListening(false);
      audioProcessorRef.current?.disconnect();
      audioSourceRef.current?.disconnect();
      await audioContextRef.current?.close();
      audioProcessorRef.current = null;
      audioSourceRef.current = null;
      audioContextRef.current = null;
      return;
    }
    if (!result?.token) {
      setMessage("Faça o reconhecimento facial antes de usar o comando de voz.");
      return;
    }
    const socket = io({ transports: ["websocket"] });
    socketRef.current = socket;
    socket.on("voice_partial", (data: { text?: string }) => setVoiceText(data.text ?? ""));
    socket.on("voice_saved", (data: { text?: string }) => {
      setVoiceText(data.text ?? "Comando salvo");
      setMessage("Comando de voz salvo com sucesso.");
    });
    socket.on("voice_error", (data: { message?: string }) => setMessage(data.message ?? "Erro no comando de voz."));
    socket.on("connect", () => {
      socket.emit("voice_auth", { token: result.token });
    });
    try {
      const audioContext = new AudioContext({ sampleRate: 16000 });
      if (audioContext.state === "suspended") await audioContext.resume();
      const source = audioContext.createMediaStreamSource(streamRef.current!);
      await audioContext.audioWorklet.addModule("/audio-processor.js");
      const processor = new AudioWorkletNode(audioContext, "pcm16-processor");
      processor.port.onmessage = (event: MessageEvent<ArrayBuffer>) => {
        if (socket.connected) socket.emit("audio_chunk", event.data);
      };
      source.connect(processor);
      audioContextRef.current = audioContext;
      audioSourceRef.current = source;
      audioProcessorRef.current = processor;
    } catch (error) {
      socket.disconnect();
      setMessage(apiMessage(error, "Não foi possível ativar o microfone."));
      return;
    }
    setListening(true);
    setMessage("Escutando. Fale o comando e termine com 'salvar'.");
  }

  return (
    <main className="app-shell narrow-shell">
      <header className="topbar"><button className="text-button" onClick={onBack}>← Voltar</button><span className="eyebrow">REGISTRO DE PONTO</span></header>
      <section className="recognition-layout">
        <div className="camera-stage">
          <video autoPlay muted playsInline ref={videoRef} />
          <div className="face-guide" />
          <canvas className="hidden" ref={canvasRef} />
        </div>
        <div className="recognition-info">
          <p className="eyebrow">VERIFICAÇÃO BIOMÉTRICA</p>
          <h1>Confirme sua presença.</h1>
          <p className="muted">A câmera identifica o colaborador e prepara o registro antes de salvar.</p>
          <div className="status-line"><span className="status-dot" />{message}</div>
          {result?.status === "recognized" && (
            <div className="result-box">
              <strong>{result.name}</strong>
              <span>{result.punch_type === "IN" ? "Entrada" : "Saída"} · confiança {(result.confidence ?? 0).toFixed(2)}</span>
              <div className="actions"><button className="button primary" disabled={busy} onClick={() => confirm(true)}>Confirmar ponto</button><button className="button ghost" disabled={busy} onClick={() => confirm(false)}>Cancelar</button></div>
            </div>
          )}
          <div className="actions main-actions"><button className="button primary" disabled={busy} onClick={recognize}>{busy ? "Analisando..." : "Reconhecer rosto"}</button><button className={listening ? "button recording" : "button ghost"} onClick={toggleVoice}>{listening ? "Parar comando de voz" : "Usar comando de voz"}</button></div>
          {voiceText && <p className="voice-preview">{voiceText}</p>}
        </div>
      </section>
    </main>
  );
}

function AdminPanel() {
  const [employees, setEmployees] = useState<Employee[]>([]);
  const [message, setMessage] = useState("");
  const [busy, setBusy] = useState(false);
  const [registerOpen, setRegisterOpen] = useState(false);

  async function loadEmployees() {
    try {
      const { data } = await api.get<{ employees: Employee[] }>("/employees");
      setEmployees(data.employees);
    } catch (error) {
      setMessage(apiMessage(error, "Não foi possível carregar os colaboradores."));
    }
  }
  useEffect(() => { void loadEmployees(); }, []);

  async function remove(id: string) {
    if (!window.confirm("Anonimizar e remover este colaborador?")) return;
    setBusy(true);
    try {
      await api.post("/employees/delete", { employee_id: id });
      setMessage("Colaborador removido e dados anonimizados.");
      await loadEmployees();
    } catch (error) {
      setMessage(apiMessage(error, "Não foi possível remover o colaborador."));
    } finally {
      setBusy(false);
    }
  }

  return <section className="admin-section">
    <div className="section-heading">
      <div><p className="eyebrow">ADMINISTRAÇÃO</p><h2>Colaboradores</h2></div>
      <div className="section-heading-actions">
        <span className="count-badge">{employees.length} cadastrados</span>
        <button className="button dark" type="button" onClick={() => setRegisterOpen(true)}>Cadastrar pessoa</button>
      </div>
    </div>
    {message && <p className="feedback">{message}</p>}
    <div className="employee-list">
      {employees.map((employee) => <EmployeeManagementRow key={employee.id} employee={employee} busy={busy} onRemove={() => void remove(employee.id)} />)}
      {employees.length === 0 && <p className="muted">Nenhum colaborador cadastrado.</p>}
    </div>
    {registerOpen && <RegistrationPanel onClose={() => { setRegisterOpen(false); void loadEmployees(); }} />}
  </section>;
}

async function getAudioStream(signal: AbortSignal) {
  return new Promise<MediaStream>((resolve, reject) => {
    let settled = false;
    const timer = window.setTimeout(() => {
      if (settled) return;
      settled = true;
      reject(new DOMException("Tempo limite para acessar o microfone.", "TimeoutError"));
    }, 5000);
    const cancel = () => {
      if (settled) return;
      settled = true;
      window.clearTimeout(timer);
      reject(new DOMException("Gravação cancelada.", "AbortError"));
    };
    if (signal.aborted) return cancel();
    signal.addEventListener("abort", cancel, { once: true });
    navigator.mediaDevices.getUserMedia({ audio: true, video: false }).then((stream) => {
      signal.removeEventListener("abort", cancel);
      window.clearTimeout(timer);
      if (settled) stream.getTracks().forEach((track) => track.stop());
      else {
        settled = true;
        resolve(stream);
      }
    }).catch((error: unknown) => {
      signal.removeEventListener("abort", cancel);
      window.clearTimeout(timer);
      if (!settled) {
        settled = true;
        reject(error);
      }
    });
  });
}

async function captureVoicePcm(seconds: number, signal: AbortSignal) {
  const audioContext = new AudioContext({ sampleRate: 16000 });
  let stream: MediaStream | null = null;
  try {
    const activeStream = await getAudioStream(signal);
    stream = activeStream;
    await audioContext.audioWorklet.addModule("/audio-processor.js");
    const source = audioContext.createMediaStreamSource(activeStream);
    const processor = new AudioWorkletNode(audioContext, "pcm16-processor");
    const chunks: Uint8Array[] = [];
    let totalBytes = 0;
    processor.port.onmessage = (event: MessageEvent<ArrayBuffer>) => {
      const chunk = new Uint8Array(event.data);
      chunks.push(chunk);
      totalBytes += chunk.byteLength;
    };
    source.connect(processor);
    await new Promise<void>((resolve, reject) => {
      const timer = window.setTimeout(resolve, seconds * 1000);
      const cancel = () => {
        window.clearTimeout(timer);
        reject(new DOMException("Gravação cancelada.", "AbortError"));
      };
      if (signal.aborted) return cancel();
      signal.addEventListener("abort", cancel, { once: true });
    });
    source.disconnect();
    processor.disconnect();
    const merged = new Uint8Array(totalBytes);
    let offset = 0;
    chunks.forEach((chunk) => { merged.set(chunk, offset); offset += chunk.byteLength; });
    let binary = "";
    for (let index = 0; index < merged.length; index += 0x8000) {
      binary += String.fromCharCode(...merged.subarray(index, index + 0x8000));
    }
    return btoa(binary);
  } finally {
    stream?.getTracks().forEach((track) => track.stop());
    await audioContext.close();
  }
}

function EmployeeManagementRow({ employee, busy, onRemove }: { employee: Employee; busy: boolean; onRemove: () => void }) {
  const [name, setName] = useState(employee.name);
  const [voiceBusy, setVoiceBusy] = useState(false);
  const [voiceStatus, setVoiceStatus] = useState(`Biometria de voz: grave uma amostra de ${employee.voiceprint_min_samples} necessárias.`);
  const voiceAbortRef = useRef<AbortController | null>(null);

  async function updateName() {
    if (!name.trim()) return setVoiceStatus("Informe um nome válido.");
    try {
      const { data } = await api.post<{ message: string }>("/employees/update", { employee_id: employee.id, employee_name: name.trim() });
      setVoiceStatus(data.message);
    } catch (error) { setVoiceStatus(apiMessage(error, "Falha ao atualizar usuário.")); }
  }

  async function enrollVoice() {
    const controller = new AbortController();
    voiceAbortRef.current = controller;
    setVoiceBusy(true);
    setVoiceStatus("Gravando voz por até 5 segundos...");
    try {
      const pcm_base64 = await captureVoicePcm(5, controller.signal);
      const { data } = await api.post<{ message: string; total_samples: number; ready: boolean }>("/voice-biometry/enroll", { employee_id: employee.id, pcm_base64 });
      setVoiceStatus(`${data.message} Total: ${data.total_samples}${data.ready ? " (pronto)" : " (faltam amostras)"}.`);
    } catch (error) {
      if (error instanceof DOMException && error.name === "AbortError") {
        setVoiceStatus("Gravação cancelada.");
      } else if (error instanceof DOMException && error.name === "TimeoutError") {
        setVoiceStatus("Não foi possível acessar o microfone em 5 segundos.");
      } else {
        setVoiceStatus(apiMessage(error, "Falha ao gravar voiceprint."));
      }
    } finally {
      voiceAbortRef.current = null;
      setVoiceBusy(false);
    }
  }

  function cancelVoice() {
    voiceAbortRef.current?.abort();
  }

  async function clearVoice() {
    if (!window.confirm("Remover todos os voiceprints deste colaborador?")) return;
    try {
      const { data } = await api.post<{ message: string }>("/voice-biometry/clear", { employee_id: employee.id });
      setVoiceStatus(data.message);
    } catch (error) { setVoiceStatus(apiMessage(error, "Falha ao limpar voiceprints.")); }
  }

  return <article className="employee-row employee-row-managed">
    <div className="employee-details">
      <strong>{employee.name}</strong>
      <span>ID {employee.id} · {employee.sample_count} amostras faciais</span>
      <span className={employee.voiceprint_ready ? "tag good" : "tag"}>Voz {employee.voiceprint_count}/{employee.voiceprint_min_samples}</span>
    </div>
    <div className="employee-controls">
      <input aria-label={`Nome de ${employee.id}`} value={name} onChange={(event) => setName(event.target.value)} />
      <button className="button" type="button" onClick={() => void updateName()}>Salvar nome</button>
      <button className="button" type="button" onClick={() => window.open(`/lgpd/export/${encodeURIComponent(employee.id)}`, "_blank")}>Exportar LGPD</button>
      <button className={voiceBusy ? "button recording" : "button primary"} type="button" onClick={voiceBusy ? cancelVoice : () => void enrollVoice()}>{voiceBusy ? "Cancelar gravação" : "Gravar voz (5s)"}</button>
      <button className="button ghost" type="button" onClick={() => void clearVoice()}>Limpar voz</button>
      <button className="icon-button" disabled={busy} onClick={onRemove} title="Remover">×</button>
      <span className="employee-status">{voiceStatus}</span>
    </div>
  </article>;
}

function RegistrationPanel({ onClose }: { onClose: () => void }) {
  const videoRef = useRef<HTMLVideoElement>(null);
  const canvasRef = useRef<HTMLCanvasElement>(null);
  const streamRef = useRef<MediaStream | null>(null);
  const [employeeId, setEmployeeId] = useState(createEmployeeId);
  const [employeeName, setEmployeeName] = useState("");
  const [consent, setConsent] = useState(false);
  const [images, setImages] = useState<string[]>([]);
  const [privacy, setPrivacy] = useState<PrivacyNotice | null>(null);
  const [status, setStatus] = useState("Inicializando câmera...");
  const [statusType, setStatusType] = useState("info");
  const [busy, setBusy] = useState(false);
  const [phrases, setPhrases] = useState<string[]>([]);
  const [phrase, setPhrase] = useState("");
  const [bulkPhrases, setBulkPhrases] = useState("");
  const [phraseStatus, setPhraseStatus] = useState("Carregando frases de treino...");
  const phraseFileRef = useRef<HTMLInputElement>(null);
  const [voiceConfig, setVoiceConfig] = useState<VoiceBiometryConfig>({
    enforced: true, min_samples: 3, min_seconds: 1.5, threshold: 0.7,
  });

  function updateStatus(message: string, type = "info") {
    setStatus(message);
    setStatusType(type);
  }

  async function loadPhrases() {
    try {
      const { data } = await api.get<{ phrases: string[] }>("/voice-phrases");
      setPhrases(data.phrases ?? []);
      setPhraseStatus(`${data.phrases?.length ?? 0} frases de treino carregadas.`);
    } catch (error) {
      setPhraseStatus(apiMessage(error, "Falha ao carregar frases."));
    }
  }

  useEffect(() => {
    let active = true;
    void api.get<PrivacyNotice>("/lgpd/privacy-notice")
      .then(({ data }) => { if (active) setPrivacy(data); })
      .catch(() => undefined);
    void api.get<{ voice_biometry?: VoiceBiometryConfig }>("/employees")
      .then(({ data }) => { if (active && data.voice_biometry) setVoiceConfig(data.voice_biometry); })
      .catch(() => undefined);
    void loadPhrases();
    async function startCamera() {
      if (!navigator.mediaDevices?.getUserMedia) {
        updateStatus("Este navegador não disponibilizou a câmera. Use a opção Selecionar fotos.", "warning");
        return;
      }
      try {
        let stream: MediaStream;
        try {
          stream = await getCameraStream({
            video: { width: { ideal: 1280 }, height: { ideal: 720 } }, audio: false,
          });
        } catch (error) {
          if (error instanceof DOMException && error.name === "TimeoutError") throw error;
          stream = await getCameraStream({ video: true, audio: false });
        }
        if (!active) {
          stream.getTracks().forEach((track) => track.stop());
          return;
        }
        streamRef.current = stream;
        const video = videoRef.current;
        if (video) {
          video.srcObject = stream;
          await video.play().catch(() => undefined);
        }
        updateStatus("Câmera pronta. Capture pelo menos 3 fotos.", "success");
      } catch (error) {
        const reason = error instanceof DOMException && error.name === "TimeoutError"
          ? "A câmera não respondeu em 5 segundos."
          : error instanceof DOMException && error.name === "NotAllowedError"
          ? "Permissão da câmera negada."
          : "A câmera não está disponível.";
        updateStatus(`${reason} Use a opção Selecionar fotos.`, "warning");
      }
    }
    void startCamera();
    return () => {
      active = false;
      streamRef.current?.getTracks().forEach((track) => track.stop());
    };
  }, []);

  function captureFrame() {
    const video = videoRef.current;
    const canvas = canvasRef.current;
    if (!video || !canvas || !video.videoWidth || !video.videoHeight) return null;
    canvas.width = video.videoWidth;
    canvas.height = video.videoHeight;
    canvas.getContext("2d")?.drawImage(video, 0, 0, canvas.width, canvas.height);
    return canvas.toDataURL("image/jpeg", 0.95);
  }

  function capture() {
    const image = captureFrame();
    if (!image) return updateStatus("A câmera ainda não está pronta.", "warning");
    setImages((current) => [...current, image]);
    updateStatus(`Foto capturada. Total: ${images.length + 1}.`, "success");
  }

  function addImageFiles(event: React.ChangeEvent<HTMLInputElement>) {
    const files = Array.from(event.target.files ?? []);
    if (!files.length) return;
    Promise.all(files.map((file) => new Promise<string>((resolve, reject) => {
      const reader = new FileReader();
      reader.onload = () => resolve(String(reader.result));
      reader.onerror = () => reject(reader.error);
      reader.readAsDataURL(file);
    }))).then((newImages) => {
      setImages((current) => [...current, ...newImages]);
      updateStatus(`${newImages.length} foto(s) adicionada(s).`, "success");
    }).catch(() => updateStatus("Não foi possível carregar as fotos selecionadas.", "error"));
    event.target.value = "";
  }

  async function register() {
    if (!employeeName.trim()) return updateStatus("Informe o nome da pessoa.", "warning");
    if (!images.length) return updateStatus("Capture pelo menos uma foto antes de cadastrar.", "warning");
    if (!consent) return updateStatus("Confirme o consentimento LGPD antes de cadastrar.", "warning");
    setBusy(true);
    try {
      const { data } = await api.post<{ message: string }>("/register-person", {
        employee_id: employeeId.trim(), employee_name: employeeName.trim(), images, consent: true,
      });
      setImages([]);
      setEmployeeId(createEmployeeId());
      setEmployeeName("");
      setConsent(false);
      updateStatus(data.message, "success");
    } catch (error) {
      updateStatus(apiMessage(error, "Falha ao cadastrar pessoa."), "error");
    } finally {
      setBusy(false);
    }
  }

  async function addPhrase() {
    if (!phrase.trim()) return setPhraseStatus("Informe uma frase para treino.");
    try {
      const { data } = await api.post<{ phrases: string[]; message: string }>("/voice-phrases/add", { phrase: phrase.trim() });
      setPhrases(data.phrases ?? []);
      setPhrase("");
      setPhraseStatus(data.message);
    } catch (error) {
      setPhraseStatus(apiMessage(error, "Falha ao adicionar frase."));
    }
  }

  async function addBulkPhrases() {
    if (!bulkPhrases.trim()) return setPhraseStatus("Informe uma frase por linha.");
    try {
      const { data } = await api.post<{ phrases: string[]; message: string }>("/voice-phrases/bulk", { text: bulkPhrases });
      setPhrases(data.phrases ?? []);
      setBulkPhrases("");
      setPhraseStatus(data.message);
    } catch (error) {
      setPhraseStatus(apiMessage(error, "Falha no treino em lote."));
    }
  }

  async function removePhrase(item: string) {
    if (!window.confirm("Deseja remover esta frase do treino de voz?")) return;
    try {
      const { data } = await api.post<{ phrases: string[]; message: string }>("/voice-phrases/remove", { phrase: item });
      setPhrases(data.phrases ?? []);
      setPhraseStatus(data.message);
    } catch (error) {
      setPhraseStatus(apiMessage(error, "Falha ao remover frase."));
    }
  }

  async function importPhrases(event: React.ChangeEvent<HTMLInputElement>) {
    const file = event.target.files?.[0];
    if (!file) return;
    const form = new FormData();
    form.append("file", file);
    try {
      const { data } = await api.post<{ phrases: string[]; message: string }>("/voice-phrases/import-json", form, {
        headers: { "Content-Type": "multipart/form-data" },
      });
      setPhrases(data.phrases ?? []);
      setPhraseStatus(data.message);
    } catch (error) {
      setPhraseStatus(apiMessage(error, "Falha ao importar JSON."));
    } finally {
      event.target.value = "";
    }
  }

  return <div className="registration-panel" role="dialog" aria-modal="true" aria-labelledby="registration-title">
    <div className="registration-header"><div><p className="eyebrow">CADASTRO BIOMÉTRICO</p><h2 id="registration-title">Cadastrar nova pessoa</h2></div><button className="text-button" type="button" onClick={onClose}>Fechar</button></div>
    <div className="registration-grid">
      <div>
        <div className="registration-form">
          <label>Nome da pessoa<input maxLength={120} value={employeeName} onChange={(event) => setEmployeeName(event.target.value)} placeholder="Ex.: Maria Silva" /></label>
        </div>
        <div className="privacy-box">{privacy?.status === "ok" ? `Aviso de privacidade (v${privacy.version})\n\n${privacy.text}\n\nRetenção de imagens: ${privacy.retention_days} dias.` : "Não foi possível carregar o aviso de privacidade."}</div>
        <label className="consent-row"><input type="checkbox" checked={consent} onChange={(event) => setConsent(event.target.checked)} /><span>Declaro ter lido o aviso de privacidade e autorizo o tratamento dos dados biométricos para controle de ponto, conforme a LGPD.</span></label>
        <video className="registration-camera" autoPlay muted playsInline ref={videoRef} />
        <canvas className="hidden" ref={canvasRef} />
        <div className="actions"><button className="button" type="button" onClick={capture} disabled={busy}>Capturar foto</button><label className="button ghost file-button">Selecionar fotos<input type="file" accept="image/png,image/jpeg" multiple onChange={addImageFiles} /></label><button className="button primary" type="button" onClick={() => void register()} disabled={busy}>Cadastrar pessoa</button><button className="button ghost" type="button" onClick={() => { setImages([]); updateStatus("Fotos removidas."); }} disabled={busy}>Limpar fotos</button></div>
        <p className={`feedback status-${statusType}`}>{status}</p>
        <div className="preview-grid">{images.map((image, index) => <figure key={`${image.slice(-20)}-${index}`}><img src={image} alt={`Foto capturada ${index + 1}`} /><button type="button" className="button ghost" onClick={() => setImages((current) => current.filter((_, itemIndex) => itemIndex !== index))}>Remover</button></figure>)}</div>
      </div>
      <aside className="voice-training">
        <p className="eyebrow">RECONHECIMENTO DE VOZ</p><h3>Treino de frases</h3><p className="muted">{phraseStatus}</p>
        <div className="stack-form"><input maxLength={200} value={phrase} onChange={(event) => setPhrase(event.target.value)} placeholder="Frase para treinar" /><button className="button primary" type="button" onClick={() => void addPhrase()}>Adicionar frase</button><textarea rows={4} value={bulkPhrases} onChange={(event) => setBulkPhrases(event.target.value)} placeholder="Uma frase por linha" /><button className="button" type="button" onClick={() => void addBulkPhrases()}>Treinar em lote</button><input ref={phraseFileRef} type="file" accept="application/json,.json" onChange={(event) => void importPhrases(event)} /><button className="button" type="button" onClick={() => phraseFileRef.current?.click()}>Importar JSON</button><button className="button" type="button" onClick={() => window.open("/voice-phrases/export-json", "_blank")}>Exportar JSON</button></div>
        <div className="phrase-list">{phrases.map((item) => <div className="phrase-item" key={item}><span>{item}</span><button className="button ghost" type="button" onClick={() => void removePhrase(item)}>Remover</button></div>)}</div>
        <small>Biometria de voz: mínimo de {voiceConfig.min_samples} amostras de {voiceConfig.min_seconds}s.</small>
      </aside>
    </div>
  </div>;
}

function Dashboard({ profile, onLogout, onView }: { profile: Profile; onLogout: () => void; onView: (view: View) => void }) {
  return <main className="app-shell"><header className="topbar"><div><p className="eyebrow">AMOR EM MECHAS</p><h1>Ponto</h1></div><div className="topbar-actions"><span className="profile-pill">{profile === "admin" ? "Administrador" : "Colaborador"}</span><button className="text-button" onClick={onLogout}>Sair</button></div></header><section className="hero"><div><p className="eyebrow">JORNADA DE HOJE</p><h2>Presença registrada<br /><em>com confiança.</em></h2><p className="muted">Use a câmera para reconhecer sua identidade e registrar entrada ou saída.</p><button className="button primary large" onClick={() => onView("recognition")}>Abrir reconhecimento <span>→</span></button></div><div className="hero-orbit"><div className="orbit-core">AM</div><span className="orbit-label">FACE<br />+ VOZ</span></div></section>{profile === "admin" && <AdminPanel />}<footer><span>Reconhecimento facial seguro</span><span>LGPD · Biometria protegida</span></footer></main>;
}

export default function App() {
  const [profile, setProfile] = useState<Profile | null>(null);
  const [view, setView] = useState<View>("home");
  useEffect(() => { api.get<{ profile?: Profile }>("/me").then(({ data }) => setProfile(data.profile ?? null)).catch(() => setProfile(null)); }, []);
  async function logout() { await api.post("/logout").catch(() => undefined); setProfile(null); setView("home"); }
  if (!profile) return <Login onLogin={setProfile} />;
  if (view === "recognition") return <CameraRecognition onBack={() => setView("home")} />;
  return <Dashboard profile={profile} onLogout={() => void logout()} onView={setView} />;
}
