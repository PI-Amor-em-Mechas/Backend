-- =============================================================================
-- Amor em Mechas — Schema MariaDB (>= 10.4)
-- Inclui: dominio de negocio (pacientes, madrinhas, kits) +
--         sistema de reconhecimento facial e ponto de madrinhas
--
-- Diferencas em relacao a versao MySQL (data/schema.sql):
--   - ROW_FORMAT=DYNAMIC explicito (recomendado em MariaDB com utf8mb4)
--   - Constraints com nome explicito (fk_*, chk_*) para facilitar DROP/ALTER
--   - Uso de CHECK constraints (suportado desde MariaDB 10.2)
--   - Acoes ON DELETE/UPDATE explicitas nas FKs
-- =============================================================================

CREATE DATABASE IF NOT EXISTS amor_em_mechas
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE amor_em_mechas;

-- -----------------------------------------------------------------------------
-- DOMINIO DE NEGOCIO
-- -----------------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS solicitante (
    id_solicitante INT          PRIMARY KEY AUTO_INCREMENT,
    nome_completo  VARCHAR(255) NOT NULL,
    rg             CHAR(9)      NOT NULL
) ENGINE=InnoDB ROW_FORMAT=DYNAMIC DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS kit_do_amor (
    id_kit         INT          PRIMARY KEY AUTO_INCREMENT,
    cor_peruca     VARCHAR(45)  NOT NULL,
    solicitante_id INT          NOT NULL,
    CONSTRAINT fk_kit_solicitante
        FOREIGN KEY (solicitante_id) REFERENCES solicitante (id_solicitante)
        ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB ROW_FORMAT=DYNAMIC DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS endereco (
    id_endereco INT          PRIMARY KEY AUTO_INCREMENT,
    rua         VARCHAR(255) NOT NULL,
    numero      VARCHAR(4)   NOT NULL,
    bairro      VARCHAR(255) NOT NULL,
    cidade      VARCHAR(255) NOT NULL,
    estado      ENUM(
        'Acre', 'Alagoas', 'Amapá', 'Amazonas', 'Bahia', 'Ceará',
        'Distrito Federal', 'Espírito Santo', 'Goiás', 'Maranhão',
        'Mato Grosso', 'Mato Grosso do Sul', 'Minas Gerais', 'Pará',
        'Paraíba', 'Paraná', 'Pernambuco', 'Piauí', 'Rio de Janeiro',
        'Rio Grande do Norte', 'Rio Grande do Sul', 'Rondônia', 'Roraima',
        'Santa Catarina', 'São Paulo', 'Sergipe', 'Tocantins'
    ) NOT NULL,
    cep         CHAR(8)      NOT NULL
) ENGINE=InnoDB ROW_FORMAT=DYNAMIC DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS dados_medicos (
    id                 INT          PRIMARY KEY AUTO_INCREMENT,
    motivo             ENUM(
        'Tratamento quimioterápico',
        'Alopecia Areata',
        'Outros'
    ) NOT NULL,
    tipo_cancer        ENUM(
        'Mama',
        'Ovário',
        'Leucemia',
        'Outro'
    ) NOT NULL,
    justificativa      VARCHAR(255) NOT NULL,
    inicio_tratamento  DATE         NOT NULL,
    tipo_atendimento   ENUM(
        'Público - SUS',
        'Convênio',
        'Particular',
        'Outro'
    ) NOT NULL,
    relatorio_medico   LONGBLOB     NOT NULL
) ENGINE=InnoDB ROW_FORMAT=DYNAMIC DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS paciente (
    id_paciente       INT          PRIMARY KEY AUTO_INCREMENT,
    data_pedido       DATE         NOT NULL,
    email             VARCHAR(255) NOT NULL,
    nome_completo     VARCHAR(255) NOT NULL,
    celular           CHAR(11)     NOT NULL,
    data_nascimento   DATE         NOT NULL,
    estado_civil      ENUM(
        'Solteiro(a)',
        'Casado(a)',
        'Divorciado(a)',
        'Separado(a)',
        'Viúvo(a)'
    ) NOT NULL,
    qtd_filhos        INT          NOT NULL,
    qtd_pessoas_casa  INT          NOT NULL,
    cpf               VARCHAR(11)  NOT NULL,
    cabelo_antes      LONGBLOB     NOT NULL,
    endereco_id       INT,
    kit_id            INT,
    dados_medicos_id  INT,
    CONSTRAINT chk_paciente_qtd_filhos       CHECK (qtd_filhos >= 0),
    CONSTRAINT chk_paciente_qtd_pessoas_casa CHECK (qtd_pessoas_casa >= 0),
    CONSTRAINT fk_paciente_endereco
        FOREIGN KEY (endereco_id)      REFERENCES endereco      (id_endereco)
        ON DELETE SET NULL ON UPDATE CASCADE,
    CONSTRAINT fk_paciente_kit
        FOREIGN KEY (kit_id)           REFERENCES kit_do_amor   (id_kit)
        ON DELETE SET NULL ON UPDATE CASCADE,
    CONSTRAINT fk_paciente_dados_medicos
        FOREIGN KEY (dados_medicos_id) REFERENCES dados_medicos (id)
        ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB ROW_FORMAT=DYNAMIC DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS filho (
    id_filho    INT PRIMARY KEY AUTO_INCREMENT,
    paciente_id INT,
    idade       INT NOT NULL,
    CONSTRAINT chk_filho_idade CHECK (idade >= 0),
    CONSTRAINT fk_filho_paciente
        FOREIGN KEY (paciente_id) REFERENCES paciente (id_paciente)
        ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB ROW_FORMAT=DYNAMIC DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS avaliacao (
    id_avaliacao   INT        PRIMARY KEY AUTO_INCREMENT,
    nota_avaliacao DATE       NOT NULL,
    concentimento  TINYINT(1) NOT NULL,
    concluido      TINYINT(1) NOT NULL,
    data_conclusao DATETIME   DEFAULT CURRENT_TIMESTAMP,
    solicitante_id INT,
    CONSTRAINT fk_avaliacao_solicitante
        FOREIGN KEY (solicitante_id) REFERENCES solicitante (id_solicitante)
        ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB ROW_FORMAT=DYNAMIC DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Colaboradoras/madrinhas cadastradas no sistema biometrico.
-- id: identificador textual (ex.: CPF, matricula, uuid).
CREATE TABLE IF NOT EXISTS colaborador (
    id              VARCHAR(50)  PRIMARY KEY,
    nome            VARCHAR(255) NOT NULL,
    criado_em       DATETIME     NOT NULL,
    anonimizado_em  DATETIME     NULL,
    KEY idx_colaborador_nome (nome)
) ENGINE=InnoDB ROW_FORMAT=DYNAMIC DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Madrinha: voluntaria que doa cabelo / presta servicos.
-- colaborador_id referencia o cadastro biometrico no sistema de ponto.
CREATE TABLE IF NOT EXISTS madrinha (
    id_madrinha    INT          PRIMARY KEY AUTO_INCREMENT,
    nome_completo  VARCHAR(255) NOT NULL,
    data_inicio    DATE         NOT NULL,
    imagem_facial  LONGBLOB     NOT NULL,
    colaborador_id VARCHAR(50)  NULL,
    CONSTRAINT fk_madrinha_colaborador
        FOREIGN KEY (colaborador_id) REFERENCES colaborador (id)
        ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB ROW_FORMAT=DYNAMIC DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS usuario (
    idUsuario INT          PRIMARY KEY AUTO_INCREMENT,
    username  VARCHAR(255),
    password  VARCHAR(255),
    role      ENUM(
        'Diretora Presidente',
        'Diretora Financeira',
        'Diretora Executiva',
        'Coordenadora de Projetos',
        'Coordenadora da Administração'
    ) NOT NULL
) ENGINE=InnoDB ROW_FORMAT=DYNAMIC DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------------------------------------------
-- SISTEMA DE RECONHECIMENTO FACIAL E PONTO DE MADRINHAS
-- -----------------------------------------------------------------------------

-- Embeddings faciais SFace (128 dimensoes, float32 serializado).
CREATE TABLE IF NOT EXISTS embedding_facial (
    id             INT         PRIMARY KEY AUTO_INCREMENT,
    colaborador_id VARCHAR(50) NOT NULL,
    vetor          LONGBLOB    NOT NULL,   -- float32 serializado (128 * 4 bytes)
    dimensao       INT         NOT NULL,   -- dimensao do vetor (128)
    dtype          VARCHAR(16) NOT NULL,   -- 'float32'
    criado_em      DATETIME    NOT NULL,
    CONSTRAINT chk_embedding_dimensao CHECK (dimensao > 0),
    CONSTRAINT fk_embedding_colaborador
        FOREIGN KEY (colaborador_id) REFERENCES colaborador (id)
        ON DELETE CASCADE ON UPDATE CASCADE,
    KEY idx_emb_colaborador (colaborador_id)
) ENGINE=InnoDB ROW_FORMAT=DYNAMIC DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Registros de ponto (entrada/saida) gerados apos confirmacao biometrica.
CREATE TABLE IF NOT EXISTS ponto (
    id              INT          PRIMARY KEY AUTO_INCREMENT,
    colaborador_id  VARCHAR(50)  NOT NULL,
    data_hora       DATETIME     NOT NULL,
    tipo            ENUM('IN', 'OUT') NOT NULL,
    confianca       DOUBLE       NOT NULL,   -- score de cosseno SFace [0, 1]
    caminho_imagem  VARCHAR(512) NULL,
    CONSTRAINT chk_ponto_confianca CHECK (confianca BETWEEN 0 AND 1),
    CONSTRAINT fk_ponto_colaborador
        FOREIGN KEY (colaborador_id) REFERENCES colaborador (id)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    KEY idx_ponto_colaborador_data (colaborador_id, data_hora)
) ENGINE=InnoDB ROW_FORMAT=DYNAMIC DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Consentimento LGPD por colaboradora e versao do termo.
CREATE TABLE IF NOT EXISTS consentimento (
    id              INT          PRIMARY KEY AUTO_INCREMENT,
    colaborador_id  VARCHAR(50)  NOT NULL,
    versao          VARCHAR(20)  NOT NULL,
    consentido_em   DATETIME     NOT NULL,
    revogado_em     DATETIME     NULL,
    user_agent      TEXT         NULL,
    ip              VARCHAR(45)  NULL,    -- suporta IPv6
    CONSTRAINT fk_consentimento_colaborador
        FOREIGN KEY (colaborador_id) REFERENCES colaborador (id)
        ON DELETE CASCADE ON UPDATE CASCADE,
    KEY idx_consentimento_colaborador (colaborador_id)
) ENGINE=InnoDB ROW_FORMAT=DYNAMIC DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Comandos de voz registrados apos reconhecimento (palavra-chave "salvar").
CREATE TABLE IF NOT EXISTS comando_voz (
    id              INT         PRIMARY KEY AUTO_INCREMENT,
    colaborador_id  VARCHAR(50) NOT NULL,
    texto           TEXT        NOT NULL,
    criado_em       DATETIME    NOT NULL,
    CONSTRAINT fk_comando_voz_colaborador
        FOREIGN KEY (colaborador_id) REFERENCES colaborador (id)
        ON DELETE CASCADE ON UPDATE CASCADE,
    KEY idx_comando_voz_colaborador (colaborador_id)
) ENGINE=InnoDB ROW_FORMAT=DYNAMIC DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Trilha de auditoria para acoes sensiveis (login, cadastro, exclusao, LGPD).
CREATE TABLE IF NOT EXISTS log_auditoria (
    id        INT          PRIMARY KEY AUTO_INCREMENT,
    data_hora DATETIME     NOT NULL,
    autor     VARCHAR(50)  NOT NULL,   -- perfil/usuario que executou a acao
    acao      VARCHAR(100) NOT NULL,   -- ex.: 'auth.login', 'colaborador.delete'
    alvo      VARCHAR(50)  NULL,       -- colaborador_id afetado, se aplicavel
    ip        VARCHAR(45)  NULL,
    detalhes  TEXT         NULL,       -- JSON com contexto adicional
    KEY idx_log_auditoria_data (data_hora)
) ENGINE=InnoDB ROW_FORMAT=DYNAMIC DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
