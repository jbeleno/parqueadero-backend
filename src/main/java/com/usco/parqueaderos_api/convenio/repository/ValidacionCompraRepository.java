package com.usco.parqueaderos_api.convenio.repository;

import com.usco.parqueaderos_api.convenio.entity.ValidacionCompra;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ValidacionCompraRepository extends JpaRepository<ValidacionCompra, Long> {
    Optional<ValidacionCompra> findFirstByTicketIdOrderByFechaAplicacionDesc(Long ticketId);
    List<ValidacionCompra> findByTicketId(Long ticketId);
    List<ValidacionCompra> findByConvenioId(Long convenioId);
}
