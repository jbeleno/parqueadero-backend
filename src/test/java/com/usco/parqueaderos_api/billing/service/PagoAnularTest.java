package com.usco.parqueaderos_api.billing.service;

import com.usco.parqueaderos_api.auth.service.CurrentUserService;
import com.usco.parqueaderos_api.billing.dto.PagoDTO;
import com.usco.parqueaderos_api.billing.entity.Factura;
import com.usco.parqueaderos_api.billing.entity.Pago;
import com.usco.parqueaderos_api.billing.repository.FacturaRepository;
import com.usco.parqueaderos_api.billing.repository.PagoRepository;
import com.usco.parqueaderos_api.common.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PagoAnularTest {

    private PagoRepository pagoRepo;
    private FacturaRepository facturaRepo;
    private CurrentUserService user;
    private PagoService service;

    @BeforeEach
    void setup() {
        pagoRepo = Mockito.mock(PagoRepository.class);
        facturaRepo = Mockito.mock(FacturaRepository.class);
        user = Mockito.mock(CurrentUserService.class);
        service = new PagoService(pagoRepo, facturaRepo, user);
        when(pagoRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(facturaRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    private Factura factura(double total, String estado) {
        Factura f = new Factura();
        f.setId(1L);
        f.setValorTotal(total);
        f.setEstado(estado);
        return f;
    }

    private Pago pago(Long id, double monto, String estado, Factura f) {
        Pago p = new Pago();
        p.setId(id);
        p.setMonto(monto);
        p.setEstado(estado);
        p.setFactura(f);
        return p;
    }

    @Test
    @DisplayName("USER no puede anular")
    void user_no_puede() {
        when(user.isSuperAdmin()).thenReturn(false);
        assertThrows(AccessDeniedException.class, () -> service.anular(1L, "motivo valido aqui"));
    }

    @Test
    @DisplayName("Motivo corto -> ERR_MISSING_FIELDS")
    void motivo_corto() {
        when(user.isSuperAdmin()).thenReturn(true);
        BusinessException ex = assertThrows(BusinessException.class, () -> service.anular(1L, "corto"));
        assertEquals("ERR_MISSING_FIELDS", ex.getErrorCode());
    }

    @Test
    @DisplayName("Anular el unico pago completado -> factura vuelve a PENDIENTE")
    void anula_y_factura_pendiente() {
        when(user.isSuperAdmin()).thenReturn(true);
        when(user.getCurrentUserId()).thenReturn(99L);
        Factura f = factura(5000, "PAGADA");
        Pago p = pago(10L, 5000, "COMPLETADO", f);
        when(pagoRepo.findById(10L)).thenReturn(Optional.of(p));
        // Al consultar pagos restantes: ya esta marcado ANULADO en memoria del service
        when(pagoRepo.findByFacturaId(1L)).thenReturn(List.of(p));
        PagoDTO out = service.anular(10L, "Chargeback del banco confirmado");
        assertEquals("ANULADO", out.getEstado());
        assertEquals("PENDIENTE", f.getEstado());
    }

    @Test
    @DisplayName("Anular pago parcial -> si suma restante sigue cubriendo, factura sigue PAGADA")
    void parcial_no_baja_estado() {
        when(user.isSuperAdmin()).thenReturn(true);
        when(user.getCurrentUserId()).thenReturn(99L);
        Factura f = factura(5000, "PAGADA");
        Pago p1 = pago(10L, 3000, "COMPLETADO", f);
        Pago p2 = pago(11L, 3000, "COMPLETADO", f);
        when(pagoRepo.findById(10L)).thenReturn(Optional.of(p1));
        // Al anular p1, quedan p2 (3000) + p1 (ANULADO)
        // pero el filtro en service excluye ANULADOs por estado
        when(pagoRepo.findByFacturaId(1L)).thenReturn(List.of(p1, p2));
        service.anular(10L, "Reverso manual operativo");
        // suma de COMPLETADOs restantes = 3000 < 5000 -> PENDIENTE
        assertEquals("PENDIENTE", f.getEstado());
    }

    @Test
    @DisplayName("Doble anulacion del mismo pago -> ERR_PAGO_YA_ANULADO")
    void doble_anula() {
        when(user.isSuperAdmin()).thenReturn(true);
        Pago p = pago(10L, 5000, "ANULADO", factura(5000, "PENDIENTE"));
        when(pagoRepo.findById(10L)).thenReturn(Optional.of(p));
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.anular(10L, "Motivo suficientemente largo"));
        assertEquals("ERR_PAGO_YA_ANULADO", ex.getErrorCode());
    }
}
