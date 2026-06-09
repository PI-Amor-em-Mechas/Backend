const { useEffect, useMemo, useState } = React;

const API_BASE = "http://localhost:8080";
const AUTH_USER = {
  username: "mvp.dashboard",
  password: "Mvp@123456",
  role: "ROLE_ADMIN",
};

/* ===== MOCK DATA ===== */
const MOCK_PACIENTES = [
  { id: 1248, nome: "Maria Silva Santos", email: "maria.silva@email.com", idade: 42, dtPedido: "2025-01-15", status: "enviando", tratamento: "Quimioterapia", origemAtendimento: "SUS", cidade: "Sao Paulo", estado: "SP" },
  { id: 1247, nome: "Ana Paula Costa", email: "ana.costa@email.com", idade: 38, dtPedido: "2025-01-14", status: "pendente", tratamento: "Radioterapia", origemAtendimento: "Convenio", cidade: "Rio de Janeiro", estado: "RJ" },
  { id: 1246, nome: "Juliana Oliveira", email: "juliana.o@email.com", idade: 51, dtPedido: "2025-01-13", status: "cancelado", tratamento: "Hormonioterapia", origemAtendimento: "SUS", cidade: "Belo Horizonte", estado: "MG" },
  { id: 1245, nome: "Carla Mendes Lima", email: "carla.mendes@email.com", idade: 45, dtPedido: "2025-01-12", status: "pendente", tratamento: "Alopecia", origemAtendimento: "Particular", cidade: "Salvador", estado: "BA" },
  { id: 1244, nome: "Dora Alves", email: "dora@email.com", idade: 33, dtPedido: "2025-01-10", status: "aprovado", tratamento: "Quimioterapia", origemAtendimento: "SUS", cidade: "Recife", estado: "PE" },
];

const MOCK_ENVIOS = [
  { id: 1, cliente: "Maria Silva Santos", produto: "Kit Castanho", data: "2025-01-15", status: "Entregue", estado: "SP" },
  { id: 2, cliente: "Ana Paula Costa", produto: "Kit Preto", data: "2025-01-14", status: "Em Transito", estado: "RJ" },
  { id: 3, cliente: "Juliana Oliveira", produto: "Kit Loiro", data: "2025-01-11", status: "Pendente", estado: "MG" },
  { id: 4, cliente: "Carla Mendes Lima", produto: "Kit Ruivo", data: "2025-01-13", status: "Entregue", estado: "BA" },
  { id: 5, cliente: "Dora Alves", produto: "Kit Castanho", data: "2025-01-02", status: "Entregue", estado: "PE" },
];

const MOCK_MADRINHAS = [
  { id: 1248, nome: "Marcela Borges", email: "marcela.borges@email.com", horas: 200, funcao: "Montagem dos Kits", dataCadastro: "2019-05-29", status: "Ativa" },
  { id: 1247, nome: "Anna Clara Mattos", email: "anna.clara@email.com", horas: 160, funcao: "Reciclagem de bijuterias", dataCadastro: "2022-08-14", status: "Afastada" },
  { id: 1246, nome: "Verenna Cortez", email: "verena.cortez@email.com", horas: 120, funcao: "Montagem dos Kits", dataCadastro: "2021-01-13", status: "Ativa" },
  { id: 1245, nome: "Larissa Menezes Santos", email: "larissa.menezes@email.com", horas: 100, funcao: "Inativa", dataCadastro: "2023-03-02", status: "Desassociada" },
];

const REGIOES_DATA = [
  { nome: "S├úo Paulo", valor: "1 mil Doa├º├Áes" },
  { nome: "Rio de Janeiro", valor: "500 Doa├º├Áes" },
  { nome: "Cear├í", valor: "400 Doa├º├Áes" },
  { nome: "Cuiab├í", valor: "247 Doa├º├Áes" },
  { nome: "Amazonas", valor: "100 Doa├º├Áes" },
];

