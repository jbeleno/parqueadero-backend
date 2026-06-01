package com.usco.parqueaderos_api.catalog.global.service;

import com.usco.parqueaderos_api.catalog.global.entity.*;
import com.usco.parqueaderos_api.catalog.global.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Resuelve los items visibles de un catalogo global para una empresa dada.
 * v50 Sprint 2.
 *
 * Algoritmo:
 *   1. Items globales (empresa_id IS NULL) que la empresa marco como activos
 *      en empresa_catalogo_global_activo.
 *   2. Items custom (empresa_id = X) de esa empresa, activos.
 *   3. Union ordenada por orden_display.
 *
 * Retrocompat: si la empresa NO tiene filas en empresa_catalogo_global_activo
 * para un catalogo dado, acepta TODOS los globales activos por default.
 *
 * Si empresaId es null (usuario sin empresa, ej. SUPER_ADMIN sin scope),
 * devuelve solo los globales activos.
 */
@Service
@RequiredArgsConstructor
public class CatalogoResolverService {

    private final TipoDocumentoRepository tipoDocumentoRepo;
    private final GeneroRepository generoRepo;
    private final MonedaRepository monedaRepo;
    private final ZonaHorariaRepository zonaHorariaRepo;
    private final UnidadTarifaRepository unidadTarifaRepo;
    private final RegimenTributarioRepository regimenTributarioRepo;
    private final EstadoCivilRepository estadoCivilRepo;
    private final PaisCodigoPlacaRepository paisCodigoPlacaRepo;
    private final TipoServicioVehiculoRepository tipoServicioVehiculoRepo;
    private final TipoAccesoDispositivoRepository tipoAccesoDispositivoRepo;
    private final CanalOrigenReservaRepository canalOrigenReservaRepo;
    private final EmpresaCatalogoGlobalActivoRepository activoRepo;

    @Transactional(readOnly = true)
    public List<TipoDocumento> resolverTiposDocumento(Long empresaId) {
        return resolver("tipo_documento", empresaId,
                tipoDocumentoRepo::findByEmpresaIdIsNullAndActivoTrueOrderByOrdenDisplayAsc,
                tipoDocumentoRepo::findByEmpresaIdAndActivoTrueOrderByOrdenDisplayAsc,
                TipoDocumento::getId);
    }

    @Transactional(readOnly = true)
    public List<Genero> resolverGeneros(Long empresaId) {
        return resolver("genero", empresaId,
                generoRepo::findByEmpresaIdIsNullAndActivoTrueOrderByOrdenDisplayAsc,
                generoRepo::findByEmpresaIdAndActivoTrueOrderByOrdenDisplayAsc,
                Genero::getId);
    }

    @Transactional(readOnly = true)
    public List<Moneda> resolverMonedas(Long empresaId) {
        return resolver("moneda", empresaId,
                monedaRepo::findByEmpresaIdIsNullAndActivoTrueOrderByOrdenDisplayAsc,
                monedaRepo::findByEmpresaIdAndActivoTrueOrderByOrdenDisplayAsc,
                Moneda::getId);
    }

    @Transactional(readOnly = true)
    public List<ZonaHoraria> resolverZonasHorarias(Long empresaId) {
        return resolver("zona_horaria", empresaId,
                zonaHorariaRepo::findByEmpresaIdIsNullAndActivoTrueOrderByOrdenDisplayAsc,
                zonaHorariaRepo::findByEmpresaIdAndActivoTrueOrderByOrdenDisplayAsc,
                ZonaHoraria::getId);
    }

    @Transactional(readOnly = true)
    public List<UnidadTarifa> resolverUnidadesTarifa(Long empresaId) {
        return resolver("unidad_tarifa", empresaId,
                unidadTarifaRepo::findByEmpresaIdIsNullAndActivoTrueOrderByOrdenDisplayAsc,
                unidadTarifaRepo::findByEmpresaIdAndActivoTrueOrderByOrdenDisplayAsc,
                UnidadTarifa::getId);
    }

    @Transactional(readOnly = true)
    public List<RegimenTributario> resolverRegimenesTributarios(Long empresaId) {
        return resolver("regimen_tributario", empresaId,
                regimenTributarioRepo::findByEmpresaIdIsNullAndActivoTrueOrderByOrdenDisplayAsc,
                regimenTributarioRepo::findByEmpresaIdAndActivoTrueOrderByOrdenDisplayAsc,
                RegimenTributario::getId);
    }

    @Transactional(readOnly = true)
    public List<EstadoCivil> resolverEstadosCiviles(Long empresaId) {
        return resolver("estado_civil", empresaId,
                estadoCivilRepo::findByEmpresaIdIsNullAndActivoTrueOrderByOrdenDisplayAsc,
                estadoCivilRepo::findByEmpresaIdAndActivoTrueOrderByOrdenDisplayAsc,
                EstadoCivil::getId);
    }

    @Transactional(readOnly = true)
    public List<PaisCodigoPlaca> resolverPaisesPlaca(Long empresaId) {
        return resolver("pais_codigo_placa", empresaId,
                paisCodigoPlacaRepo::findByEmpresaIdIsNullAndActivoTrueOrderByOrdenDisplayAsc,
                paisCodigoPlacaRepo::findByEmpresaIdAndActivoTrueOrderByOrdenDisplayAsc,
                PaisCodigoPlaca::getId);
    }

