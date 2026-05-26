package com.usco.parqueaderos_api.report.repository;

import com.usco.parqueaderos_api.report.entity.CierreDia;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface CierreDiaRepository extends JpaRepository<CierreDia, Long> {
    Optional<CierreDia> findByParqueaderoIdAndFecha(Long parqueaderoId, LocalDate fecha);
}
