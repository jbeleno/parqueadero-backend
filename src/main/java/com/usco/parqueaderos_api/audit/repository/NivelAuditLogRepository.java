package com.usco.parqueaderos_api.audit.repository;

import com.usco.parqueaderos_api.audit.entity.NivelAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface NivelAuditLogRepository extends JpaRepository<NivelAuditLog, Long> {
    Optional<NivelAuditLog> findByCodigo(String codigo);
}
