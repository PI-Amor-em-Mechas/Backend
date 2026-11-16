-- PROPOSTA NÃO EXECUTADA
-- Motivo: a entidade Arquivo usa data_upload, mas a tabela Azure não possui a coluna.
-- Esta proposta é deliberadamente separada do schema base e não é uma migration
-- automaticamente descoberta pelo Spring.
--
-- Pré-condições:
--   1. validar previamente o inventário completo JPA x Azure;
--   2. obter aprovação explícita para alterar o banco;
--   3. confirmar que a tabela arquivo continua sem a coluna;
--   4. confirmar que não há registros que exijam backfill.
--
-- A coluna é nullable porque o campo Java não declara nullable=false e a tabela
-- pode receber registros existentes antes desta mudança. Nenhum default ou
-- backfill fictício é introduzido.

USE `amor_em_mechas`;

SELECT
    TABLE_NAME,
    COLUMN_NAME,
    COLUMN_TYPE,
    IS_NULLABLE,
    COLUMN_DEFAULT,
    EXTRA
FROM information_schema.columns
WHERE table_schema = 'amor_em_mechas'
  AND table_name = 'arquivo'
  AND column_name = 'data_upload';

SELECT COUNT(*) AS arquivo_count
FROM `arquivo`;

ALTER TABLE `arquivo`
    ADD COLUMN IF NOT EXISTS `data_upload` DATETIME(6) NULL DEFAULT NULL;

SELECT
    COLUMN_NAME,
    COLUMN_TYPE,
    IS_NULLABLE,
    COLUMN_DEFAULT,
    EXTRA
FROM information_schema.columns
WHERE table_schema = 'amor_em_mechas'
  AND table_name = 'arquivo'
  AND column_name = 'data_upload';

-- Rollback, somente se a coluna estiver vazia e após validação explícita:
-- ALTER TABLE `arquivo` DROP COLUMN `data_upload`;
