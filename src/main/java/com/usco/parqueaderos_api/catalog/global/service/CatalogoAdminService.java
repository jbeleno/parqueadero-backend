package com.usco.parqueaderos_api.catalog.global.service;

import com.usco.parqueaderos_api.catalog.global.entity.*;
import com.usco.parqueaderos_api.catalog.global.repository.*;
import com.usco.parqueaderos_api.common.exception.BusinessException;
import com.usco.parqueaderos_api.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Servicio de administracion CRUD de catalogos globales. v50 Sprint 3.
 *
 * Reglas RBAC:
 *  - empresaId NULL en el item → solo SUPER_ADMIN puede crear/editar/archivar.
 *  - empresaId = X (item custom de empresa X) → ADMIN de X o SUPER_ADMIN.
 *
 * El controller deserializa el body como Map<String,Object> y delega aqui.
 * Cada catalogo tiene su propio metodo create/update porque las entities
 * difieren en sus campos especificos (ej. PaisCodigoPlaca tiene regexPlaca,
 * Moneda tiene simbolo+decimales, etc.).
 */
@Service
@RequiredArgsConstructor
@SuppressWarnings("unchecked")
public class CatalogoAdminService {

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

    /** Resuelve el repositorio correspondiente al nombre del catalogo. */
    private JpaRepository<?, Long> repoFor(String catalogo) {
        return switch (catalogo) {
            case "tipos-documento", "tipo_documento" -> tipoDocumentoRepo;
            case "generos", "genero" -> generoRepo;
            case "monedas", "moneda" -> monedaRepo;
            case "zonas-horarias", "zona_horaria" -> zonaHorariaRepo;
            case "unidades-tarifa", "unidad_tarifa" -> unidadTarifaRepo;
            case "regimenes-tributarios", "regimen_tributario" -> regimenTributarioRepo;
            case "estados-civiles", "estado_civil" -> estadoCivilRepo;
            case "paises-placa", "pais_codigo_placa" -> paisCodigoPlacaRepo;
            case "tipos-servicio-vehiculo", "tipo_servicio_vehiculo" -> tipoServicioVehiculoRepo;
            case "tipos-acceso-dispositivo", "tipo_acceso_dispositivo" -> tipoAccesoDispositivoRepo;
            case "canales-origen-reserva", "canal_origen_reserva" -> canalOrigenReservaRepo;
            default -> throw new BusinessException(
                    "Catalogo desconocido: " + catalogo,
                    "ERR_CATALOG_UNKNOWN");
        };
    }

    @Transactional(readOnly = true)
    public List<?> listarTodos(String catalogo, Long empresaIdScope) {
        // SUPER_ADMIN ve globales + customs de cualquier empresa (empresaIdScope null).
        // ADMIN solo globales + sus customs (empresaIdScope = su empresa).
        return switch (catalogo) {
            case "tipos-documento" -> empresaIdScope == null
                    ? tipoDocumentoRepo.findAll()
                    : tipoDocumentoRepo.findByEmpresaIdIsNullOrEmpresaIdOrderByOrdenDisplayAsc(empresaIdScope);
            case "generos" -> empresaIdScope == null
                    ? generoRepo.findAll()
                    : generoRepo.findByEmpresaIdIsNullOrEmpresaIdOrderByOrdenDisplayAsc(empresaIdScope);
            case "monedas" -> empresaIdScope == null
                    ? monedaRepo.findAll()
                    : monedaRepo.findByEmpresaIdIsNullOrEmpresaIdOrderByOrdenDisplayAsc(empresaIdScope);
            case "zonas-horarias" -> empresaIdScope == null
                    ? zonaHorariaRepo.findAll()
                    : zonaHorariaRepo.findByEmpresaIdIsNullOrEmpresaIdOrderByOrdenDisplayAsc(empresaIdScope);
            case "unidades-tarifa" -> empresaIdScope == null
                    ? unidadTarifaRepo.findAll()
                    : unidadTarifaRepo.findByEmpresaIdIsNullOrEmpresaIdOrderByOrdenDisplayAsc(empresaIdScope);
            case "regimenes-tributarios" -> empresaIdScope == null
                    ? regimenTributarioRepo.findAll()
                    : regimenTributarioRepo.findByEmpresaIdIsNullOrEmpresaIdOrderByOrdenDisplayAsc(empresaIdScope);
            case "estados-civiles" -> empresaIdScope == null
                    ? estadoCivilRepo.findAll()
                    : estadoCivilRepo.findByEmpresaIdIsNullOrEmpresaIdOrderByOrdenDisplayAsc(empresaIdScope);
            case "paises-placa" -> empresaIdScope == null
                    ? paisCodigoPlacaRepo.findAll()
                    : paisCodigoPlacaRepo.findByEmpresaIdIsNullOrEmpresaIdOrderByOrdenDisplayAsc(empresaIdScope);
            case "tipos-servicio-vehiculo" -> empresaIdScope == null
                    ? tipoServicioVehiculoRepo.findAll()
                    : tipoServicioVehiculoRepo.findByEmpresaIdIsNullOrEmpresaIdOrderByOrdenDisplayAsc(empresaIdScope);
            case "tipos-acceso-dispositivo" -> empresaIdScope == null
                    ? tipoAccesoDispositivoRepo.findAll()
                    : tipoAccesoDispositivoRepo.findByEmpresaIdIsNullOrEmpresaIdOrderByOrdenDisplayAsc(empresaIdScope);
            case "canales-origen-reserva" -> empresaIdScope == null
                    ? canalOrigenReservaRepo.findAll()
                    : canalOrigenReservaRepo.findByEmpresaIdIsNullOrEmpresaIdOrderByOrdenDisplayAsc(empresaIdScope);
            default -> throw new BusinessException(
                    "Catalogo desconocido: " + catalogo, "ERR_CATALOG_UNKNOWN");
        };
    }

