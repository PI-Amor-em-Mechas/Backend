-- MySQL Workbench Forward Engineering

SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0;
SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0;
SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_ENGINE_SUBSTITUTION';

-- -----------------------------------------------------
-- Schema mydb
-- -----------------------------------------------------
-- -----------------------------------------------------
-- Schema amor_em_mechas
-- -----------------------------------------------------

-- -----------------------------------------------------
-- Schema amor_em_mechas
-- -----------------------------------------------------
CREATE SCHEMA IF NOT EXISTS `amor_em_mechas` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci ;
USE `amor_em_mechas` ;

-- -----------------------------------------------------
-- Table `amor_em_mechas`.`arquivo`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `amor_em_mechas`.`arquivo` (
  `id` INT NOT NULL AUTO_INCREMENT,
  `conteudo` LONGBLOB NULL DEFAULT NULL,
  `mime_type` VARCHAR(255) NULL DEFAULT NULL,
  `nome` VARCHAR(255) NULL DEFAULT NULL,
  `nome_original` VARCHAR(255) NULL DEFAULT NULL,
  `tamanho` BIGINT NULL DEFAULT NULL,
  `tipo` VARCHAR(255) NULL DEFAULT NULL,
  `caminho_arquivo` VARCHAR(255) NULL DEFAULT NULL,
  PRIMARY KEY (`id`))
ENGINE = InnoDB
AUTO_INCREMENT = 3
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_0900_ai_ci;


-- -----------------------------------------------------
-- Table `amor_em_mechas`.`solicitante`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `amor_em_mechas`.`solicitante` (
  `id` INT NOT NULL AUTO_INCREMENT,
  `nome_completo` VARCHAR(255) NULL DEFAULT NULL,
  `rg` VARCHAR(255) NULL DEFAULT NULL,
  PRIMARY KEY (`id`))
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_0900_ai_ci;


-- -----------------------------------------------------
-- Table `amor_em_mechas`.`avaliacao`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `amor_em_mechas`.`avaliacao` (
  `id` INT NOT NULL AUTO_INCREMENT,
  `concluido` BIT(1) NULL DEFAULT NULL,
  `consentimento` BIT(1) NULL DEFAULT NULL,
  `dt_conclusao` DATE NULL DEFAULT NULL,
  `nota_formulario` INT NULL DEFAULT NULL,
  `solicitante_id` INT NULL DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE INDEX `UKdgtjkjj26htth067y5wpsmfeq` (`solicitante_id` ASC) VISIBLE,
  CONSTRAINT `FKik355cl232fcks13d706l9w5l`
    FOREIGN KEY (`solicitante_id`)
    REFERENCES `amor_em_mechas`.`solicitante` (`id`))
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_0900_ai_ci;


-- -----------------------------------------------------
-- Table `amor_em_mechas`.`endereco`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `amor_em_mechas`.`endereco` (
  `id` INT NOT NULL AUTO_INCREMENT,
  `bairro` VARCHAR(255) NULL DEFAULT NULL,
  `cep` VARCHAR(255) NULL DEFAULT NULL,
  `cidade` VARCHAR(255) NULL DEFAULT NULL,
  `estado` VARCHAR(255) NULL DEFAULT NULL,
  `numero` VARCHAR(255) NULL DEFAULT NULL,
  `rua` VARCHAR(255) NULL DEFAULT NULL,
  PRIMARY KEY (`id`))
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_0900_ai_ci;


