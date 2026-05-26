package com.usco.parqueaderos_api.billing.repository;

import com.usco.parqueaderos_api.billing.entity.Factura;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FacturaRepository extends JpaRepository<Factura, Long> {
    List<Factura> findByTicketId(Long ticketId);
    List<Factura> findByParqueaderoId(Long parqueaderoId);
    List<Factura> findByParqueaderoEmpresaId(Long empresaId);
    List<Factura> findByVehiculoPersonaId(Long personaId);

    List<Factura> findByVehiculoIdAndEstado(Long vehiculoId, String estado);

    /** Vehiculos con al menos una factura PENDIENTE en un parqueadero. */
    @Query("SELECT DISTINCT f.vehiculo.id FROM Factura f " +
           "WHERE f.estado = 'PENDIENTE' AND f.parqueadero.id = :parqueaderoId")
    List<Long> findVehiculosConDeudaEnParqueadero(@Param("parqueaderoId") Long parqueaderoId);

    /** Vehiculos morosos a nivel de empresa (cualquier parqueadero del operador). */
    @Query("SELECT DISTINCT f.vehiculo.id FROM Factura f " +
           "WHERE f.estado = 'PENDIENTE' AND f.parqueadero.empresa.id = :empresaId")
    List<Long> findVehiculosConDeudaEnEmpresa(@Param("empresaId") Long empresaId);

    long countByVehiculoId(Long vehiculoId);
}