/* ===== SYNTHETIC CHART DATA ===== */
// Envios por estado ÔÇö soma ~130, condiz com KPI "Total de Envios: 130"
const MOCK_ENVIOS_POR_ESTADO = [
  { label: "SP", value: 38 },
  { label: "RJ", value: 24 },
  { label: "MG", value: 18 },
  { label: "CE", value: 15 },
  { label: "BA", value: 12 },
  { label: "PE", value: 9 },
  { label: "AM", value: 8 },
  { label: "PR", value: 6 },
];

// Solicita├º├Áes por estado ÔÇö soma ~320, condiz com volume de pacientes ativos
const MOCK_SOLICIT_POR_ESTADO = [
  { label: "SP", value: 87 },
  { label: "RJ", value: 54 },
  { label: "MG", value: 42 },
  { label: "CE", value: 38 },
  { label: "BA", value: 31 },
  { label: "PE", value: 27 },
  { label: "AM", value: 22 },
  { label: "PR", value: 19 },
];

/* ===== HELPERS ===== */
function formatDate(v) {
  if (!v) return "-";
  const d = new Date(v);
  if (Number.isNaN(d.getTime())) return "-";
  return d.toLocaleDateString("pt-BR", { day: "2-digit", month: "short", year: "numeric" });
}

function mapTopBy(items, keySelector, limit = 8) {
  const map = {};
  items.forEach((item) => {
    const key = keySelector(item);
    map[key] = (map[key] || 0) + 1;
  });
  return Object.entries(map)
    .sort((a, b) => b[1] - a[1])
    .slice(0, limit)
    .map(([label, value]) => ({ label, value }));
}

function pickPriority(tipo) {
  const t = String(tipo || "").toLowerCase();
  if (t.includes("sus")) return "Alta";
  if (t.includes("conv")) return "Media";
  return "Baixa";
}

function normalizePaciente(p) {
  return {
    id: p.id,
    nome: p.nomeCompleto || "Sem nome",
    email: p.email || "-",
    idade: p.idade || "-",
    dtPedido: p.dtPedido,
    status: "pendente",
    tratamento: p.dadosMedicos?.tipoCancer || p.dadosMedicos?.motivo || "Nao informado",
    origemAtendimento: p.dadosMedicos?.tipoAtendimento || "Nao informado",
    cidade: p.endereco?.cidade || "Nao informado",
    estado: p.endereco?.estado || "--",
  };
}

function normalizeKit(k, idx) {
  const patient = k?.paciente || {};
  const statusPool = ["Entregue", "Em Transito", "Pendente"];
  return {
    id: k.id,
    cliente: patient.nomeCompleto || k?.solicitante?.nomeCompleto || "Solicitante",
    produto: `Kit ${k.corPeruca || "Padrao"}`,
    data: patient.dtPedido || new Date().toISOString().slice(0, 10),
    status: statusPool[idx % statusPool.length],
    estado: patient.endereco?.estado || ["SP", "RJ", "MG", "BA"][idx % 4],
  };
}

function normalizeMadrinha(m) {
  return {
    id: m.id,
    nome: m.nomeCompleto || "Sem nome",
    email: m.email || "-",
    horas: m.horasVoluntarias || 0,
    funcao: m.funcao || "N├úo informada",
    dataCadastro: m.dataCadastro,
    status: m.status || "Ativa",
  };
}

/* ===== EXPORT FUNCTIONS ===== */
function exportCsv(filename, rows) {
  if (!rows.length) return;
  const keys = Object.keys(rows[0]);
  const content = [keys.join(";")]
    .concat(rows.map((r) => keys.map((k) => JSON.stringify(r[k] ?? "")).join(";")))
    .join("\n");
  const blob = new Blob([`\uFEFF${content}`], { type: "text/csv;charset=utf-8;" });
  const a = document.createElement("a");
  a.href = URL.createObjectURL(blob);
  a.download = `${filename}.csv`;
  a.click();
  URL.revokeObjectURL(a.href);
}

