package com.usco.parqueaderos_api.report.repository;

import com.usco.parqueaderos_api.report.entity.ReporteEjecutado;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReporteEjecutadoRepository extends JpaRepository<ReporteEjecutado, Long> {
    List<ReporteEjecutado> findTop100ByEmpresaIdOrderByFechaHoraDesc(Long empresaId);
    List<ReporteEjecutado> findTop100ByClaveReporteOrderByFechaHoraDesc(String claveReporte);
}
