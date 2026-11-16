ALTER TABLE arquivo
    ADD COLUMN paciente_id INT NULL;

ALTER TABLE arquivo
    ADD CONSTRAINT fk_arquivo_paciente
    FOREIGN KEY (paciente_id)
    REFERENCES paciente (id_paciente);
