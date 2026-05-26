package com.usco.parqueaderos_api.billing.service;

import com.usco.parqueaderos_api.auth.service.CurrentUserService;
import com.usco.parqueaderos_api.billing.dto.DeudaVehiculoDTO;
import com.usco.parqueaderos_api.billing.dto.FacturaDTO;
import com.usco.parqueaderos_api.billing.entity.Factura;
import com.usco.parqueaderos_api.billing.repository.FacturaRepository;
import com.usco.parqueaderos_api.common.exception.ResourceNotFoundException;
import com.usco.parqueaderos_api.vehicle.entity.Vehiculo;
import com.usco.parqueaderos_api.vehicle.repository.VehiculoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Servicio de deuda y morosos.
 *
 * Una factura cuenta como deuda si esta en estado PENDIENTE. Las PAGADAs
 * (cubiertas por la suma de pagos COMPLETADOs) y ANULADAS no cuentan.
 */
@Service
@RequiredArgsConstructor
public class DeudaService {

    private final FacturaRepository facturaRepository;
    private final VehiculoRepository vehiculoRepository;
    private final CurrentUserService currentUser;

    /** Deuda detallada de un vehiculo. ADMIN solo ve facturas de su empresa. */
    @Transactional(readOnly = true)
    public DeudaVehiculoDTO deudaPorVehiculo(Long vehiculoId) {
        Vehiculo v = vehiculoRepository.findById(vehiculoId)
                .orElseThrow(() -> new ResourceNotFoundException("Vehiculo", vehiculoId));

        // RBAC: USER solo ve sus propios vehiculos; ADMIN filtra por empresa.
        if (!currentUser.isSuperAdmin()) {
            if (currentUser.isAdmin()) {
                // ADMIN: validamos abajo al filtrar facturas por empresa
            } else {
                Long personaId = v.getPersona() != null ? v.getPersona().getId() : null;
                currentUser.requireOwnerOrAnyAdmin(personaId);
            }
        }

        List<Factura> pendientes = facturaRepository.findByVehiculoIdAndEstado(vehiculoId, "PENDIENTE");
        if (currentUser.isAdmin() && !currentUser.isSuperAdmin()) {
            Long empresaId = currentUser.getCurrentEmpresaId().orElse(null);
            if (empresaId != null) {
                pendientes = pendientes.stream()
                        .filter(f -> f.getParqueadero() != null && f.getParqueadero().getEmpresa() != null
                                && empresaId.equals(f.getParqueadero().getEmpresa().getId()))
                        .toList();
            }
        }

        double total = pendientes.stream().mapToDouble(Factura::getValorTotal).sum();
        List<FacturaDTO> dtos = pendientes.stream().map(this::toFacturaDTO).toList();
        return new DeudaVehiculoDTO(
                v.getId(),
                v.getPlaca(),
                total,
                pendientes.size(),
                dtos
        );
    }

    /** Lista de morosos del parqueadero del operador. Solo ADMIN/SUPER_ADMIN. */
    @Transactional(readOnly = true)
    public List<DeudaVehiculoDTO> listarMorosos() {
        if (!currentUser.isAdmin() && !currentUser.isSuperAdmin()) {
            throw new AccessDeniedException("Solo operador puede listar morosos");
        }
        List<Factura> facturas;
        if (currentUser.isSuperAdmin()) {
            facturas = facturaRepository.findAll().stream()
                    .filter(f -> "PENDIENTE".equals(f.getEstado()))
                    .toList();
        } else {
            Long empresaId = currentUser.getCurrentEmpresaId().orElse(null);
            if (empresaId == null) return List.of();
            facturas = facturaRepository.findByParqueaderoEmpresaId(empresaId).stream()
                    .filter(f -> "PENDIENTE".equals(f.getEstado()))
                    .toList();
        }

        // Agrupar por vehiculo
        Map<Long, List<Factura>> porVehiculo = new HashMap<>();
        for (Factura f : facturas) {
            if (f.getVehiculo() == null) continue;
            porVehiculo.computeIfAbsent(f.getVehiculo().getId(), k -> new ArrayList<>()).add(f);
        }

        List<DeudaVehiculoDTO> resultado = new ArrayList<>();
        porVehiculo.forEach((vehId, list) -> {
            double total = list.stream().mapToDouble(Factura::getValorTotal).sum();
            String placa = list.get(0).getVehiculo() != null ? list.get(0).getVehiculo().getPlaca() : null;
            resultado.add(new DeudaVehiculoDTO(
                    vehId, placa, total, list.size(),
                    list.stream().map(this::toFacturaDTO).toList()
            ));
        });
        // Ordenar por monto adeudado desc
        resultado.sort((a, b) -> Double.compare(b.getTotalAdeudado(), a.getTotalAdeudado()));
        return resultado;
    }

    private FacturaDTO toFacturaDTO(Factura f) {
        FacturaDTO dto = new FacturaDTO();
        dto.setId(f.getId());
        dto.setFechaHora(f.getFechaHora());
        if (f.getTicket() != null) dto.setTicketId(f.getTicket().getId());
        if (f.getParqueadero() != null) {
            dto.setParqueaderoId(f.getParqueadero().getId());
            dto.setParqueaderoNombre(f.getParqueadero().getNombre());
        }
        if (f.getVehiculo() != null) {
            dto.setVehiculoId(f.getVehiculo().getId());
            dto.setVehiculoPlaca(f.getVehiculo().getPlaca());
        }
        dto.setValorTotal(f.getValorTotal());
        dto.setEstado(f.getEstado());
        return dto;
    }
}
