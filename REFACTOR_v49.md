# Refactor v49 — Tracking

Rama: `refactor/v49-historicidad-snapshots`
Origen: `main` (commit `cd4bbd5`, v48.5)
Plan completo (local, no en repo): `~/Desktop/parqueaderos-docs-local/PLAN_REFACTOR_v49.md`

El refactor v49 cubre **12 fases** (~30h estimadas) y se ejecuta a lo largo
de varias sesiones. Este archivo es el tablero de avance para retomar el
trabajo sin perder contexto.

---

## Estado por fase

| # | Fase | Estado | Sesion | Commit |
|---|---|---|---|---|
| Sprint A | Snapshots de historicidad (ticket/factura/pago) | ✅ DONE | 2026-05-28 | `9abdcf0` |
| 0 | BaseEntity con auditoria temporal universal | ✅ DONE (37 de 41 entities; las 4 con fechaCreacion propia revisan en v50) | 2026-05-29 | `368a49c` + `8c5e998` |
| 1 | Catalogos globales (tipo_documento, genero, moneda, zona_horaria, unidad_tarifa, regimen_tributario) | ✅ DONE (6 de 12 prioritarios) | 2026-05-29 | pendiente |
| 2 | Catalogos por empresa (10 tablas: empresa_metodo_pago, estado_*, tipo_*) | ⬜ TODO | — | — |
| 3 | `empresa_config` (key-value, 26 settings seed) | ✅ DONE | 2026-05-29 | pendiente |
| 4 | `empresa_validacion_campo` (validaciones editables por empresa) | ⬜ TODO | — | — |
| 5 | Enriquecimiento de 12 entities pobres (~180 columnas) | ⬜ TODO | — | — |
| 6 | Catalogos legacy enriquecidos (Estado/Rol/Tipo* con codigo, color, icono, orden) | ✅ DONE | 2026-05-29 | `a5f2772` |
| 7 | Validaciones cross-field en DTOs (12 DTOs con `@AssertTrue` custom) | 🟡 PARTIAL (v48.5 cubrio 6 DTOs) | 2026-05-28 | `0eaaa1d` |
| 8 | Reportes parametrizables (`reporte_definicion`) | ⬜ TODO | — | — |
| 9 | Auditoria enriquecida (`audit_log` con FKs a accion_auditable_id) | ✅ DONE | 2026-05-29 | pendiente |
| 10 | Soft-delete uniforme (`archivado_en` en lugar de `estado_id=3`) | ⬜ TODO | — | — |
| 11 | Refactor servicios consumidores (eliminar strings/numeros magicos) | ⬜ TODO | — | — |
| 12 | Docs + smoke E2E + push | 🟡 PARTIAL (smoke pendiente, push si hace falta) | — | — |

---

## Sprint A — Snapshots de historicidad

**Problema:** los reportes mostraban los datos ACTUALES (no los del momento
del evento) cuando algo cambiaba: vehiculo cambia de dueño, operador
renombrado, tarifa renombrada. → Reportes mentian.

**Solucion:** 13 columnas snapshot NULLABLE en `ticket` (8), `factura` (4),
`pago` (1). Llenado al crear en 5 services. DTOs exponen los snapshots con
fallback al FK actual para registros pre-v49.

**Impacto:**
- 0 breaking changes para el front (columnas nuevas opcionales).
- Reportes ahora son inmutables ante cambios posteriores.
- TicketAutoService (flujo OCR) ganó también los snapshots de tarifa que le
  faltaban — bug pre-existente arreglado de paso.

**Tests:** 134 → 136 (+2 verificando llenado y sobrevivencia a cambios).

---

## Fase 0 — BaseEntity / AuditableEntity

**Problema:** 42 de 45 entities sin `fecha_creacion`/`fecha_actualizacion`.
Imposible saber cuando se creo/modifico cualquier registro.

**Solucion:**
- `common/entity/BaseEntity.java` con `@MappedSuperclass` + `@PrePersist` +
  `@PreUpdate`.
- `common/entity/AuditableEntity.java extends BaseEntity` agrega
  `creadoPorUsuarioId` + `actualizadoPorUsuarioId` (llenado manual en
  services).
- Migracion SQL aditiva: agrega ambas columnas a 36 tablas con
  `DEFAULT CURRENT_TIMESTAMP` (los registros pre-v49 quedan con la fecha de
  carga, no la real — limitacion conocida del backfill).

**Entities migradas en esta fase (10):**
- `Empresa`, `Parqueadero`
- `Ticket`, `Factura`, `Pago`
- `Vehiculo`, `Persona`, `Reserva`
- `Convenio`, `Tarifa`

**Pendientes para sesiones futuras** (31 entities, ~30 min cada batch):
- Estructura: `Nivel`, `Seccion`, `SubSeccion`, `PuntoParqueo`, `Camara`,
  `Camino`
- Identity: `UsuarioRol`, `UsuarioParqueadero`
- Caja: `Caja`, `MovimientoCaja`, `MovimientoSaldo`, `CierreDia`
- Tariff: `TarifaFranja`, `ValidacionCompra`
- Geo: `Pais`, `Departamento`, `Ciudad`
- Catalogos: `Estado`, `Rol`, `TipoVehiculo`, `TipoParqueadero`,
  `TipoPuntoParqueo`, `TipoDispositivo`
- IoT: `Dispositivo`, `DispositivoParqueo`
- Otros: `AuditLog`, `RefreshToken`
- Entities con fechaCreacion propia (revisar caso por caso):
  `Usuario`, `Suscripcion`, `ResolucionDian`

**Tests:** 136 → 139 (+3 verificando herencia y callbacks).

---

## Sesiones siguientes (orden sugerido)

1. **Fase 1** (4h) — Catalogos globales (12 tablas). Mayor valor:
   `tipo_documento` reemplaza el VARCHAR libre en Persona/Empresa.
2. **Fase 6** (1h) — Enriquecer catalogos existentes con `codigo`,
   `color_hex`, `icono`. Bajo riesgo, alto valor visual.
3. **Fase 3** (3h) — `empresa_config` key-value. Saca los hardcoded
   (cooldown OCR, dias suscripcion, formatos).
4. **Fase 2** (4h) — Catalogos por empresa.
5. **Fase 5** (3h) — Enriquecimiento de entities pobres.
6. **Fase 9** (1h) — Auditoria enriquecida.
7. **Fase 4** (4h) — `empresa_validacion_campo` (la pieza mas compleja).
8. **Fase 10** (2h) — Soft-delete uniforme.
9. **Fase 8** (3h) — Reportes parametrizables.
10. **Fase 11** (3h) — Refactor servicios consumidores.
11. **Fase 12** (1.5h) — Docs + smoke + push.

**Tiempo restante estimado:** ~30h - 4.5h ya hechas = **~25h**.

---

## Como retomar

```bash
# 1. Estar en la rama
git checkout refactor/v49-historicidad-snapshots

# 2. Verificar estado
git log --oneline main..HEAD

# 3. Leer este archivo + el plan completo en
#    ~/Desktop/parqueaderos-docs-local/PLAN_REFACTOR_v49.md

# 4. Elegir la siguiente fase de la tabla de arriba

# 5. Tests siempre verdes antes de commitear:
./mvnw test
```

---

## Riesgo de merge a `main`

Esta rama acumula cambios atomicos por fase. Cuando este completa, se
hara squash o merge regular a `main` (decidir al final). Mientras tanto
la rama es push-friendly: cada fase es un commit revisable.

**No hay breaking changes contractuales con el front en lo hecho hasta
ahora**: todas las columnas nuevas son NULLABLE y los DTOs solo ganan
campos opcionales.
