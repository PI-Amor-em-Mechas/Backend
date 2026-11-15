import { useEffect, useRef, useState } from "react";
import { io, type Socket } from "socket.io-client";
import { api, apiMessage } from "./api";
import type { Employee, Profile, RecognitionResult } from "./types";

type View = "home" | "recognition" | "admin";

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
  const [form, setForm] = useState({ id: "", name: "" });
  const [message, setMessage] = useState("");
  const [busy, setBusy] = useState(false);

  async function loadEmployees() {
    try {
      const { data } = await api.get<{ employees: Employee[] }>("/employees");
      setEmployees(data.employees);
    } catch (error) {
      setMessage(apiMessage(error, "Não foi possível carregar os colaboradores."));
    }
  }
  useEffect(() => { void loadEmployees(); }, []);

  async function register(event: React.FormEvent) {
    event.preventDefault();
    setMessage("O cadastro facial exige capturas da câmera. Abra o fluxo de cadastro para concluir.");
  }
  async function remove(id: string) {
    if (!window.confirm("Anonimizar e remover este colaborador?")) return;
    setBusy(true);
    try {
      await api.post("/employees/delete", { employee_id: id });
      setMessage("Colaborador removido e dados anonimizados.");
      await loadEmployees();
    } catch (error) { setMessage(apiMessage(error, "Não foi possível remover o colaborador.")); }
    finally { setBusy(false); }
  }

  return <section className="admin-section"><div className="section-heading"><div><p className="eyebrow">ADMINISTRAÇÃO</p><h2>Colaboradores</h2></div><span className="count-badge">{employees.length} cadastrados</span></div><form className="inline-form" onSubmit={register}><input placeholder="ID do colaborador" value={form.id} onChange={(event) => setForm({ ...form, id: event.target.value })} /><input placeholder="Nome completo" value={form.name} onChange={(event) => setForm({ ...form, name: event.target.value })} /><button className="button dark" type="submit">Preparar cadastro</button></form>{message && <p className="feedback">{message}</p>}<div className="employee-list">{employees.map((employee) => <article className="employee-row" key={employee.id}><div><strong>{employee.name}</strong><span>ID {employee.id} · {employee.sample_count} amostras faciais</span></div><div className="employee-meta"><span className={employee.consent_valid ? "tag good" : "tag warn"}>{employee.consent_valid ? "LGPD ok" : "Sem consentimento"}</span><span className={employee.voiceprint_ready ? "tag good" : "tag"}>Voz {employee.voiceprint_count}/{employee.voiceprint_min_samples}</span><button className="icon-button" disabled={busy} onClick={() => remove(employee.id)} title="Remover">×</button></div></article>)}{employees.length === 0 && <p className="muted">Nenhum colaborador cadastrado.</p>}</div></section>;
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
