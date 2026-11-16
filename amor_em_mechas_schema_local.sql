/*M!999999\- enable the sandbox mode */ 

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*M!100616 SET @OLD_NOTE_VERBOSITY=@@NOTE_VERBOSITY, NOTE_VERBOSITY=0 */;
DROP TABLE IF EXISTS `arquivo`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `arquivo` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `caminho_arquivo` varchar(255) DEFAULT NULL,
  `mime_type` varchar(255) DEFAULT NULL,
  `nome` varchar(255) DEFAULT NULL,
  `nome_original` varchar(255) DEFAULT NULL,
  `tamanho` bigint(20) DEFAULT NULL,
  `tipo` varchar(255) DEFAULT NULL,
  `status_validacao` varchar(20) NOT NULL DEFAULT 'PENDENTE',
  `validado_por` varchar(100) DEFAULT NULL,
  `validado_em` datetime(6) DEFAULT NULL,
  `observacao_validacao` varchar(1000) DEFAULT NULL,
  `comparacao_campos` text DEFAULT NULL,
  `paciente_id` int(11) DEFAULT NULL,
  `status_comparacao` enum('AGUARDANDO','CONCLUIDA','FALHA','NAO_SOLICITADA','PROCESSANDO') NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `avaliacao`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `avaliacao` (
  `id_avaliacao` int(11) NOT NULL AUTO_INCREMENT,
  `nota_avaliacao` date NOT NULL,
  `concentimento` tinyint(1) NOT NULL,
  `concluido` tinyint(1) NOT NULL,
  `data_conclusao` datetime DEFAULT current_timestamp(),
  `solicitante_id` int(11) DEFAULT NULL,
  `consentimento` bit(1) DEFAULT NULL,
  `dt_conclusao` date DEFAULT NULL,
  `nota_formulario` int(11) DEFAULT NULL,
  PRIMARY KEY (`id_avaliacao`),
  KEY `fk_avaliacao_solicitante` (`solicitante_id`),
  CONSTRAINT `fk_avaliacao_solicitante` FOREIGN KEY (`solicitante_id`) REFERENCES `solicitante` (`id_solicitante`) ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci ROW_FORMAT=DYNAMIC;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `colaborador`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `colaborador` (
  `id` varchar(50) NOT NULL,
  `nome` varchar(255) NOT NULL,
  `criado_em` datetime NOT NULL,
  `anonimizado_em` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_colaborador_nome` (`nome`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci ROW_FORMAT=DYNAMIC;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `comando_voz`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `comando_voz` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `colaborador_id` varchar(50) NOT NULL,
  `texto` text NOT NULL,
  `criado_em` datetime NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_comando_voz_colaborador` (`colaborador_id`),
  CONSTRAINT `fk_comando_voz_colaborador` FOREIGN KEY (`colaborador_id`) REFERENCES `colaborador` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci ROW_FORMAT=DYNAMIC;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `consentimento`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `consentimento` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `colaborador_id` varchar(50) NOT NULL,
  `versao` varchar(20) NOT NULL,
  `consentido_em` datetime NOT NULL,
  `revogado_em` datetime DEFAULT NULL,
  `user_agent` text DEFAULT NULL,
  `ip` varchar(45) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_consentimento_colaborador` (`colaborador_id`),
  CONSTRAINT `fk_consentimento_colaborador` FOREIGN KEY (`colaborador_id`) REFERENCES `colaborador` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci ROW_FORMAT=DYNAMIC;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `dados_medicos`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `dados_medicos` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `motivo` varchar(255) DEFAULT NULL,
  `tipo_cancer` varchar(255) DEFAULT NULL,
  `justificativa` varchar(255) NOT NULL,
  `inicio_tratamento` date NOT NULL,
  `tipo_atendimento` varchar(255) DEFAULT NULL,
  `relatorio_medico` longblob NOT NULL,
  `dt_inicio_tratamento` date DEFAULT NULL,
  `arquivo_id` int(11) DEFAULT NULL,
  `paciente_id` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKa332yjksf9g59umesfr8hd6tr` (`arquivo_id`),
  KEY `FK7yoo0hxyrjil3tjj0ngucd81w` (`paciente_id`),
  CONSTRAINT `FK7yoo0hxyrjil3tjj0ngucd81w` FOREIGN KEY (`paciente_id`) REFERENCES `paciente` (`id_paciente`),
  CONSTRAINT `FKfo42boy8v5m7by4iw2840l6it` FOREIGN KEY (`arquivo_id`) REFERENCES `arquivo` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci ROW_FORMAT=DYNAMIC;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `embedding_facial`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `embedding_facial` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `colaborador_id` varchar(50) NOT NULL,
  `vetor` longblob NOT NULL,
  `dimensao` int(11) NOT NULL,
  `dtype` varchar(16) NOT NULL,
  `criado_em` datetime NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_emb_colaborador` (`colaborador_id`),
  CONSTRAINT `fk_embedding_colaborador` FOREIGN KEY (`colaborador_id`) REFERENCES `colaborador` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `chk_embedding_dimensao` CHECK (`dimensao` > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci ROW_FORMAT=DYNAMIC;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `embedding_voz`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `embedding_voz` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `colaborador_id` varchar(50) NOT NULL,
  `vetor` longblob NOT NULL,
  `dimensao` int(11) NOT NULL,
  `dtype` varchar(16) NOT NULL,
  `criado_em` datetime NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_emb_voz_colaborador` (`colaborador_id`),
  CONSTRAINT `fk_emb_voz_colaborador` FOREIGN KEY (`colaborador_id`) REFERENCES `colaborador` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `chk_emb_voz_dimensao` CHECK (`dimensao` > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci ROW_FORMAT=DYNAMIC;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `endereco`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `endereco` (
  `id_endereco` int(11) NOT NULL AUTO_INCREMENT,
  `rua` varchar(255) NOT NULL,
  `numero` varchar(255) DEFAULT NULL,
  `bairro` varchar(255) NOT NULL,
  `cidade` varchar(255) NOT NULL,
  `estado` varchar(255) DEFAULT NULL,
  `cep` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id_endereco`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci ROW_FORMAT=DYNAMIC;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `filho`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `filho` (
  `id_filho` int(11) NOT NULL AUTO_INCREMENT,
  `paciente_id` int(11) DEFAULT NULL,
  `idade` int(11) NOT NULL,
  PRIMARY KEY (`id_filho`),
  KEY `fk_filho_paciente` (`paciente_id`),
  CONSTRAINT `fk_filho_paciente` FOREIGN KEY (`paciente_id`) REFERENCES `paciente` (`id_paciente`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `chk_filho_idade` CHECK (`idade` >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci ROW_FORMAT=DYNAMIC;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `hl7_audit_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `hl7_audit_log` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `detalhes` varchar(1000) DEFAULT NULL,
  `endereco_ip` varchar(45) DEFAULT NULL,
  `metodo_http` varchar(50) DEFAULT NULL,
  `recurso` varchar(255) NOT NULL,
  `status_resposta` varchar(20) DEFAULT NULL,
  `sucesso` bit(1) NOT NULL,
  `timestamp` datetime(6) NOT NULL,
  `tipo_evento` enum('ACESSO_DADOS_MEDICOS','ACESSO_NEGADO','ACESSO_PACIENTE','ALTERACAO_LAUDO','ALTERACAO_PACIENTE','CRIACAO_LAUDO','DOWNLOAD_ARQUIVO','EXCLUSAO_LAUDO','LOGIN','LOGIN_FALHA','LOGOUT','TOKEN_EXPIRADO','TOKEN_INVALIDO','UPLOAD_ARQUIVO') NOT NULL,
  `user_agent` varchar(500) DEFAULT NULL,
  `usuario` varchar(100) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_audit_timestamp` (`timestamp`),
  KEY `idx_audit_usuario` (`usuario`),
  KEY `idx_audit_recurso` (`recurso`),
  KEY `idx_audit_evento` (`tipo_evento`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `kit_amor`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `kit_amor` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `cor_peruca` varchar(255) DEFAULT NULL,
  `paciente_id` int(11) DEFAULT NULL,
  `solicitante_id` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FK7f2hlq2xv8a1bki8ysln2krq` (`paciente_id`),
  KEY `FK8cs20g98n8cycf83otxswjv1e` (`solicitante_id`),
  CONSTRAINT `FK7f2hlq2xv8a1bki8ysln2krq` FOREIGN KEY (`paciente_id`) REFERENCES `paciente` (`id_paciente`),
  CONSTRAINT `FK8cs20g98n8cycf83otxswjv1e` FOREIGN KEY (`solicitante_id`) REFERENCES `solicitante` (`id_solicitante`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `kit_do_amor`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `kit_do_amor` (
  `id_kit` int(11) NOT NULL AUTO_INCREMENT,
  `cor_peruca` varchar(45) NOT NULL,
  `solicitante_id` int(11) NOT NULL,
  PRIMARY KEY (`id_kit`),
  KEY `fk_kit_solicitante` (`solicitante_id`),
  CONSTRAINT `fk_kit_solicitante` FOREIGN KEY (`solicitante_id`) REFERENCES `solicitante` (`id_solicitante`) ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci ROW_FORMAT=DYNAMIC;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `log_auditoria`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `log_auditoria` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `data_hora` datetime NOT NULL,
  `autor` varchar(50) NOT NULL,
  `acao` varchar(100) NOT NULL,
  `alvo` varchar(50) DEFAULT NULL,
  `ip` varchar(45) DEFAULT NULL,
  `detalhes` text DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_log_auditoria_data` (`data_hora`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci ROW_FORMAT=DYNAMIC;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `madrinha`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `madrinha` (
  `id_madrinha` int(11) NOT NULL AUTO_INCREMENT,
  `nome_completo` varchar(255) NOT NULL,
  `data_inicio` date NOT NULL,
  `imagem_facial` longblob NOT NULL,
  `colaborador_id` varchar(50) DEFAULT NULL,
  `data_cadastro` date DEFAULT NULL,
  `email` varchar(255) DEFAULT NULL,
  `funcao` varchar(255) DEFAULT NULL,
  `horas_voluntarias` int(11) DEFAULT NULL,
  `status` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id_madrinha`),
  KEY `fk_madrinha_colaborador` (`colaborador_id`),
  CONSTRAINT `fk_madrinha_colaborador` FOREIGN KEY (`colaborador_id`) REFERENCES `colaborador` (`id`) ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci ROW_FORMAT=DYNAMIC;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `paciente`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `paciente` (
  `id_paciente` int(11) NOT NULL AUTO_INCREMENT,
  `data_pedido` date NOT NULL,
  `email` varchar(255) NOT NULL,
  `nome_completo` varchar(255) NOT NULL,
  `celular` varchar(255) DEFAULT NULL,
  `data_nascimento` date NOT NULL,
  `estado_civil` varchar(255) DEFAULT NULL,
  `qtd_filhos` int(11) NOT NULL,
  `qtd_pessoas_casa` int(11) NOT NULL,
  `cpf` varchar(255) DEFAULT NULL,
  `cabelo_antes` longblob NOT NULL,
  `endereco_id` int(11) DEFAULT NULL,
  `kit_id` int(11) DEFAULT NULL,
  `dados_medicos_id` int(11) DEFAULT NULL,
  `cel` varchar(255) DEFAULT NULL,
  `consentimento_lgpd` bit(1) NOT NULL,
  `dados_anonimizados` bit(1) NOT NULL,
  `dt_anonimizacao` datetime(6) DEFAULT NULL,
  `dt_consentimento` datetime(6) DEFAULT NULL,
  `dt_nasc` date DEFAULT NULL,
  `dt_pedido` date DEFAULT NULL,
  `finalidade_tratamento` varchar(255) DEFAULT NULL,
  `qtd_pessoas_em_casa` int(11) DEFAULT NULL,
  `tem_filhos` bit(1) DEFAULT NULL,
  `cabelo_antes_id` int(11) DEFAULT NULL,
  `solicitante_id` int(11) DEFAULT NULL,
  PRIMARY KEY (`id_paciente`),
  UNIQUE KEY `UKialsxdnnfqwk6h0g9tkbrb1wg` (`cabelo_antes_id`),
  KEY `fk_paciente_endereco` (`endereco_id`),
  KEY `fk_paciente_kit` (`kit_id`),
  KEY `fk_paciente_dados_medicos` (`dados_medicos_id`),
  KEY `FKt3nj1qb6lo2cnvb96j77inb1n` (`solicitante_id`),
  CONSTRAINT `FK40pxa17u1gl9s2lu3i78lnay` FOREIGN KEY (`cabelo_antes_id`) REFERENCES `arquivo` (`id`),
  CONSTRAINT `FKt3nj1qb6lo2cnvb96j77inb1n` FOREIGN KEY (`solicitante_id`) REFERENCES `solicitante` (`id_solicitante`),
  CONSTRAINT `fk_paciente_dados_medicos` FOREIGN KEY (`dados_medicos_id`) REFERENCES `dados_medicos` (`id`) ON DELETE SET NULL ON UPDATE CASCADE,
  CONSTRAINT `fk_paciente_endereco` FOREIGN KEY (`endereco_id`) REFERENCES `endereco` (`id_endereco`) ON DELETE SET NULL ON UPDATE CASCADE,
  CONSTRAINT `fk_paciente_kit` FOREIGN KEY (`kit_id`) REFERENCES `kit_do_amor` (`id_kit`) ON DELETE SET NULL ON UPDATE CASCADE,
  CONSTRAINT `chk_paciente_qtd_filhos` CHECK (`qtd_filhos` >= 0),
  CONSTRAINT `chk_paciente_qtd_pessoas_casa` CHECK (`qtd_pessoas_casa` >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci ROW_FORMAT=DYNAMIC;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `ponto`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `ponto` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `colaborador_id` varchar(50) NOT NULL,
  `data_hora` datetime NOT NULL,
  `tipo` enum('IN','OUT') NOT NULL,
  `confianca` double NOT NULL,
  `caminho_imagem` varchar(512) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_ponto_colaborador_data` (`colaborador_id`,`data_hora`),
  CONSTRAINT `fk_ponto_colaborador` FOREIGN KEY (`colaborador_id`) REFERENCES `colaborador` (`id`) ON UPDATE CASCADE,
  CONSTRAINT `chk_ponto_confianca` CHECK (`confianca` between 0 and 1)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci ROW_FORMAT=DYNAMIC;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `solicitante`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `solicitante` (
  `id_solicitante` int(11) NOT NULL AUTO_INCREMENT,
  `nome_completo` varchar(255) NOT NULL,
  `rg` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id_solicitante`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci ROW_FORMAT=DYNAMIC;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `usuario`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `usuario` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `username` varchar(255) NOT NULL,
  `password` varchar(255) NOT NULL,
  `role` varchar(255) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_usuario_username` (`username`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci ROW_FORMAT=DYNAMIC;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `usuario_backup_20261010`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `usuario_backup_20261010` (
  `idUsuario` int(11) NOT NULL AUTO_INCREMENT,
  `username` varchar(255) DEFAULT NULL,
  `password` varchar(255) DEFAULT NULL,
  `role` varchar(255) NOT NULL,
  PRIMARY KEY (`idUsuario`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci ROW_FORMAT=DYNAMIC;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*M!100616 SET NOTE_VERBOSITY=@OLD_NOTE_VERBOSITY */;

