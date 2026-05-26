package com.usco.parqueaderos_api.subscription.repository;

import com.usco.parqueaderos_api.subscription.entity.MovimientoSaldo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MovimientoSaldoRepository extends JpaRepository<MovimientoSaldo, Long> {

    /** Historial de movimientos de una suscripcion, mas reciente primero. */
    List<MovimientoSaldo> findBySuscripcionIdOrderByFechaDesc(Long suscripcionId);
}
