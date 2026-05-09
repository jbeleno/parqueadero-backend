package com.usco.parqueaderos_api.billing.service;

import com.usco.parqueaderos_api.auth.service.CurrentUserService;
import com.usco.parqueaderos_api.billing.dto.PagoDTO;
import com.usco.parqueaderos_api.billing.entity.Factura;
import com.usco.parqueaderos_api.billing.entity.Pago;
import com.usco.parqueaderos_api.billing.repository.FacturaRepository;
import com.usco.parqueaderos_api.billing.repository.PagoRepository;
import com.usco.parqueaderos_api.common.exception.BusinessException;
import com.usco.parqueaderos_api.parking.entity.Empresa;
import com.usco.parqueaderos_api.parking.entity.Parqueadero;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PagoServiceTest {

    @Mock PagoRepository pagoRepository;
    @Mock FacturaRepository facturaRepository;
    @Mock CurrentUserService currentUser;

    @InjectMocks PagoService pagoService;

    private Factura factura;

    @BeforeEach
    void setUp() {
        Empresa e = new Empresa(); e.setId(1L);
        Parqueadero p = new Parqueadero(); p.setId(2L); p.setEmpresa(e);
        factura = new Factura();
        factura.setId(100L);
        factura.setParqueadero(p);
        factura.setValorTotal(10000.0);
        factura.setEstado("PENDIENTE");
    }

    private PagoDTO dto(Double monto, String metodo) {
        PagoDTO d = new PagoDTO();
        d.setFacturaId(factura.getId());
        d.setMonto(monto);
        d.setMetodo(metodo);
        return d;
    }

    @Test
    @DisplayName("save: USER recibe AccessDenied")
    void save_userNoPuede_403() {
        when(currentUser.isAdmin()).thenReturn(false);
        when(currentUser.isSuperAdmin()).thenReturn(false);
        assertThrows(AccessDeniedException.class,
                () -> pagoService.save(dto(10000.0, "EFECTIVO")));
    }

    @Test
    @DisplayName("save: monto 0 lanza ERR_INVALID_AMOUNT")
    void save_montoCero_lanza() {
        when(currentUser.isAdmin()).thenReturn(true);
        when(facturaRepository.findById(factura.getId())).thenReturn(Optional.of(factura));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> pagoService.save(dto(0.0, "EFECTIVO")));
        assertEquals("ERR_INVALID_AMOUNT", ex.getErrorCode());
    }

    @Test
    @DisplayName("save: monto negativo lanza ERR_INVALID_AMOUNT")
    void save_montoNegativo_lanza() {
        when(currentUser.isAdmin()).thenReturn(true);
        when(facturaRepository.findById(factura.getId())).thenReturn(Optional.of(factura));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> pagoService.save(dto(-100.0, "EFECTIVO")));
        assertEquals("ERR_INVALID_AMOUNT", ex.getErrorCode());
    }

    @Test
    @DisplayName("save: monto excede saldo pendiente lanza ERR_INSUFFICIENT_FUNDS")
    void save_excedeSaldo_lanza() {
        when(currentUser.isAdmin()).thenReturn(true);
        when(facturaRepository.findById(factura.getId())).thenReturn(Optional.of(factura));
        when(pagoRepository.findByFacturaId(factura.getId())).thenReturn(Collections.emptyList());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> pagoService.save(dto(15000.0, "EFECTIVO")));
        assertEquals("ERR_INSUFFICIENT_FUNDS", ex.getErrorCode());
    }

    @Test
    @DisplayName("save: factura ya PAGADA lanza ERR_INVOICE_ALREADY_PAID")
    void save_facturaPagada_lanza() {
        factura.setEstado("PAGADA");
        when(currentUser.isAdmin()).thenReturn(true);
        when(facturaRepository.findById(factura.getId())).thenReturn(Optional.of(factura));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> pagoService.save(dto(10000.0, "EFECTIVO")));
        assertEquals("ERR_INVOICE_ALREADY_PAID", ex.getErrorCode());
    }

    @Test
    @DisplayName("save: pago completo marca factura PAGADA automaticamente")
    void save_pagoCompleto_marcaFacturaPagada() {
        when(currentUser.isAdmin()).thenReturn(true);
        when(facturaRepository.findById(factura.getId())).thenReturn(Optional.of(factura));
        when(pagoRepository.findByFacturaId(factura.getId())).thenReturn(Collections.emptyList());
        when(pagoRepository.save(any(Pago.class))).thenAnswer(inv -> {
            Pago p = inv.getArgument(0);
            p.setId(1L);
            return p;
        });

        pagoService.save(dto(10000.0, "EFECTIVO"));

        verify(facturaRepository).save(argThat(f -> "PAGADA".equals(f.getEstado())));
    }

    @Test
    @DisplayName("save: pago parcial NO marca factura PAGADA")
    void save_pagoParcial_facturaSiguePendiente() {
        when(currentUser.isAdmin()).thenReturn(true);
        when(facturaRepository.findById(factura.getId())).thenReturn(Optional.of(factura));
        when(pagoRepository.findByFacturaId(factura.getId())).thenReturn(Collections.emptyList());
        when(pagoRepository.save(any(Pago.class))).thenAnswer(inv -> {
            Pago p = inv.getArgument(0);
            p.setId(1L);
            return p;
        });

        pagoService.save(dto(5000.0, "EFECTIVO"));

        verify(facturaRepository, never()).save(any());
    }

    @Test
    @DisplayName("save: forza fecha del servidor (ignora fecha del DTO)")
    void save_ignoraFechaDelDto() {
        when(currentUser.isAdmin()).thenReturn(true);
        when(facturaRepository.findById(factura.getId())).thenReturn(Optional.of(factura));
        when(pagoRepository.findByFacturaId(factura.getId())).thenReturn(Collections.emptyList());
        when(pagoRepository.save(any(Pago.class))).thenAnswer(inv -> inv.getArgument(0));

        PagoDTO d = dto(5000.0, "EFECTIVO");
        d.setFechaHora(java.time.LocalDateTime.of(1970, 1, 1, 0, 0));

        pagoService.save(d);

        verify(pagoRepository).save(argThat(saved ->
                saved.getFechaHora() != null
                && saved.getFechaHora().getYear() >= 2024
        ));
    }
}
