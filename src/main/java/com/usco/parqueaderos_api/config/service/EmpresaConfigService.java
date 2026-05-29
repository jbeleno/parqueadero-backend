package com.usco.parqueaderos_api.config.service;

import com.usco.parqueaderos_api.config.entity.EmpresaConfig;
import com.usco.parqueaderos_api.config.repository.EmpresaConfigRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Resuelve valores de configuracion por empresa con cache.
 *
 * Patron de uso desde otros services:
 * <pre>
 *   int cooldown = configService.getInt(empresaId, "ocr.cooldown_segundos", 30);
 *   boolean tel  = configService.getBool(empresaId, "registro.telefono_obligatorio", true);
 *   String fmt   = configService.getString(empresaId, "formato.fecha", "dd/MM/yyyy");
 * </pre>
 *
 * Cache: 'empresaConfig' configurado con ConcurrentMapCacheManager por
 * defecto. Se invalida al actualizar via {@link #update}.
 */
@Service
@RequiredArgsConstructor
public class EmpresaConfigService {

    private final EmpresaConfigRepository repo;

    @Transactional(readOnly = true)
    @Cacheable(value = "empresaConfig", key = "#empresaId + ':' + #clave", unless = "#result == null")
    public String getString(Long empresaId, String clave, String defaultValue) {
        if (empresaId == null) return defaultValue;
        return repo.findByEmpresaIdAndClave(empresaId, clave)
                .map(EmpresaConfig::getValor)
                .orElse(defaultValue);
    }

    @Transactional(readOnly = true)
    public int getInt(Long empresaId, String clave, int defaultValue) {
        String v = getString(empresaId, clave, null);
        if (v == null) return defaultValue;
        try { return Integer.parseInt(v.trim()); }
        catch (NumberFormatException e) { return defaultValue; }
    }

    @Transactional(readOnly = true)
    public double getDouble(Long empresaId, String clave, double defaultValue) {
        String v = getString(empresaId, clave, null);
        if (v == null) return defaultValue;
        try { return Double.parseDouble(v.trim()); }
        catch (NumberFormatException e) { return defaultValue; }
    }

    @Transactional(readOnly = true)
    public boolean getBool(Long empresaId, String clave, boolean defaultValue) {
        String v = getString(empresaId, clave, null);
        if (v == null) return defaultValue;
        return "true".equalsIgnoreCase(v.trim()) || "1".equals(v.trim());
    }

    @Transactional
    @CacheEvict(value = "empresaConfig", key = "#empresaId + ':' + #clave")
    public EmpresaConfig upsert(Long empresaId, String clave, String valor, Long usuarioId) {
        EmpresaConfig cfg = repo.findByEmpresaIdAndClave(empresaId, clave)
                .orElseGet(() -> {
                    EmpresaConfig nuevo = new EmpresaConfig();
                    nuevo.setEmpresaId(empresaId);
                    nuevo.setClave(clave);
                    return nuevo;
                });
        cfg.setValor(valor);
        cfg.setActualizadoPorUsuarioId(usuarioId);
        return repo.save(cfg);
    }
}
