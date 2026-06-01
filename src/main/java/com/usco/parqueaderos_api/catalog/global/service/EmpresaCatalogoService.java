package com.usco.parqueaderos_api.catalog.global.service;

import com.usco.parqueaderos_api.catalog.global.entity.EmpresaCatalogoGlobalActivo;
import com.usco.parqueaderos_api.catalog.global.repository.EmpresaCatalogoGlobalActivoRepository;
import com.usco.parqueaderos_api.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Servicio para gestionar la lista de catalogos globales que UNA empresa
 * acepta. v50 Sprint 4.
 *
 * Cada empresa puede:
 *  - Activar/desactivar individualmente items globales.
 *  - Aceptar todos los globales activos (bulk).
 *  - Rechazar todos los globales (queda solo con sus customs).
 */
@Service
@RequiredArgsConstructor
public class EmpresaCatalogoService {

    private static final Set<String> CATALOGOS_VALIDOS = Set.of(
            "tipo_documento", "genero", "moneda", "zona_horaria",
            "unidad_tarifa", "regimen_tributario", "estado_civil",
            "pais_codigo_placa", "tipo_servicio_vehiculo",
            "tipo_acceso_dispositivo", "canal_origen_reserva"
    );

    private final EmpresaCatalogoGlobalActivoRepository activoRepo;
    private final CatalogoResolverService resolverService;

    private void validarCatalogo(String catalogo) {
        if (!CATALOGOS_VALIDOS.contains(catalogo)) {
            throw new BusinessException(
                    "Catalogo desconocido: " + catalogo + ". Validos: " + CATALOGOS_VALIDOS,
                    "ERR_CATALOG_UNKNOWN");
        }
    }

    /**
     * Convierte "tipos-documento" (URL) a "tipo_documento" (BD). Acepta ambos.
     */
    public String normalizar(String catalogo) {
        return switch (catalogo) {
            case "tipos-documento" -> "tipo_documento";
            case "generos" -> "genero";
            case "monedas" -> "moneda";
            case "zonas-horarias" -> "zona_horaria";
            case "unidades-tarifa" -> "unidad_tarifa";
            case "regimenes-tributarios" -> "regimen_tributario";
            case "estados-civiles" -> "estado_civil";
            case "paises-placa" -> "pais_codigo_placa";
            case "tipos-servicio-vehiculo" -> "tipo_servicio_vehiculo";
            case "tipos-acceso-dispositivo" -> "tipo_acceso_dispositivo";
            case "canales-origen-reserva" -> "canal_origen_reserva";
            default -> catalogo;
        };
    }

    @Transactional(readOnly = true)
    public List<EmpresaCatalogoGlobalActivo> listarConfig(Long empresaId, String catalogo) {
        String nombre = normalizar(catalogo);
        validarCatalogo(nombre);
        return activoRepo.findByEmpresaIdAndCatalogo(empresaId, nombre);
    }

    @Transactional
    public EmpresaCatalogoGlobalActivo upsert(Long empresaId, String catalogo, Long itemId,
                                                Boolean activo, Long usuarioId) {
        String nombre = normalizar(catalogo);
        validarCatalogo(nombre);
        if (itemId == null) {
            throw new BusinessException("itemId es obligatorio", "ERR_MISSING_FIELDS");
        }
        EmpresaCatalogoGlobalActivo existente = activoRepo
                .findByEmpresaIdAndCatalogoAndItemId(empresaId, nombre, itemId)
                .orElseGet(() -> {
                    var nuevo = new EmpresaCatalogoGlobalActivo();
                    nuevo.setEmpresaId(empresaId);
                    nuevo.setCatalogo(nombre);
                    nuevo.setItemId(itemId);
                    return nuevo;
                });
        existente.setActivo(activo == null ? Boolean.TRUE : activo);
        existente.setActualizadoPorUsuarioId(usuarioId);
        return activoRepo.save(existente);
    }

    /**
     * Marca TODOS los globales del catalogo como activos para esta empresa.
     * Crea filas en empresa_catalogo_global_activo con activo=true para
     * cada item global vigente.
     */
    @Transactional
    public int aceptarTodos(Long empresaId, String catalogo, Long usuarioId) {
        String nombre = normalizar(catalogo);
        validarCatalogo(nombre);
        List<?> globales = resolverService.resolverParaEmpresaConSoloGlobales(nombre, empresaId);
        int count = 0;
        for (Object item : globales) {
            Long itemId = resolverService.extraerId(item);
            upsert(empresaId, nombre, itemId, true, usuarioId);
            count++;
        }
        return count;
    }

    /**
     * Marca TODOS los globales del catalogo como inactivos para esta empresa.
     * La empresa queda solo con sus customs.
     */
    @Transactional
    public int rechazarTodos(Long empresaId, String catalogo, Long usuarioId) {
        String nombre = normalizar(catalogo);
        validarCatalogo(nombre);
        List<?> globales = resolverService.resolverParaEmpresaConSoloGlobales(nombre, empresaId);
        int count = 0;
        for (Object item : globales) {
            Long itemId = resolverService.extraerId(item);
            upsert(empresaId, nombre, itemId, false, usuarioId);
            count++;
        }
        return count;
    }
}
