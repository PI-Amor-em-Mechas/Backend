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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS kit_do_amor (
    id_kit         INT          PRIMARY KEY AUTO_INCREMENT,
    cor_peruca     VARCHAR(45)  NOT NULL,
    solicitante_id INT          NOT NULL,
    FOREIGN KEY (solicitante_id) REFERENCES solicitante (id_solicitante)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

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
    FOREIGN KEY (endereco_id)      REFERENCES endereco     (id_endereco),
    FOREIGN KEY (kit_id)           REFERENCES kit_do_amor  (id_kit),
    FOREIGN KEY (dados_medicos_id) REFERENCES dados_medicos (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS filho (
    id_filho   INT PRIMARY KEY AUTO_INCREMENT,
    paciente_id INT,
    idade       INT NOT NULL,
    FOREIGN KEY (paciente_id) REFERENCES paciente (id_paciente)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS avaliacao (
    id_avaliacao   INT      PRIMARY KEY AUTO_INCREMENT,
    nota_avaliacao DATE     NOT NULL,
    concentimento  TINYINT(1) NOT NULL,
    concluido      TINYINT(1) NOT NULL,
    data_conclusao DATETIME DEFAULT CURRENT_TIMESTAMP,
    solicitante_id INT,
    FOREIGN KEY (solicitante_id) REFERENCES solicitante (id_solicitante)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


CREATE TABLE IF NOT EXISTS colaborador (
    id              VARCHAR(50)  PRIMARY KEY,
    nome            VARCHAR(255) NOT NULL,
    criado_em       DATETIME     NOT NULL,
    anonimizado_em  DATETIME     NULL,
    KEY idx_colaborador_nome (nome)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS madrinha (
    id_madrinha    INT          PRIMARY KEY AUTO_INCREMENT,
    nome_completo  VARCHAR(255) NOT NULL,
    data_inicio    DATE         NOT NULL,
    imagem_facial  LONGBLOB     NOT NULL,
    -- Vinculo com o sistema de reconhecimento facial (opcional)
    colaborador_id VARCHAR(50)  NULL,
    FOREIGN KEY (colaborador_id) REFERENCES colaborador (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------------------------------------------
-- SISTEMA DE RECONHECIMENTO FACIAL E PONTO DE MADRINHAS
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS embedding_facial (
    id             INT         PRIMARY KEY AUTO_INCREMENT,
    colaborador_id VARCHAR(50) NOT NULL,
    vetor          LONGBLOB    NOT NULL,   
    dimensao       INT         NOT NULL,   
    dtype          VARCHAR(16) NOT NULL,   
    criado_em      DATETIME    NOT NULL,
    FOREIGN KEY (colaborador_id) REFERENCES colaborador (id),
    KEY idx_emb_colaborador (colaborador_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS ponto (
    id              INT          PRIMARY KEY AUTO_INCREMENT,
    colaborador_id  VARCHAR(50)  NOT NULL,
    data_hora       DATETIME     NOT NULL,
    tipo            ENUM('IN', 'OUT') NOT NULL,
    confianca       DOUBLE       NOT NULL,   
    caminho_imagem  VARCHAR(512) NULL,       
    FOREIGN KEY (colaborador_id) REFERENCES colaborador (id),
    KEY idx_ponto_colaborador_data (colaborador_id, data_hora)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS consentimento (
    id              INT          PRIMARY KEY AUTO_INCREMENT,
    colaborador_id  VARCHAR(50)  NOT NULL,
    versao          VARCHAR(20)  NOT NULL,
    consentido_em   DATETIME     NOT NULL,
    revogado_em     DATETIME     NULL,
    user_agent      TEXT         NULL,
    ip              VARCHAR(45)  NULL,    
    FOREIGN KEY (colaborador_id) REFERENCES colaborador (id),
    KEY idx_consentimento_colaborador (colaborador_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS comando_voz (
    id              INT         PRIMARY KEY AUTO_INCREMENT,
    colaborador_id  VARCHAR(50) NOT NULL,
    texto           TEXT        NOT NULL,
    criado_em       DATETIME    NOT NULL,
    FOREIGN KEY (colaborador_id) REFERENCES colaborador (id),
    KEY idx_comando_voz_colaborador (colaborador_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Voiceprints (Resemblyzer / GE2E, 256-D float32) — 2o fator pos-face.
CREATE TABLE IF NOT EXISTS embedding_voz (
    id             INT         PRIMARY KEY AUTO_INCREMENT,
    colaborador_id VARCHAR(50) NOT NULL,
    vetor          LONGBLOB    NOT NULL,
    dimensao       INT         NOT NULL,
    dtype          VARCHAR(16) NOT NULL,
    criado_em      DATETIME    NOT NULL,
    FOREIGN KEY (colaborador_id) REFERENCES colaborador (id),
    KEY idx_emb_voz_colaborador (colaborador_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS log_auditoria (
    id        INT          PRIMARY KEY AUTO_INCREMENT,
    data_hora DATETIME     NOT NULL,
    autor     VARCHAR(50)  NOT NULL,   -- perfil/usuario que executou a acao
    acao      VARCHAR(100) NOT NULL,   -- ex.: 'auth.login', 'colaborador.delete'
    alvo      VARCHAR(50)  NULL,       -- colaborador_id afetado, se aplicavel
    ip        VARCHAR(45)  NULL,
    detalhes  TEXT         NULL,       -- JSON com contexto adicional
    KEY idx_log_auditoria_data (data_hora)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