    @Transactional(readOnly = true)
    public Object getById(String catalogo, Long id) {
        return repoFor(catalogo).findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(catalogo + ":" + id));
    }

    /**
     * Crea un item nuevo. Si body.empresaId == null → es global (requiere SUPER_ADMIN).
     * Si body.empresaId == X → es custom de X (requiere ADMIN de X o SUPER_ADMIN).
     *
     * El RBAC lo enforce el caller (controller). Aqui solo deserializamos y persistimos.
     */
    @Transactional
    public Object crear(String catalogo, Map<String, Object> body) {
        Long empresaId = asLong(body.get("empresaId"));
        String codigo = asString(body.get("codigo"));
        String nombre = asString(body.get("nombre"));
        if (codigo == null || codigo.isBlank() || nombre == null || nombre.isBlank()) {
            throw new BusinessException("codigo y nombre son obligatorios", "ERR_MISSING_FIELDS");
        }

        Object entity = switch (catalogo) {
            case "tipos-documento" -> {
                TipoDocumento e = new TipoDocumento();
                e.setEmpresaId(empresaId);
                e.setCodigo(codigo);
                e.setNombre(nombre);
                e.setDescripcion(asString(body.get("descripcion")));
                e.setAplicaPersona(asBool(body.get("aplicaPersona"), true));
                e.setAplicaEmpresa(asBool(body.get("aplicaEmpresa"), false));
                e.setOrdenDisplay(asInt(body.get("ordenDisplay")));
                e.setActivo(asBool(body.get("activo"), true));
                yield tipoDocumentoRepo.save(e);
            }
            case "generos" -> {
                Genero e = new Genero();
                e.setEmpresaId(empresaId);
                e.setCodigo(codigo);
                e.setNombre(nombre);
                e.setDescripcion(asString(body.get("descripcion")));
                e.setOrdenDisplay(asInt(body.get("ordenDisplay")));
                e.setActivo(asBool(body.get("activo"), true));
                yield generoRepo.save(e);
            }
            case "monedas" -> {
                Moneda e = new Moneda();
                e.setEmpresaId(empresaId);
                e.setCodigo(codigo);
                e.setNombre(nombre);
                e.setSimbolo(asString(body.get("simbolo")));
                e.setDecimales(asInt(body.get("decimales")));
                e.setOrdenDisplay(asInt(body.get("ordenDisplay")));
                e.setActivo(asBool(body.get("activo"), true));
                yield monedaRepo.save(e);
            }
            case "zonas-horarias" -> {
                ZonaHoraria e = new ZonaHoraria();
                e.setEmpresaId(empresaId);
                e.setCodigo(codigo);
                e.setNombre(nombre);
                e.setOffsetHoras(asInt(body.get("offsetHoras")));
                e.setOrdenDisplay(asInt(body.get("ordenDisplay")));
                e.setActivo(asBool(body.get("activo"), true));
                yield zonaHorariaRepo.save(e);
            }
            case "unidades-tarifa" -> {
                UnidadTarifa e = new UnidadTarifa();
                e.setEmpresaId(empresaId);
                e.setCodigo(codigo);
                e.setNombre(nombre);
                e.setMinutos(asInt(body.get("minutos")));
                e.setOrdenDisplay(asInt(body.get("ordenDisplay")));
                e.setActivo(asBool(body.get("activo"), true));
                yield unidadTarifaRepo.save(e);
            }
            case "regimenes-tributarios" -> {
                RegimenTributario e = new RegimenTributario();
                e.setEmpresaId(empresaId);
                e.setCodigo(codigo);
                e.setNombre(nombre);
                e.setDescripcion(asString(body.get("descripcion")));
                e.setPaisCodigo(asString(body.get("paisCodigo")));
                e.setOrdenDisplay(asInt(body.get("ordenDisplay")));
                e.setActivo(asBool(body.get("activo"), true));
                yield regimenTributarioRepo.save(e);
            }
            case "estados-civiles" -> {
                EstadoCivil e = new EstadoCivil();
                e.setEmpresaId(empresaId);
                e.setCodigo(codigo);
                e.setNombre(nombre);
                e.setDescripcion(asString(body.get("descripcion")));
                e.setOrdenDisplay(asInt(body.get("ordenDisplay")));
                e.setActivo(asBool(body.get("activo"), true));
                yield estadoCivilRepo.save(e);
            }
            case "paises-placa" -> {
                PaisCodigoPlaca e = new PaisCodigoPlaca();
                e.setEmpresaId(empresaId);
                e.setCodigo(codigo);
                e.setNombre(nombre);
                e.setRegexPlaca(asString(body.get("regexPlaca")));
                e.setOrdenDisplay(asInt(body.get("ordenDisplay")));
                e.setActivo(asBool(body.get("activo"), true));
                yield paisCodigoPlacaRepo.save(e);
            }
            case "tipos-servicio-vehiculo" -> {
                TipoServicioVehiculo e = new TipoServicioVehiculo();
                e.setEmpresaId(empresaId);
                e.setCodigo(codigo);
                e.setNombre(nombre);
                e.setDescripcion(asString(body.get("descripcion")));
                e.setOrdenDisplay(asInt(body.get("ordenDisplay")));
                e.setActivo(asBool(body.get("activo"), true));
                yield tipoServicioVehiculoRepo.save(e);
            }
            case "tipos-acceso-dispositivo" -> {
                TipoAccesoDispositivo e = new TipoAccesoDispositivo();
                e.setEmpresaId(empresaId);
                e.setCodigo(codigo);
                e.setNombre(nombre);
                e.setDescripcion(asString(body.get("descripcion")));
                e.setIcono(asString(body.get("icono")));
                e.setOrdenDisplay(asInt(body.get("ordenDisplay")));
                e.setActivo(asBool(body.get("activo"), true));
                yield tipoAccesoDispositivoRepo.save(e);
            }
            case "canales-origen-reserva" -> {
                CanalOrigenReserva e = new CanalOrigenReserva();
                e.setEmpresaId(empresaId);
                e.setCodigo(codigo);
                e.setNombre(nombre);
                e.setDescripcion(asString(body.get("descripcion")));
                e.setIcono(asString(body.get("icono")));
                e.setOrdenDisplay(asInt(body.get("ordenDisplay")));
                e.setActivo(asBool(body.get("activo"), true));
                yield canalOrigenReservaRepo.save(e);
            }
            default -> throw new BusinessException("Catalogo desconocido", "ERR_CATALOG_UNKNOWN");
        };
        return entity;
    }

    @Transactional
    public Object actualizar(String catalogo, Long id, Map<String, Object> body) {
        Object entity = getById(catalogo, id);
        // Para mantener simple: edita los campos comunes; specifics dispatchea por tipo.
        applyCommonUpdates(entity, body);
        applySpecificUpdates(catalogo, entity, body);
        // Persistir
        @SuppressWarnings("rawtypes")
        JpaRepository repo = repoFor(catalogo);
        return repo.save(entity);
    }

    @Transactional
    public Object archivar(String catalogo, Long id) {
        Object entity = getById(catalogo, id);
        setActivo(entity, false);
        @SuppressWarnings("rawtypes")
        JpaRepository repo = repoFor(catalogo);
        return repo.save(entity);
    }

    @Transactional
    public Object desarchivar(String catalogo, Long id) {
        Object entity = getById(catalogo, id);
        setActivo(entity, true);
        @SuppressWarnings("rawtypes")
        JpaRepository repo = repoFor(catalogo);
        return repo.save(entity);
    }

    /** Helper RBAC: la empresaId del item objetivo (para que el caller decida si puede tocarlo). */
    public Long getEmpresaIdDelItem(String catalogo, Long id) {
        Object item = getById(catalogo, id);
        return getEmpresaIdReflect(item);
    }

    // ───────────────── helpers ─────────────────

    private void applyCommonUpdates(Object entity, Map<String, Object> body) {
        if (body.containsKey("nombre"))        setField(entity, "nombre", asString(body.get("nombre")));
        if (body.containsKey("codigo"))        setField(entity, "codigo", asString(body.get("codigo")));
        if (body.containsKey("descripcion"))   setField(entity, "descripcion", asString(body.get("descripcion")));
        if (body.containsKey("ordenDisplay"))  setField(entity, "ordenDisplay", asInt(body.get("ordenDisplay")));
        if (body.containsKey("activo"))        setField(entity, "activo", asBool(body.get("activo"), true));
    }

    private void applySpecificUpdates(String catalogo, Object entity, Map<String, Object> body) {
        switch (catalogo) {
            case "tipos-documento":
                if (body.containsKey("aplicaPersona")) setField(entity, "aplicaPersona", asBool(body.get("aplicaPersona"), true));
                if (body.containsKey("aplicaEmpresa")) setField(entity, "aplicaEmpresa", asBool(body.get("aplicaEmpresa"), false));
                break;
            case "monedas":
                if (body.containsKey("simbolo"))   setField(entity, "simbolo", asString(body.get("simbolo")));
                if (body.containsKey("decimales")) setField(entity, "decimales", asInt(body.get("decimales")));
                break;
            case "zonas-horarias":
                if (body.containsKey("offsetHoras")) setField(entity, "offsetHoras", asInt(body.get("offsetHoras")));
                break;
            case "unidades-tarifa":
                if (body.containsKey("minutos")) setField(entity, "minutos", asInt(body.get("minutos")));
                break;
            case "regimenes-tributarios":
                if (body.containsKey("paisCodigo")) setField(entity, "paisCodigo", asString(body.get("paisCodigo")));
                break;
            case "paises-placa":
                if (body.containsKey("regexPlaca")) setField(entity, "regexPlaca", asString(body.get("regexPlaca")));
                break;
            case "tipos-acceso-dispositivo":
            case "canales-origen-reserva":
                if (body.containsKey("icono")) setField(entity, "icono", asString(body.get("icono")));
                break;
            default: break;
        }
    }

    private void setActivo(Object entity, Boolean v) {
        setField(entity, "activo", v);
    }

    private Long getEmpresaIdReflect(Object entity) {
        try {
            var m = entity.getClass().getMethod("getEmpresaId");
            return (Long) m.invoke(entity);
        } catch (Exception e) {
            return null;
        }
    }

    private void setField(Object entity, String fieldName, Object value) {
        try {
            String setter = "set" + Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);
            // Find setter
            for (var m : entity.getClass().getMethods()) {
                if (m.getName().equals(setter) && m.getParameterCount() == 1) {
                    m.invoke(entity, value);
                    return;
                }
            }
        } catch (Exception e) {
            throw new BusinessException(
                    "No se pudo asignar campo " + fieldName + ": " + e.getMessage(),
                    "ERR_FIELD_SETTER");
        }
    }

    private static Long asLong(Object v) {
        if (v == null) return null;
        if (v instanceof Number n) return n.longValue();
        try { return Long.parseLong(v.toString()); } catch (Exception e) { return null; }
    }
    private static Integer asInt(Object v) {
        if (v == null) return null;
        if (v instanceof Number n) return n.intValue();
        try { return Integer.parseInt(v.toString()); } catch (Exception e) { return null; }
    }
    private static String asString(Object v) {
        return v == null ? null : v.toString();
    }
    private static Boolean asBool(Object v, boolean def) {
        if (v == null) return def;
        if (v instanceof Boolean b) return b;
        return Boolean.parseBoolean(v.toString());
    }
}
