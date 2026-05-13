package com.usco.parqueaderos_api.vehicle.repository;

import com.usco.parqueaderos_api.vehicle.entity.Vehiculo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VehiculoRepository extends JpaRepository<Vehiculo, Long> {
    Optional<Vehiculo> findByPlaca(String placa);
    List<Vehiculo> findByPersonaId(Long personaId);

    /**
     * Vehiculos con historial (ticket o reserva) en algun parqueadero de
     * la empresa indicada. Para que un ADMIN pueda ver "los clientes de
     * su empresa" cuando el frontend mande ?soloMiEmpresa=true.
     */
    @Query("SELECT DISTINCT v FROM Vehiculo v " +
           "WHERE v.id IN (" +
           "  SELECT t.vehiculo.id FROM Ticket t " +
           "  WHERE t.parqueadero.empresa.id = :empresaId" +
           ") OR v.id IN (" +
           "  SELECT r.vehiculo.id FROM Reserva r " +
           "  WHERE r.parqueadero.empresa.id = :empresaId" +
           ")")
    List<Vehiculo> findByActividadEnEmpresa(@Param("empresaId") Long empresaId);
}
