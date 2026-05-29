package com.usco.parqueaderos_api.audit.repository;

import com.usco.parqueaderos_api.audit.entity.AccionAuditable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AccionAuditableRepository extends JpaRepository<AccionAuditable, Long> {
    Optional<AccionAuditable> findByCodigo(String codigo);
}
