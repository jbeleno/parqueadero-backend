package com.usco.parqueaderos_api.report.repository;

import com.usco.parqueaderos_api.report.entity.ReporteDefinicion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ReporteDefinicionRepository extends JpaRepository<ReporteDefinicion, Long> {

    /** Busca por clave en orden: empresa del usuario primero, fallback global. */
    @Query("SELECT r FROM ReporteDefinicion r WHERE r.clave = :clave " +
           "AND (r.empresaId = :empresaId OR r.empresaId IS NULL) " +
           "AND r.activo = true " +
           "ORDER BY CASE WHEN r.empresaId IS NULL THEN 1 ELSE 0 END")
    List<ReporteDefinicion> findByClaveConFallbackGlobal(String clave, Long empresaId);

    /** Lista los reportes disponibles para una empresa (sus propios + globales). */
    @Query("SELECT r FROM ReporteDefinicion r WHERE (r.empresaId = :empresaId OR r.empresaId IS NULL) " +
           "AND r.activo = true ORDER BY r.nombre")
    List<ReporteDefinicion> findDisponiblesPara(Long empresaId);

    Optional<ReporteDefinicion> findByClaveAndEmpresaIdIsNull(String clave);
}
