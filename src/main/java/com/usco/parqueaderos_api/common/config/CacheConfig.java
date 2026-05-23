package com.usco.parqueaderos_api.common.config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Configuration;

/**
 * Activa el soporte de @Cacheable. Por default usa ConcurrentMapCacheManager
 * (cache en memoria del proceso, suficiente para catalogos que cambian poco).
 *
 * Caches definidos via @Cacheable("nombre") en CatalogService:
 *  - estados, roles, tiposParqueadero, tiposPuntoParqueo, tiposVehiculo, tiposDispositivo
 *
 * @CacheEvict en los metodos save/archive de cada catalogo invalida la entrada.
 * Si en el futuro se escala a multiples instancias, cambiar a Redis/Caffeine.
 */
@Configuration
@EnableCaching
public class CacheConfig {
}
