package com.usco.parqueaderos_api.billing.repository;

import com.usco.parqueaderos_api.billing.entity.Factura;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FacturaRepository extends JpaRepository<Factura, Long> {
    List<Factura> findByTicketId(Long ticketId);
    List<Factura> findByParqueaderoId(Long parqueaderoId);
    List<Factura> findByParqueaderoEmpresaId(Long empresaId);
    List<Factura> findByVehiculoPersonaId(Long personaId);
}
