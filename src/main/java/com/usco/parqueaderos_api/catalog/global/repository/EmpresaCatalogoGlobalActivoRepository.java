package com.usco.parqueaderos_api.catalog.global.repository;

import com.usco.parqueaderos_api.catalog.global.entity.EmpresaCatalogoGlobalActivo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EmpresaCatalogoGlobalActivoRepository
        extends JpaRepository<EmpresaCatalogoGlobalActivo, Long> {

    /** Toda la configuracion de UNA empresa para UN catalogo. */
    List<EmpresaCatalogoGlobalActivo> findByEmpresaIdAndCatalogo(Long empresaId, String catalogo);

    /** Toda la configuracion de la empresa (todos los catalogos). */
    List<EmpresaCatalogoGlobalActivo> findByEmpresaIdOrderByCatalogoAscItemIdAsc(Long empresaId);

    /** Lookup de una regla especifica para upsert. */
    Optional<EmpresaCatalogoGlobalActivo> findByEmpresaIdAndCatalogoAndItemId(
            Long empresaId, String catalogo, Long itemId);

    /** Verifica si la empresa tiene CUALQUIER fila para ese catalogo (decide retrocompat). */
    boolean existsByEmpresaIdAndCatalogo(Long empresaId, String catalogo);
}