    @Transactional(readOnly = true)
    public List<TipoServicioVehiculo> resolverTiposServicioVehiculo(Long empresaId) {
        return resolver("tipo_servicio_vehiculo", empresaId,
                tipoServicioVehiculoRepo::findByEmpresaIdIsNullAndActivoTrueOrderByOrdenDisplayAsc,
                tipoServicioVehiculoRepo::findByEmpresaIdAndActivoTrueOrderByOrdenDisplayAsc,
                TipoServicioVehiculo::getId);
    }

    @Transactional(readOnly = true)
    public List<TipoAccesoDispositivo> resolverTiposAccesoDispositivo(Long empresaId) {
        return resolver("tipo_acceso_dispositivo", empresaId,
                tipoAccesoDispositivoRepo::findByEmpresaIdIsNullAndActivoTrueOrderByOrdenDisplayAsc,
                tipoAccesoDispositivoRepo::findByEmpresaIdAndActivoTrueOrderByOrdenDisplayAsc,
                TipoAccesoDispositivo::getId);
    }

    @Transactional(readOnly = true)
    public List<CanalOrigenReserva> resolverCanalesOrigenReserva(Long empresaId) {
        return resolver("canal_origen_reserva", empresaId,
                canalOrigenReservaRepo::findByEmpresaIdIsNullAndActivoTrueOrderByOrdenDisplayAsc,
                canalOrigenReservaRepo::findByEmpresaIdAndActivoTrueOrderByOrdenDisplayAsc,
                CanalOrigenReserva::getId);
    }

    /**
     * Devuelve solo los items GLOBALES (empresa_id IS NULL) del catalogo.
     * Util para bulk operations donde hay que iterar los canonicos.
     */
    @Transactional(readOnly = true)
    public List<?> resolverParaEmpresaConSoloGlobales(String catalogo, Long empresaId) {
        return switch (catalogo) {
            case "tipo_documento" -> tipoDocumentoRepo.findByEmpresaIdIsNullAndActivoTrueOrderByOrdenDisplayAsc();
            case "genero" -> generoRepo.findByEmpresaIdIsNullAndActivoTrueOrderByOrdenDisplayAsc();
            case "moneda" -> monedaRepo.findByEmpresaIdIsNullAndActivoTrueOrderByOrdenDisplayAsc();
            case "zona_horaria" -> zonaHorariaRepo.findByEmpresaIdIsNullAndActivoTrueOrderByOrdenDisplayAsc();
            case "unidad_tarifa" -> unidadTarifaRepo.findByEmpresaIdIsNullAndActivoTrueOrderByOrdenDisplayAsc();
            case "regimen_tributario" -> regimenTributarioRepo.findByEmpresaIdIsNullAndActivoTrueOrderByOrdenDisplayAsc();
            case "estado_civil" -> estadoCivilRepo.findByEmpresaIdIsNullAndActivoTrueOrderByOrdenDisplayAsc();
            case "pais_codigo_placa" -> paisCodigoPlacaRepo.findByEmpresaIdIsNullAndActivoTrueOrderByOrdenDisplayAsc();
            case "tipo_servicio_vehiculo" -> tipoServicioVehiculoRepo.findByEmpresaIdIsNullAndActivoTrueOrderByOrdenDisplayAsc();
            case "tipo_acceso_dispositivo" -> tipoAccesoDispositivoRepo.findByEmpresaIdIsNullAndActivoTrueOrderByOrdenDisplayAsc();
            case "canal_origen_reserva" -> canalOrigenReservaRepo.findByEmpresaIdIsNullAndActivoTrueOrderByOrdenDisplayAsc();
            default -> List.of();
        };
    }

    /** Helper reflection: extrae el id de un item de cualquier catalogo. */
    public Long extraerId(Object item) {
        try {
            var m = item.getClass().getMethod("getId");
            return (Long) m.invoke(item);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Algoritmo generico.
     *
     * @param catalogo       nombre logico ("tipo_documento", etc.)
     * @param empresaId      empresa del usuario (puede ser null)
     * @param globalSupplier funcion que carga TODOS los globales activos
     * @param customLoader   funcion que carga customs de una empresa por id
     * @param idExtractor    extrae el id de un item para filtrar
     */
    private <T> List<T> resolver(
            String catalogo,
            Long empresaId,
            java.util.function.Supplier<List<T>> globalSupplier,
            java.util.function.Function<Long, List<T>> customLoader,
            java.util.function.Function<T, Long> idExtractor) {

        // Sin empresa: solo globales activos.
        if (empresaId == null) {
            return globalSupplier.get();
        }

        // Globales activos
        List<T> globales = globalSupplier.get();

        // Modelo OPT-OUT (lista negra): un global esta visible POR DEFAULT a
        // menos que la empresa tenga una fila explicita con activo=false.
        // Esto es mas intuitivo: "marco CE como false" → solo CE se quita,
        // los demas siguen visibles. Sin necesidad de aceptar-todos primero.
        Set<Long> rechazadosIds = new HashSet<>();
        activoRepo.findByEmpresaIdAndCatalogo(empresaId, catalogo).stream()
                .filter(a -> Boolean.FALSE.equals(a.getActivo()))
                .forEach(a -> rechazadosIds.add(a.getItemId()));
        List<T> globalesFiltrados = globales.stream()
                .filter(g -> !rechazadosIds.contains(idExtractor.apply(g)))
                .toList();

        // Customs de la empresa
        List<T> customs = customLoader.apply(empresaId);

        // Union (orden display ya viene aplicado por cada query)
        return Stream.concat(globalesFiltrados.stream(), customs.stream()).toList();
    }
}
