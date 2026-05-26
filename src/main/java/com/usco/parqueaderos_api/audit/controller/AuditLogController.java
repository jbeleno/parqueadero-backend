package com.usco.parqueaderos_api.audit.controller;

import com.usco.parqueaderos_api.audit.dto.AuditLogDTO;
import com.usco.parqueaderos_api.audit.entity.AuditLog;
import com.usco.parqueaderos_api.audit.repository.AuditLogRepository;
import com.usco.parqueaderos_api.auth.service.CurrentUserService;
import com.usco.parqueaderos_api.common.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Lectura de audit_log. RBAC:
 *  - SUPER_ADMIN: todo
 *  - ADMIN: solo registros de su empresa
 *  - ADMIN_PARQUEADERO: solo registros de sus parqueaderos (futuro)
 *  - OPERARIO_CAJA: solo registros propios
 *  - USER: nada
 *
 * El audit_log NO tiene endpoints de modificacion: es append-only.
 */
@RestController
@RequestMapping("/api/admin/audit-log")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN','ADMIN_PARQUEADERO','OPERARIO_CAJA')")
public class AuditLogController {

    private final AuditLogRepository repository;
    private final CurrentUserService currentUser;

    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> buscar(
            @RequestParam(required = false) String tabla,
            @RequestParam(required = false) Long usuarioId,
            @RequestParam(required = false) Long empresaId,
            @RequestParam(required = false) String accion,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime hasta,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {

        // Filtros forzados segun rol
        if (!currentUser.isSuperAdmin()) {
            if (currentUser.isAdmin()) {
                Long ownEmp = currentUser.getCurrentEmpresaId().orElse(null);
                if (ownEmp == null) throw new AccessDeniedException("Usuario sin empresa");
                empresaId = ownEmp; // forzar
            } else {
                // OPERARIO / ADMIN_PARQUEADERO: solo su propio audit
                usuarioId = currentUser.getCurrentUserId();
                empresaId = null;
            }
        }

        Pageable pageable = PageRequest.of(Math.max(0, page), Math.min(200, Math.max(1, size)));
        Page<AuditLog> p = repository.buscar(tabla, usuarioId, empresaId, accion, desde, hasta, pageable);
        List<AuditLogDTO> dtos = p.getContent().stream().map(this::toDTO).toList();
        Map<String, Object> body = Map.of(
                "page", p.getNumber(),
                "size", p.getSize(),
                "totalPages", p.getTotalPages(),
                "totalElements", p.getTotalElements(),
                "items", dtos
        );
        return ResponseEntity.ok(ApiResponse.ok(body));
    }

    @GetMapping("/por-registro/{tabla}/{registroId}")
    public ResponseEntity<ApiResponse<List<AuditLogDTO>>> historiaDeRegistro(
            @PathVariable String tabla, @PathVariable Long registroId) {
        List<AuditLogDTO> list = repository.findByTablaAndRegistroIdOrderByFechaHoraDesc(tabla, registroId)
                .stream().map(this::toDTO).toList();
        return ResponseEntity.ok(ApiResponse.ok(list));
    }

    private AuditLogDTO toDTO(AuditLog a) {
        return new AuditLogDTO(
                a.getId(), a.getTabla(), a.getRegistroId(), a.getAccion(),
                a.getUsuarioId(), a.getEmpresaId(), a.getOrigen(), a.getMotivo(),
                a.getValoresAntes(), a.getValoresDespues(),
                a.getRequestId(), a.getEndpoint(), a.getIp(), a.getUserAgent(),
                a.getFechaHora()
        );
    }
}