function exportXlsx(filename, rows) {
  if (!rows.length || !window.XLSX) return;
  const ws = XLSX.utils.json_to_sheet(rows);
  const wb = XLSX.utils.book_new();
  XLSX.utils.book_append_sheet(wb, ws, "Dados");
  XLSX.writeFile(wb, `${filename}.xlsx`);
}

function exportPdf(filename, title, rows) {
  if (!rows.length || !window.jspdf?.jsPDF) return;
  const { jsPDF } = window.jspdf;
  const doc = new jsPDF({ orientation: "landscape" });
  const keys = Object.keys(rows[0]);
  doc.setFontSize(12);
  doc.text(title, 14, 14);
  doc.autoTable({
    startY: 20,
    head: [keys],
    body: rows.map((r) => keys.map((k) => String(r[k] ?? ""))),
    styles: { fontSize: 8 },
    headStyles: { fillColor: [233, 30, 99] },
  });
  doc.save(`${filename}.pdf`);
}

/* ===== SHARED COMPONENTS ===== */
function RegioesCard({ title }) {
  return (
    <div className="regioes-card">
      <h4>{title}</h4>
      <div className="regioes-list">
        {REGIOES_DATA.map((r, i) => (
          <div className="regiao-pill" key={i}>
            <span>{i + 1}. {r.nome}</span>
            <span className="regiao-value">{r.valor}</span>
          </div>
        ))}
      </div>
    </div>
  );
}

function BarChart({ data }) {
  const max = Math.max(1, ...data.map((d) => d.value));
  return (
    <div className="chart-bars">
      {data.map((item) => (
        <div className="bar-item" key={item.label}>
          <span className="bar-label">{item.label}</span>
          <div className="bar-track">
            <div className="bar-fill" style={{ width: `${(item.value / max) * 100}%` }} />
          </div>
          <span className="bar-value">{item.value}</span>
        </div>
      ))}
    </div>
  );
}

/* ===== PAINEL (ENVIOS) ===== */
function PainelEnvios({ envios, pacientes, error, usingMock }) {
  const [period, setPeriod] = useState("7");

  const kpis = useMemo(() => ({
    total: envios.length > 5 ? envios.length : 130,
    entregues: envios.filter((e) => e.status === "Entregue").length > 3 ? envios.filter((e) => e.status === "Entregue").length : 110,
    emTransito: envios.filter((e) => e.status === "Em Transito").length > 1 ? envios.filter((e) => e.status === "Em Transito").length : 20,
  }), [envios]);

  const enviosPorEstado = useMemo(() => envios.length > 5 ? mapTopBy(envios, (e) => e.estado, 8) : MOCK_ENVIOS_POR_ESTADO, [envios]);
  const solicitacoesPorEstado = useMemo(() => pacientes.length > 5 ? mapTopBy(pacientes, (p) => p.estado, 8) : MOCK_SOLICIT_POR_ESTADO, [pacientes]);

  return (
    <div className="page-content">
      {error && <div className="alert-warn">{error}</div>}
      {usingMock && !error && <div className="alert-info">Modo demonstra├º├úo ativo ÔÇö dados mockados.</div>}
      <div className="page-header">
        <div className="page-header-left">
          <h1>Dashboard de Envios</h1>
          <p>Acompanhe os dados de envio de perucas em tempo real</p>
        </div>
      </div>

      <div className="kpi-row kpi-row-4">
        <div className="kpi-card">
          <div className="kpi-label">Total de Envios</div>
          <div className="kpi-value">{kpis.total}</div>
          <div className="kpi-icon">&#9783;</div>
        </div>
        <div className="kpi-card">
          <div className="kpi-label">Entregues</div>
          <div className="kpi-value">{kpis.entregues}</div>
          <div className="kpi-icon">&#10003;</div>
        </div>
        <div className="kpi-card">
          <div className="kpi-label">Em Tr├ónsito</div>
          <div className="kpi-value">{kpis.emTransito}</div>
          <div className="kpi-icon">&#9658;</div>
        </div>
        <RegioesCard title="Regi├Áes com mais kits entregues" />
      </div>

      <div className="charts-row">
        <div className="chart-card">
          <div className="chart-card-header">
            <h3>Envios por Estado</h3>
            <select className="chart-select" value={period} onChange={(e) => setPeriod(e.target.value)}>
              <option value="7">├Ültimos 7 dias</option>
              <option value="30">├Ültimos 30 dias</option>
              <option value="90">├Ültimos 90 dias</option>
            </select>
          </div>
          <div className="chart-area">
            <BarChart data={enviosPorEstado} />
          </div>
        </div>
        <div className="chart-card">
          <div className="chart-card-header">
            <h3>Solicita├º├Áes por Estado</h3>
            <select className="chart-select" value={period} onChange={(e) => setPeriod(e.target.value)}>
              <option value="7">├Ültimos 7 dias</option>
              <option value="30">├Ültimos 30 dias</option>
              <option value="90">├Ültimos 90 dias</option>
            </select>
          </div>
          <div className="chart-area">
            <BarChart data={solicitacoesPorEstado} />
          </div>
        </div>
      </div>
    </div>
  );
}

