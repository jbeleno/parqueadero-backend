package com.usco.parqueaderos_api.billing.service;

import com.usco.parqueaderos_api.auth.service.CurrentUserService;
import com.usco.parqueaderos_api.billing.dto.DeudaVehiculoDTO;
import com.usco.parqueaderos_api.billing.entity.Factura;
import com.usco.parqueaderos_api.billing.repository.FacturaRepository;
import com.usco.parqueaderos_api.parking.entity.Empresa;
import com.usco.parqueaderos_api.parking.entity.Parqueadero;
import com.usco.parqueaderos_api.vehicle.entity.Vehiculo;
import com.usco.parqueaderos_api.vehicle.repository.VehiculoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

class DeudaServiceTest {

    private FacturaRepository facturaRepo;
    private VehiculoRepository vehRepo;
    private CurrentUserService user;
    private DeudaService service;

    @BeforeEach
    void setup() {
        facturaRepo = Mockito.mock(FacturaRepository.class);
        vehRepo = Mockito.mock(VehiculoRepository.class);
        user = Mockito.mock(CurrentUserService.class);
        service = new DeudaService(facturaRepo, vehRepo, user);
    }

    private Vehiculo veh(Long id, String placa) {
        Vehiculo v = new Vehiculo();
        v.setId(id);
        v.setPlaca(placa);
        return v;
    }

    private Factura factura(Long id, double monto, Vehiculo v, Long empresaId, String estado) {
        Factura f = new Factura();
        f.setId(id);
        f.setValorTotal(monto);
        f.setEstado(estado);
        f.setVehiculo(v);
        Parqueadero p = new Parqueadero();
        Empresa e = new Empresa(); e.setId(empresaId);
        p.setEmpresa(e);
        p.setId(100L);
        p.setNombre("Parq");
        f.setParqueadero(p);
        return f;
    }

    @Test
    @DisplayName("deudaPorVehiculo suma valorTotal de PENDIENTEs")
    void suma_deuda() {
        Vehiculo v = veh(1L, "ABC123");
        when(vehRepo.findById(1L)).thenReturn(Optional.of(v));
        when(user.isSuperAdmin()).thenReturn(true);
        when(facturaRepo.findByVehiculoIdAndEstado(1L, "PENDIENTE"))
                .thenReturn(List.of(
                        factura(10L, 5000, v, 10L, "PENDIENTE"),
                        factura(11L, 3000, v, 10L, "PENDIENTE")
                ));
        DeudaVehiculoDTO out = service.deudaPorVehiculo(1L);
        assertEquals(8000.0, out.getTotalAdeudado(), 0.001);
        assertEquals(2, out.getCantidadFacturas());
        assertEquals("ABC123", out.getPlaca());
    }

    @Test
    @DisplayName("ADMIN filtra facturas por empresa")
    void admin_filtra_empresa() {
        Vehiculo v = veh(1L, "ABC123");
        when(vehRepo.findById(1L)).thenReturn(Optional.of(v));
        when(user.isSuperAdmin()).thenReturn(false);
        when(user.isAdmin()).thenReturn(true);
        when(user.getCurrentEmpresaId()).thenReturn(Optional.of(10L));
        when(facturaRepo.findByVehiculoIdAndEstado(1L, "PENDIENTE"))
                .thenReturn(List.of(
                        factura(10L, 5000, v, 10L, "PENDIENTE"),   // de empresa 10 -> cuenta
                        factura(11L, 3000, v, 20L, "PENDIENTE")    // de empresa 20 -> no cuenta
                ));
        DeudaVehiculoDTO out = service.deudaPorVehiculo(1L);
        assertEquals(5000.0, out.getTotalAdeudado(), 0.001);
        assertEquals(1, out.getCantidadFacturas());
    }

    @Test
    @DisplayName("USER no puede listar morosos")
    void user_no_morosos() {
        when(user.isSuperAdmin()).thenReturn(false);
        when(user.isAdmin()).thenReturn(false);
        assertThrows(AccessDeniedException.class, () -> service.listarMorosos());
    }

    @Test
    @DisplayName("Lista morosos agrupa por vehiculo y ordena desc por monto")
    void agrupa_morosos() {
        Vehiculo v1 = veh(1L, "ABC123");
        Vehiculo v2 = veh(2L, "XYZ789");
        when(user.isSuperAdmin()).thenReturn(false);
        when(user.isAdmin()).thenReturn(true);
        when(user.getCurrentEmpresaId()).thenReturn(Optional.of(10L));
        when(facturaRepo.findByParqueaderoEmpresaId(10L)).thenReturn(List.of(
                factura(1L, 1000, v1, 10L, "PENDIENTE"),
                factura(2L, 4000, v1, 10L, "PENDIENTE"),
                factura(3L, 7000, v2, 10L, "PENDIENTE"),
                factura(4L, 9999, v1, 10L, "PAGADA") // no cuenta
        ));
        List<DeudaVehiculoDTO> out = service.listarMorosos();
        assertEquals(2, out.size());
        // v2 con 7000 > v1 con 5000 -> primero
        assertEquals(2L, out.get(0).getVehiculoId());
        assertEquals(7000.0, out.get(0).getTotalAdeudado(), 0.001);
        assertEquals(1L, out.get(1).getVehiculoId());
        assertEquals(5000.0, out.get(1).getTotalAdeudado(), 0.001);
    }
}
