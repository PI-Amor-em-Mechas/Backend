package br.com.amorEmMechas_Formulario.api.para.formulario.security.audit;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    List<AuditLog> findByUsuarioOrderByTimestampDesc(String usuario);

    List<AuditLog> findByTipoEventoOrderByTimestampDesc(AuditLog.TipoEvento tipoEvento);

    List<AuditLog> findByTimestampBetweenOrderByTimestampDesc(LocalDateTime inicio, LocalDateTime fim);

    @Query("SELECT a FROM AuditLog a WHERE a.usuario = :usuario AND a.tipoEvento = :tipo AND a.timestamp > :desde")
    List<AuditLog> findEventosRecentes(
            @Param("usuario") String usuario,
            @Param("tipo") AuditLog.TipoEvento tipo,
            @Param("desde") LocalDateTime desde
    );

    @Query("SELECT COUNT(a) FROM AuditLog a WHERE a.tipoEvento = 'LOGIN_FALHA' AND a.enderecoIp = :ip AND a.timestamp > :desde")
    long countLoginFalhasPorIp(@Param("ip") String ip, @Param("desde") LocalDateTime desde);
}