-- -----------------------------------------------------
-- Table `amor_em_mechas`.`paciente`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `amor_em_mechas`.`paciente` (
  `id` INT NOT NULL AUTO_INCREMENT,
  `cel` VARCHAR(255) NULL DEFAULT NULL,
  `cpf` VARCHAR(255) NULL DEFAULT NULL,
  `dt_nasc` DATE NULL DEFAULT NULL,
  `dt_pedido` DATE NULL DEFAULT NULL,
  `email` VARCHAR(255) NULL DEFAULT NULL,
  `estado_civil` VARCHAR(255) NULL DEFAULT NULL,
  `nome_completo` VARCHAR(255) NULL DEFAULT NULL,
  `qtd_filhos` INT NULL DEFAULT NULL,
  `qtd_pessoas_em_casa` INT NULL DEFAULT NULL,
  `tem_filhos` BIT(1) NULL DEFAULT NULL,
  `cabelo_antes_id` INT NULL DEFAULT NULL,
  `endereco_id` INT NULL DEFAULT NULL,
  `solicitante_id` INT NULL DEFAULT NULL,
  `consentimento_lgpd` BIT(1) NOT NULL,
  `dados_anonimizados` BIT(1) NOT NULL,
  `dt_anonimizacao` DATETIME(6) NULL DEFAULT NULL,
  `dt_consentimento` DATETIME(6) NULL DEFAULT NULL,
  `finalidade_tratamento` VARCHAR(255) NULL DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE INDEX `UKialsxdnnfqwk6h0g9tkbrb1wg` (`cabelo_antes_id` ASC) VISIBLE,
  UNIQUE INDEX `UKt03xff2qg2dnq4vd538qt9vsc` (`endereco_id` ASC) VISIBLE,
  INDEX `FKt3nj1qb6lo2cnvb96j77inb1n` (`solicitante_id` ASC) VISIBLE,
  CONSTRAINT `FK40pxa17u1gl9s2lu3i78lnay`
    FOREIGN KEY (`cabelo_antes_id`)
    REFERENCES `amor_em_mechas`.`arquivo` (`id`),
  CONSTRAINT `FKpus64vtl67yxnw2kimogd7abx`
    FOREIGN KEY (`endereco_id`)
    REFERENCES `amor_em_mechas`.`endereco` (`id`),
  CONSTRAINT `FKt3nj1qb6lo2cnvb96j77inb1n`
    FOREIGN KEY (`solicitante_id`)
    REFERENCES `amor_em_mechas`.`solicitante` (`id`))
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_0900_ai_ci;


-- -----------------------------------------------------
-- Table `amor_em_mechas`.`dados_medicos`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `amor_em_mechas`.`dados_medicos` (
  `id` INT NOT NULL AUTO_INCREMENT,
  `dt_inicio_tratamento` DATE NULL DEFAULT NULL,
  `justificativa` VARCHAR(255) NULL DEFAULT NULL,
  `motivo` VARCHAR(255) NULL DEFAULT NULL,
  `tipo_atendimento` VARCHAR(255) NULL DEFAULT NULL,
  `tipo_cancer` VARCHAR(255) NULL DEFAULT NULL,
  `arquivo_id` INT NULL DEFAULT NULL,
  `paciente_id` INT NULL DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE INDEX `UKa332yjksf9g59umesfr8hd6tr` (`arquivo_id` ASC) VISIBLE,
  INDEX `FK7yoo0hxyrjil3tjj0ngucd81w` (`paciente_id` ASC) VISIBLE,
  CONSTRAINT `FK7yoo0hxyrjil3tjj0ngucd81w`
    FOREIGN KEY (`paciente_id`)
    REFERENCES `amor_em_mechas`.`paciente` (`id`),
  CONSTRAINT `FKfo42boy8v5m7by4iw2840l6it`
    FOREIGN KEY (`arquivo_id`)
    REFERENCES `amor_em_mechas`.`arquivo` (`id`))
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_0900_ai_ci;


-- -----------------------------------------------------
-- Table `amor_em_mechas`.`filho`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `amor_em_mechas`.`filho` (
  `id` INT NOT NULL AUTO_INCREMENT,
  `idade` INT NULL DEFAULT NULL,
  `paciente_id` INT NULL DEFAULT NULL,
  PRIMARY KEY (`id`),
  INDEX `FKa0uib8bmaxux8onrctrp2iw6m` (`paciente_id` ASC) VISIBLE,
  CONSTRAINT `FKa0uib8bmaxux8onrctrp2iw6m`
    FOREIGN KEY (`paciente_id`)
    REFERENCES `amor_em_mechas`.`paciente` (`id`))
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_0900_ai_ci;


