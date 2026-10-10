ALTER TABLE arquivo
    ADD COLUMN IF NOT EXISTS status_validacao VARCHAR(20) NOT NULL DEFAULT 'PENDENTE',
    ADD COLUMN IF NOT EXISTS validado_por VARCHAR(100) NULL,
    ADD COLUMN IF NOT EXISTS validado_em DATETIME(6) NULL,
    ADD COLUMN IF NOT EXISTS observacao_validacao VARCHAR(1000) NULL;

UPDATE arquivo
SET status_validacao = 'PENDENTE'
WHERE status_validacao IS NULL
   OR status_validacao NOT IN ('PENDENTE', 'APROVADO', 'REJEITADO');