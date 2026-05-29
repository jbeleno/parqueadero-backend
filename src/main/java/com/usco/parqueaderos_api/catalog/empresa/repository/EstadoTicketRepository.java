package com.usco.parqueaderos_api.catalog.empresa.repository;

import com.usco.parqueaderos_api.catalog.empresa.entity.EstadoTicket;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EstadoTicketRepository extends JpaRepository<EstadoTicket, Long> {
    Optional<EstadoTicket> findByEmpresaIdAndCodigo(Long empresaId, String codigo);
    List<EstadoTicket> findByEmpresaIdAndActivoTrueOrderByOrdenDisplayAsc(Long empresaId);
    List<EstadoTicket> findByEmpresaIdOrderByOrdenDisplayAsc(Long empresaId);
}