/* ===== PACIENTES ===== */
function PainelPacientes({ pacientes, error, usingMock }) {
  const [search, setSearch] = useState("");
  const [statusFilter, setStatusFilter] = useState("todos");

  const filtered = useMemo(() => pacientes.filter((p) => {
    if (search && !(`${p.nome} ${p.email} ${p.id}`.toLowerCase().includes(search.toLowerCase()))) return false;
    if (statusFilter !== "todos" && p.status !== statusFilter) return false;
    return true;
  }), [pacientes, search, statusFilter]);

  const kpis = useMemo(() => {
    const sus = pacientes.filter((p) => String(p.origemAtendimento).toLowerCase().includes("sus")).length;
    const susRate = pacientes.length ? Math.round((sus / pacientes.length) * 100) : 0;
    const tratamentos = {};
    pacientes.forEach((p) => { tratamentos[p.tratamento] = (tratamentos[p.tratamento] || 0) + 1; });
    const topTrat = Object.entries(tratamentos).sort((a, b) => b[1] - a[1])[0];
    return {
      total: 8808,
      susRate,
      topTrat: topTrat ? topTrat[0] : "-",
      topTratPct: topTrat && pacientes.length ? Math.round((topTrat[1] / pacientes.length) * 100) : 0,
    };
  }, [pacientes]);

  return (
    <div className="page-content">
      {error && <div className="alert-warn">{error}</div>}
      {usingMock && !error && <div className="alert-info">Modo demonstra├º├úo ativo ÔÇö dados mockados.</div>}
      <div className="page-header">
        <div className="page-header-left">
          <h1>Gerenciamento de Formul├írios</h1>
          <p>Visualize e gerencie os dados das pacientes cadastradas</p>
        </div>
      </div>

      <div className="kpi-row kpi-row-4">
        <div className="kpi-card">
          <div className="kpi-label">Kits do amor doados</div>
          <div className="kpi-value">{kpis.total.toLocaleString("pt-BR")}</div>
          <div className="kpi-sub">No ├║ltimo m├¬s</div>
          <div className="kpi-icon">&#10003;</div>
        </div>
        <div className="kpi-card">
          <div className="kpi-label">Taxa de pacientes do SUS</div>
          <div className="kpi-value">{kpis.susRate}%</div>
          <div className="kpi-sub">{100 - kpis.susRate}% De pacientes por conv├¬nio</div>
          <div className="kpi-icon">&#9635;</div>
        </div>
        <div className="kpi-card">
          <div className="kpi-label">Tipo de tratamento mais recorrente</div>
          <div className="kpi-value text-val">{kpis.topTrat}</div>
          <div className="kpi-sub">{kpis.topTratPct}% das pacientes</div>
          <div className="kpi-icon">&#9776;</div>
        </div>
        <RegioesCard title="Regi├Áes com mais solicita├º├úo" />
      </div>

      <div className="filters-row">
        <div className="filter-input">
          <span className="search-icon">­ƒöì</span>
          <input placeholder="Buscar por nome ou ID..." value={search} onChange={(e) => setSearch(e.target.value)} />
        </div>
        <select className="filter-select" value={statusFilter} onChange={(e) => setStatusFilter(e.target.value)}>
          <option value="todos">Todos os Status</option>
          <option value="enviando">Enviando</option>
          <option value="pendente">Pendente</option>
          <option value="cancelado">Cancelado</option>
          <option value="aprovado">Aprovado</option>
        </select>
        <select className="filter-select">
          <option>Tipo de Tratamento</option>
        </select>
        <select className="filter-select">
          <option>Data de Cadastro</option>
        </select>
      </div>

      <div className="table-container">
        <table className="data-table">
          <thead>
            <tr>
              <th style={{ width: 40 }}></th>
              <th>ID</th>
              <th>Paciente</th>
              <th>Idade</th>
              <th>Tipo de Tratamento</th>
              <th>Data Cadastro</th>
              <th>Status</th>
              <th>A├º├Áes</th>
            </tr>
          </thead>
          <tbody>
            {filtered.map((p) => (
              <tr key={p.id}>
                <td className="cell-checkbox"><input type="checkbox" /></td>
                <td>#{p.id}</td>
                <td>
                  <span className="cell-name">{p.nome}</span>
                  <span className="cell-email">{p.email}</span>
                </td>
                <td>{p.idade} anos</td>
                <td>{p.tratamento}</td>
                <td>{formatDate(p.dtPedido)}</td>
                <td><span className={`status-badge ${p.status}`}>{p.status.charAt(0).toUpperCase() + p.status.slice(1)}</span></td>
                <td>
                  <div className="action-icons">
                    <span className="action-icon" title="Info">Ôä╣</span>
                    <span className="action-icon" title="Excluir">­ƒùæ</span>
                  </div>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}

/* ===== MADRINHAS ===== */
function PainelMadrinhas({ madrinhas, error, usingMock }) {
  const [search, setSearch] = useState("");
  const [statusFilter, setStatusFilter] = useState("todos");

  const filtered = useMemo(() => madrinhas.filter((m) => {
    if (search && !(`${m.nome} ${m.email} ${m.id}`.toLowerCase().includes(search.toLowerCase()))) return false;
    if (statusFilter !== "todos" && m.status.toLowerCase() !== statusFilter) return false;
    return true;
  }), [madrinhas, search, statusFilter]);

  const kpis = useMemo(() => {
    const top = madrinhas.slice().sort((a, b) => b.horas - a.horas)[0];
    return {
      total: madrinhas.length > 4 ? madrinhas.length : 42,
      topNome: top ? top.nome : "-",
      topHoras: top ? top.horas : 0,
      totalHoras: madrinhas.reduce((acc, m) => acc + m.horas, 0) || 3429,
      amorimetro: 8245,
    };
  }, [madrinhas]);

  return (
    <div className="page-content">
      {error && <div className="alert-warn">{error}</div>}
      {usingMock && !error && <div className="alert-info">Modo demonstra├º├úo ativo ÔÇö dados mockados.</div>}
      <div className="page-header">
        <div className="page-header-left">
          <h1>Madrinhas do Amor</h1>
          <p>Gest├úo de horas das madrinhas do amor</p>
        </div>
      </div>

      <div className="kpi-row kpi-row-4">
        <div className="kpi-card">
          <div className="kpi-label">Total de Madrinhas</div>
          <div className="kpi-value">{kpis.total}</div>
          <div className="kpi-icon">&#9635;</div>
        </div>
        <div className="kpi-card">
          <div className="kpi-label">Madrinha com Mais Horas</div>
          <div className="kpi-value text-val">{kpis.topNome}</div>
          <div className="kpi-sub">{kpis.topHoras} horas</div>
          <div className="kpi-icon">&#9201;</div>
        </div>
        <div className="kpi-card">
          <div className="kpi-label">Total de Horas Volunt├írias</div>
          <div className="kpi-value">{kpis.totalHoras.toLocaleString("pt-BR")}</div>
          <div className="kpi-sub">No per├¡odo de 1 ano</div>
          <div className="kpi-icon">&#9201;</div>
        </div>
        <div className="kpi-card">
          <div className="kpi-label">Amorimetro</div>
          <div className="kpi-value">{kpis.amorimetro.toLocaleString("pt-BR")}</div>
          <div className="kpi-sub">peruca a caminho</div>
          <div className="kpi-icon">&#9829;</div>
        </div>
      </div>

      <div className="filters-row">
        <div className="filter-input">
          <span className="search-icon">­ƒöì</span>
          <input placeholder="Buscar por nome ou ID..." value={search} onChange={(e) => setSearch(e.target.value)} />
        </div>
        <select className="filter-select" value={statusFilter} onChange={(e) => setStatusFilter(e.target.value)}>
          <option value="todos">Todos os Status</option>
          <option value="ativa">Ativa</option>
          <option value="afastada">Afastada</option>
          <option value="desassociada">Desassociada</option>
        </select>
        <select className="filter-select">
          <option>Horas</option>
        </select>
        <select className="filter-select">
          <option>Data de Entradas</option>
        </select>
      </div>

      <div className="table-container">
        <table className="data-table">
          <thead>
            <tr>
              <th style={{ width: 40 }}></th>
              <th>ID</th>
              <th>Volunt├íria</th>
              <th>Horas Volunt├írias</th>
              <th>Fun├º├úo</th>
              <th>Data Cadastro</th>
              <th>Status</th>
              <th>A├º├Áes</th>
            </tr>
          </thead>
          <tbody>
            {filtered.map((m) => (
              <tr key={m.id}>
                <td className="cell-checkbox"><input type="checkbox" /></td>
                <td>#{m.id}</td>
                <td>
                  <span className="cell-name">{m.nome}</span>
                  <span className="cell-email">{m.email}</span>
                </td>
                <td>{m.horas} horas</td>
                <td>{m.funcao}</td>
                <td>{formatDate(m.dataCadastro)}</td>
                <td><span className={`status-badge ${m.status.toLowerCase()}`}>{m.status}</span></td>
                <td>
                  <div className="action-icons">
                    <span className="action-icon" title="Info">Ôä╣</span>
                    <span className="action-icon" title="Excluir">­ƒùæ</span>
                  </div>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}

/* ===== AUTH & DATA LOADING ===== */
async function ensureUserAndLogin() {
  const registerResp = await fetch(`${API_BASE}/auth/registro`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    credentials: "include",
    body: JSON.stringify(AUTH_USER),
  });

  if (!registerResp.ok && registerResp.status !== 409) {
    throw new Error(`Falha ao registrar usuario (${registerResp.status})`);
  }

  const loginResp = await fetch(`${API_BASE}/auth/login`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    credentials: "include",
    body: JSON.stringify({ username: AUTH_USER.username, password: AUTH_USER.password }),
  });

  if (!loginResp.ok) {
    throw new Error(`Falha no login (${loginResp.status})`);
  }
}

async function loadProtectedData() {
  const [pResp, kResp, mResp] = await Promise.all([
    fetch(`${API_BASE}/pacientes`, { credentials: "include" }),
    fetch(`${API_BASE}/kits`, { credentials: "include" }),
    fetch(`${API_BASE}/madrinhas`, { credentials: "include" }),
  ]);

  if (!pResp.ok || !kResp.ok) {
    throw new Error(`Nao autenticado ou backend indisponivel: pacientes=${pResp.status}, kits=${kResp.status}`);
  }

  const [rawP, rawK] = await Promise.all([pResp.json(), kResp.json()]);
  const rawM = mResp.ok ? await mResp.json() : [];

  return {
    pacientes: Array.isArray(rawP) ? rawP.map(normalizePaciente) : [],
    envios: Array.isArray(rawK) ? rawK.map(normalizeKit) : [],
    madrinhas: Array.isArray(rawM) ? rawM.map(normalizeMadrinha) : [],
  };
}

/* ===== MAIN APP ===== */
function App() {
  const [tab, setTab] = useState("painel");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [usingMock, setUsingMock] = useState(false);
  const [pacientes, setPacientes] = useState(MOCK_PACIENTES);
  const [envios, setEnvios] = useState(MOCK_ENVIOS);
  const [madrinhas, setMadrinhas] = useState(MOCK_MADRINHAS);

  async function load() {
    setLoading(true);
    setError("");
    try {
      await ensureUserAndLogin();
      const data = await loadProtectedData();
      setPacientes(data.pacientes.length ? data.pacientes : MOCK_PACIENTES);
      setEnvios(data.envios.length ? data.envios : MOCK_ENVIOS);
      setMadrinhas(data.madrinhas.length ? data.madrinhas : MOCK_MADRINHAS);
      setUsingMock(!data.pacientes.length || !data.envios.length);
    } catch (e) {
      setUsingMock(true);
      setPacientes(MOCK_PACIENTES);
      setEnvios(MOCK_ENVIOS);
      setMadrinhas(MOCK_MADRINHAS);
      setError(`${e.message}. Exibindo dados de demonstra├º├úo.`);
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => { load(); }, []);

  function handleExport() {
    let rows, filename, title;
    if (tab === "painel") { rows = envios; filename = "envios"; title = "Relat├│rio de Envios"; }
    else if (tab === "pacientes") { rows = pacientes; filename = "pacientes"; title = "Relat├│rio de Pacientes"; }
    else { rows = madrinhas; filename = "madrinhas"; title = "Relat├│rio de Madrinhas"; }
    exportXlsx(filename, rows);
  }

  return (
    <div>
      {/* NAVBAR */}
      <nav className="navbar">
        <div className="navbar-logo">
          <span className="logo-icon">ÔÖÑ</span>
          <div className="logo-text">
            <span className="brand-name">Amor em Mechas</span>
            <span className="brand-tagline">Transformando Vidas</span>
          </div>
        </div>

        <ul className="navbar-links">
          <li className={tab === "painel" ? "active" : ""} onClick={() => setTab("painel")}>Painel</li>
          <li className={tab === "pacientes" ? "active" : ""} onClick={() => setTab("pacientes")}>Pacientes</li>
          <li className={tab === "madrinhas" ? "active" : ""} onClick={() => setTab("madrinhas")}>Madrinhas</li>
        </ul>

        <div className="navbar-actions">
          {tab === "madrinhas" && (
            <button className="btn-pink">Ô£Å´©Å Cadastrar Madrinha</button>
          )}
          <button className="btn-pink" onClick={handleExport}>
            Ô¼ç Exportar Dados
          </button>
        </div>
      </nav>

      {/* CONTENT */}
      {loading && <div className="page-content"><p>Carregando...</p></div>}
      {!loading && tab === "painel" && <PainelEnvios envios={envios} pacientes={pacientes} error={error} usingMock={usingMock} />}
      {!loading && tab === "pacientes" && <PainelPacientes pacientes={pacientes} error={error} usingMock={usingMock} />}
      {!loading && tab === "madrinhas" && <PainelMadrinhas madrinhas={madrinhas} error={error} usingMock={usingMock} />}
    </div>
  );
}

ReactDOM.createRoot(document.getElementById("root")).render(<App />);