-- -----------------------------------------------------
-- Table `amor_em_mechas`.`hl7_audit_log`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `amor_em_mechas`.`hl7_audit_log` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `detalhes` VARCHAR(1000) NULL DEFAULT NULL,
  `endereco_ip` VARCHAR(45) NULL DEFAULT NULL,
  `metodo_http` VARCHAR(50) NULL DEFAULT NULL,
  `recurso` VARCHAR(255) NOT NULL,
  `status_resposta` VARCHAR(20) NULL DEFAULT NULL,
  `sucesso` BIT(1) NOT NULL,
  `timestamp` DATETIME(6) NOT NULL,
  `tipo_evento` ENUM('ACESSO_DADOS_MEDICOS', 'ACESSO_NEGADO', 'ACESSO_PACIENTE', 'ALTERACAO_LAUDO', 'ALTERACAO_PACIENTE', 'CRIACAO_LAUDO', 'DOWNLOAD_ARQUIVO', 'EXCLUSAO_LAUDO', 'LOGIN', 'LOGIN_FALHA', 'LOGOUT', 'TOKEN_EXPIRADO', 'TOKEN_INVALIDO', 'UPLOAD_ARQUIVO') NOT NULL,
  `user_agent` VARCHAR(500) NULL DEFAULT NULL,
  `usuario` VARCHAR(100) NOT NULL,
  PRIMARY KEY (`id`),
  INDEX `idx_audit_timestamp` (`timestamp` ASC) VISIBLE,
  INDEX `idx_audit_usuario` (`usuario` ASC) VISIBLE,
  INDEX `idx_audit_recurso` (`recurso` ASC) VISIBLE,
  INDEX `idx_audit_evento` (`tipo_evento` ASC) VISIBLE)
ENGINE = InnoDB
AUTO_INCREMENT = 10
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_0900_ai_ci;


-- -----------------------------------------------------
-- Table `amor_em_mechas`.`kit_amor`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `amor_em_mechas`.`kit_amor` (
  `id` INT NOT NULL AUTO_INCREMENT,
  `cor_peruca` VARCHAR(255) NULL DEFAULT NULL,
  `paciente_id` INT NULL DEFAULT NULL,
  `solicitante_id` INT NULL DEFAULT NULL,
  PRIMARY KEY (`id`),
  INDEX `FK7f2hlq2xv8a1bki8ysln2krq` (`paciente_id` ASC) VISIBLE,
  INDEX `FK8cs20g98n8cycf83otxswjv1e` (`solicitante_id` ASC) VISIBLE,
  CONSTRAINT `FK7f2hlq2xv8a1bki8ysln2krq`
    FOREIGN KEY (`paciente_id`)
    REFERENCES `amor_em_mechas`.`paciente` (`id`),
  CONSTRAINT `FK8cs20g98n8cycf83otxswjv1e`
    FOREIGN KEY (`solicitante_id`)
    REFERENCES `amor_em_mechas`.`solicitante` (`id`))
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_0900_ai_ci;


-- -----------------------------------------------------
-- Table `amor_em_mechas`.`madrinha`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `amor_em_mechas`.`madrinha` (
  `id` INT NOT NULL AUTO_INCREMENT,
  `data_cadastro` DATE NULL DEFAULT NULL,
  `email` VARCHAR(255) NULL DEFAULT NULL,
  `funcao` VARCHAR(255) NULL DEFAULT NULL,
  `horas_voluntarias` INT NULL DEFAULT NULL,
  `nome_completo` VARCHAR(255) NULL DEFAULT NULL,
  `status` VARCHAR(255) NULL DEFAULT NULL,
  PRIMARY KEY (`id`))
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_0900_ai_ci;


-- -----------------------------------------------------
-- Table `amor_em_mechas`.`usuario`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `amor_em_mechas`.`usuario` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `password` VARCHAR(255) NOT NULL,
  `role` VARCHAR(255) NOT NULL,
  `username` VARCHAR(255) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE INDEX `UK863n1y3x0jalatoir4325ehal` (`username` ASC) VISIBLE)
ENGINE = InnoDB
AUTO_INCREMENT = 4
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_0900_ai_ci;


SET SQL_MODE=@OLD_SQL_MODE;
SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS;
SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS;
