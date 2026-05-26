-- ================================================================
-- Spring Boot data.sql — Seed de datos iniciales
-- Se ejecuta DESPUÉS de que Hibernate crea/actualiza las tablas
-- (spring.sql.init.mode=always + defer-datasource-initialization=true)
-- Idempotente gracias a ON CONFLICT DO NOTHING
-- ================================================================

-- ════════════════════════════════════════════════════════════════
-- 1. ESTADOS
-- ════════════════════════════════════════════════════════════════
INSERT INTO estado (id, nombre, descripcion) VALUES
  (1, 'ACTIVO',    'Registro activo y operativo'),
  (2, 'INACTIVO',  'Registro deshabilitado temporalmente'),
  (3, 'ARCHIVADO', 'Registro eliminado lógicamente (soft-delete)')
ON CONFLICT (id) DO NOTHING;

SELECT setval('estado_id_seq', (SELECT COALESCE(MAX(id),0) FROM estado));

-- ════════════════════════════════════════════════════════════════
-- 2. ROLES
-- ════════════════════════════════════════════════════════════════
INSERT INTO rol (id, nombre, descripcion, estado_id) VALUES
  (1, 'USER',        'Usuario estándar',                    1),
  (2, 'ADMIN',       'Administrador de parqueadero',        1),
  (3, 'SUPER_ADMIN', 'Super administrador del sistema',     1)
ON CONFLICT (id) DO NOTHING;

SELECT setval('rol_id_seq', (SELECT COALESCE(MAX(id),0) FROM rol));

-- ════════════════════════════════════════════════════════════════
-- 3. TIPOS DE VEHÍCULO
-- ════════════════════════════════════════════════════════════════
INSERT INTO tipo_vehiculo (id, nombre, descripcion) VALUES
  (1, 'Carro',     'Vehículo tipo automóvil'),
  (2, 'Moto',      'Vehículo tipo motocicleta'),
  (3, 'Bicicleta', 'Vehículo tipo bicicleta')
ON CONFLICT (id) DO NOTHING;

SELECT setval('tipo_vehiculo_id_seq', (SELECT COALESCE(MAX(id),0) FROM tipo_vehiculo));

-- ════════════════════════════════════════════════════════════════
-- 4. TIPOS DE PARQUEADERO
-- ════════════════════════════════════════════════════════════════
INSERT INTO tipo_parqueadero (id, nombre, descripcion, estado_id) VALUES
  (1, 'Público',        'Parqueadero abierto al público general',     1),
  (2, 'Privado',        'Parqueadero de acceso restringido',          1),
  (3, 'Universitario',  'Parqueadero de institución educativa',       1)
ON CONFLICT (id) DO NOTHING;

SELECT setval('tipo_parqueadero_id_seq', (SELECT COALESCE(MAX(id),0) FROM tipo_parqueadero));

-- ════════════════════════════════════════════════════════════════
-- 5. TIPOS DE PUNTO DE PARQUEO
-- ════════════════════════════════════════════════════════════════
INSERT INTO tipo_punto_parqueo (id, nombre, descripcion, estado_id) VALUES
  (1, 'Placas',         'Punto de parqueo con identificación por placa',       1),
  (2, 'administrativo', 'Punto de parqueo administrativo / reservado',         1),
  (3, 'discapacitado',  'Punto de parqueo para personas con discapacidad',     1)
ON CONFLICT (id) DO NOTHING;

SELECT setval('tipo_punto_parqueo_id_seq', (SELECT COALESCE(MAX(id),0) FROM tipo_punto_parqueo));

-- ════════════════════════════════════════════════════════════════
-- 6. TIPOS DE DISPOSITIVO
-- ════════════════════════════════════════════════════════════════
INSERT INTO tipo_dispositivo (id, nombre, descripcion, estado_id) VALUES
  (1, 'Sensor',  'Sensor de detección de vehículo',    1),
  (2, 'Cámara',  'Cámara de vigilancia / ALPR',        1),
  (3, 'Barrera', 'Barrera de entrada/salida',           1)
ON CONFLICT (id) DO NOTHING;

SELECT setval('tipo_dispositivo_id_seq', (SELECT COALESCE(MAX(id),0) FROM tipo_dispositivo));

-- ════════════════════════════════════════════════════════════════
-- 7. UBICACIÓN: PAÍS → DEPARTAMENTO → CIUDAD
-- ════════════════════════════════════════════════════════════════
INSERT INTO pais (id, nombre, acronimo, identificador_internacional) VALUES
  (1, 'Colombia', 'CO', '+57')
ON CONFLICT (id) DO NOTHING;

SELECT setval('pais_id_seq', (SELECT COALESCE(MAX(id),0) FROM pais));

INSERT INTO departamento (id, pais_id, nombre, identificador_nacional) VALUES
  (1, 1, 'Huila',    '41'),
  (2, 1, 'Tolima',   '73'),
  (3, 1, 'Caquetá',  '18'),
  (4, 1, 'Putumayo', '86'),
  (5, 1, 'Cauca',    '19')
ON CONFLICT (id) DO NOTHING;

SELECT setval('departamento_id_seq', (SELECT COALESCE(MAX(id),0) FROM departamento));

-- ════════════════════════════════════════════════════════════════
-- 7.5. EMPRESA POR DEFECTO (asignada a usuarios que se registran sin empresa)
-- ════════════════════════════════════════════════════════════════
INSERT INTO empresa (id, nombre, descripcion, estado_id) VALUES
  (1, 'Público', 'Empresa por defecto para usuarios sin afiliación a una empresa específica. Permite acceso a parqueaderos públicos.', 1)
ON CONFLICT (id) DO NOTHING;

SELECT setval('empresa_id_seq', GREATEST((SELECT COALESCE(MAX(id),0) FROM empresa), 1));

INSERT INTO ciudad (id, departamento_id, nombre, identificador_departamental) VALUES
  -- Huila
  (1,  1, 'Neiva',       '001'),
  (2,  1, 'Pitalito',    '548'),
  (3,  1, 'Garzón',      '298'),
  (4,  1, 'La Plata',    '396'),
  (5,  1, 'Campoalegre', '130'),
  -- Tolima
  (6,  2, 'Ibagué',      '001'),
  (7,  2, 'Espinal',     '226'),
  -- Caquetá
  (8,  3, 'Florencia',   '001'),
  -- Putumayo
  (9,  4, 'Mocoa',       '001'),
  -- Cauca
  (10, 5, 'Popayán',     '001')
ON CONFLICT (id) DO NOTHING;

SELECT setval('ciudad_id_seq', (SELECT COALESCE(MAX(id),0) FROM ciudad));

-- ════════════════════════════════════════════════════════════════
-- 8. INDICES OPERATIVOS (no los crea Hibernate ddl-auto)
-- ════════════════════════════════════════════════════════════════

-- Anti-sobreventa: solo puede haber UN ticket EN_CURSO por punto de parqueo.
-- Indice unico parcial. Si dos requests intentan crear ticket EN_CURSO
-- en el mismo punto, la segunda inserción falla con violacion de unicidad.
CREATE UNIQUE INDEX IF NOT EXISTS uniq_ticket_punto_en_curso
  ON ticket (punto_parqueo_id)
  WHERE estado = 'EN_CURSO';

-- Cleanup defensivo: si ya hay duplicados historicos (de antes del indice),
-- cierra los mas recientes y deja solo el mas antiguo de cada (vehiculo, parqueadero).
-- Idempotente: si no hay duplicados, el UPDATE no afecta filas.
UPDATE ticket SET
    estado = 'CERRADO',
    fecha_hora_salida = COALESCE(fecha_hora_salida, NOW())
WHERE estado = 'EN_CURSO'
  AND id NOT IN (
    SELECT MIN(id) FROM ticket
    WHERE estado = 'EN_CURSO'
    GROUP BY vehiculo_id, parqueadero_id
  );

-- Anti-duplicado de placa: el mismo vehiculo no puede tener dos tickets
-- EN_CURSO en el mismo parqueadero. Funciona como defensa en profundidad
-- ante race conditions: si la verificacion existsBy del service pasa por
-- ambas transacciones concurrentes, el motor rechaza el segundo INSERT.
-- El service captura DataIntegrityViolationException y retorna ENTRADA_DUPLICADA.
CREATE UNIQUE INDEX IF NOT EXISTS uniq_ticket_vehiculo_parqueadero_en_curso
  ON ticket (vehiculo_id, parqueadero_id)
  WHERE estado = 'EN_CURSO';

-- Indices hot-path para queries de filtrado/conteo
CREATE INDEX IF NOT EXISTS idx_ticket_estado            ON ticket (estado);
CREATE INDEX IF NOT EXISTS idx_ticket_parqueadero       ON ticket (parqueadero_id);
CREATE INDEX IF NOT EXISTS idx_ticket_vehiculo          ON ticket (vehiculo_id);
CREATE INDEX IF NOT EXISTS idx_reserva_estado_fechas    ON reserva (estado, fecha_hora_inicio, fecha_hora_fin);
CREATE INDEX IF NOT EXISTS idx_reserva_punto            ON reserva (punto_parqueo_id);
CREATE INDEX IF NOT EXISTS idx_factura_ticket           ON factura (ticket_id);
CREATE INDEX IF NOT EXISTS idx_factura_estado           ON factura (estado);
CREATE INDEX IF NOT EXISTS idx_pago_factura             ON pago (factura_id);

-- ============================================================================
-- Suscripciones (MENSUAL / PASE_DIA / ABONO_PREPAGO)
-- ============================================================================

-- Solo una Suscripcion ACTIVA del mismo tipo por (vehiculo, parqueadero).
-- Defensa en profundidad contra race condition al crear suscripciones.
CREATE UNIQUE INDEX IF NOT EXISTS uniq_suscripcion_activa
  ON suscripcion (vehiculo_id, parqueadero_id, tipo)
  WHERE estado = 'ACTIVA';

CREATE INDEX IF NOT EXISTS idx_suscripcion_vehiculo  ON suscripcion (vehiculo_id);
CREATE INDEX IF NOT EXISTS idx_suscripcion_parq      ON suscripcion (parqueadero_id);
CREATE INDEX IF NOT EXISTS idx_suscripcion_estado    ON suscripcion (estado);
-- Job @Scheduled busca por (estado=ACTIVA, fecha_fin) para marcar VENCIDA
CREATE INDEX IF NOT EXISTS idx_suscripcion_vence
  ON suscripcion (fecha_fin)
  WHERE estado = 'ACTIVA';

CREATE INDEX IF NOT EXISTS idx_mov_saldo_suscripcion
  ON movimiento_saldo (suscripcion_id, fecha DESC);

-- Indices para queries multi-tenant (joins por empresa) y jerarquia
CREATE INDEX IF NOT EXISTS idx_parqueadero_empresa      ON parqueadero (empresa_id);
CREATE INDEX IF NOT EXISTS idx_nivel_parqueadero        ON nivel (parqueadero_id);
CREATE INDEX IF NOT EXISTS idx_seccion_parqueadero      ON seccion (parqueadero_id);
CREATE INDEX IF NOT EXISTS idx_seccion_nivel            ON seccion (nivel_id);
CREATE INDEX IF NOT EXISTS idx_subseccion_seccion       ON sub_seccion (seccion_id);
CREATE INDEX IF NOT EXISTS idx_punto_subseccion         ON punto_parqueo (sub_seccion_id);
CREATE INDEX IF NOT EXISTS idx_camino_nivel             ON camino (nivel_id);
CREATE INDEX IF NOT EXISTS idx_camara_nivel             ON camara (nivel_id);
CREATE INDEX IF NOT EXISTS idx_dispositivo_parqueadero  ON dispositivo (parqueadero_id);
CREATE INDEX IF NOT EXISTS idx_tarifa_parqueadero       ON tarifa (parqueadero_id);
CREATE INDEX IF NOT EXISTS idx_vehiculo_persona         ON vehiculo (persona_id);
CREATE INDEX IF NOT EXISTS idx_usuario_empresa          ON usuario (empresa_id);
CREATE INDEX IF NOT EXISTS idx_usuario_correo           ON usuario (correo);
CREATE INDEX IF NOT EXISTS idx_reserva_usuario          ON reserva (usuario_id);
CREATE INDEX IF NOT EXISTS idx_reserva_parqueadero      ON reserva (parqueadero_id);
CREATE INDEX IF NOT EXISTS idx_factura_parqueadero      ON factura (parqueadero_id);
CREATE INDEX IF NOT EXISTS idx_factura_vehiculo         ON factura (vehiculo_id);

-- ════════════════════════════════════════════════════════════════
-- 12. MIGRACIONES IDEMPOTENTES (defensa contra ddl-auto en validate)
-- Garantiza que columnas nuevas existan aunque Hibernate no las cree.
-- ════════════════════════════════════════════════════════════════

-- Tarifa: Modelo B + IVA configurable + suscripciones.
-- IMPORTANTE: usar SIN NOT NULL aqui. Las columnas pueden existir ya en prod
-- como nullable con valores NULL; el NOT NULL queda como default Java en la entity
-- (relajado a nullable en JPA para no romper el arranque). El UPDATE posterior
-- normaliza valores nulos a 0.
ALTER TABLE tarifa ADD COLUMN IF NOT EXISTS minutos_gracia                INTEGER          DEFAULT 0;
ALTER TABLE tarifa ADD COLUMN IF NOT EXISTS valor_minimo                  DOUBLE PRECISION DEFAULT 0;
ALTER TABLE tarifa ADD COLUMN IF NOT EXISTS minutos_cubiertos_por_minimo  INTEGER          DEFAULT 0;
ALTER TABLE tarifa ADD COLUMN IF NOT EXISTS precio_mensualidad            DOUBLE PRECISION;
ALTER TABLE tarifa ADD COLUMN IF NOT EXISTS precio_pase_dia               DOUBLE PRECISION;
ALTER TABLE tarifa ADD COLUMN IF NOT EXISTS aplica_iva                    BOOLEAN          DEFAULT FALSE;
ALTER TABLE tarifa ADD COLUMN IF NOT EXISTS iva_porcentaje                DOUBLE PRECISION DEFAULT 0;
ALTER TABLE tarifa ADD COLUMN IF NOT EXISTS valor_minimo_reemplaza        BOOLEAN          DEFAULT FALSE;

-- Normalizar NULLs heredados (columnas que ya existian en prod con NULL).
UPDATE tarifa SET minutos_gracia                = 0     WHERE minutos_gracia IS NULL;
UPDATE tarifa SET valor_minimo                  = 0     WHERE valor_minimo IS NULL;
UPDATE tarifa SET minutos_cubiertos_por_minimo  = 0     WHERE minutos_cubiertos_por_minimo IS NULL;
UPDATE tarifa SET aplica_iva                    = FALSE WHERE aplica_iva IS NULL;
UPDATE tarifa SET iva_porcentaje                = 0     WHERE iva_porcentaje IS NULL;

-- Ticket: vinculo opcional con suscripcion + confirmacion fisica de salida
ALTER TABLE ticket ADD COLUMN IF NOT EXISTS suscripcion_id          BIGINT;
ALTER TABLE ticket ADD COLUMN IF NOT EXISTS fecha_hora_salida_fisica TIMESTAMP;

-- Parqueadero: tope legal por minuto (regulado en algunas ciudades)
ALTER TABLE parqueadero ADD COLUMN IF NOT EXISTS tarifa_maxima_por_minuto DOUBLE PRECISION;

-- Empresa: modo de operacion y NIT
ALTER TABLE empresa ADD COLUMN IF NOT EXISTS modo_operacion VARCHAR(20) DEFAULT 'INFORMAL';
ALTER TABLE empresa ADD COLUMN IF NOT EXISTS nit            VARCHAR(30);
UPDATE empresa SET modo_operacion = 'INFORMAL' WHERE modo_operacion IS NULL;

-- Factura: desagregado de IVA (formal -> base + iva + total).
ALTER TABLE factura ADD COLUMN IF NOT EXISTS base_imponible  DOUBLE PRECISION;
ALTER TABLE factura ADD COLUMN IF NOT EXISTS iva_monto       DOUBLE PRECISION;
ALTER TABLE factura ADD COLUMN IF NOT EXISTS iva_porcentaje  DOUBLE PRECISION;

-- v41: trazabilidad de origen + UNIQUE INDEX parcial anti-duplicado.
-- Cleanup defensivo PRIMERO: si por race historica hay >1 factura no-anulada
-- para un mismo ticket, archiva los duplicados mas nuevos preservando el original.
ALTER TABLE factura ADD COLUMN IF NOT EXISTS origen VARCHAR(50) DEFAULT 'MANUAL';
UPDATE factura SET origen = 'MANUAL' WHERE origen IS NULL;

WITH duplicados AS (
    SELECT id, ticket_id,
           ROW_NUMBER() OVER (PARTITION BY ticket_id ORDER BY id ASC) AS rn
    FROM factura
    WHERE estado <> 'ANULADA'
)
UPDATE factura
SET estado = 'ANULADA'
WHERE id IN (SELECT id FROM duplicados WHERE rn > 1);

-- UNIQUE parcial: solo aplica a facturas vigentes. Permite que un ticket
-- tenga 1 factura activa + N anuladas historicas (correccion contable).
CREATE UNIQUE INDEX IF NOT EXISTS uniq_factura_ticket_no_anulada
    ON factura (ticket_id) WHERE estado <> 'ANULADA';

-- v39: Pago anulable (G1)
ALTER TABLE pago ADD COLUMN IF NOT EXISTS motivo_anulacion       VARCHAR(300);
ALTER TABLE pago ADD COLUMN IF NOT EXISTS anulado_en             TIMESTAMP;
ALTER TABLE pago ADD COLUMN IF NOT EXISTS anulado_por_usuario_id BIGINT;

-- v39: Ticket anulable con motivo (G2) + snapshot de tarifa (G11)
ALTER TABLE ticket ADD COLUMN IF NOT EXISTS motivo_anulacion       VARCHAR(300);
ALTER TABLE ticket ADD COLUMN IF NOT EXISTS anulado_en             TIMESTAMP;
ALTER TABLE ticket ADD COLUMN IF NOT EXISTS anulado_por_usuario_id BIGINT;
ALTER TABLE ticket ADD COLUMN IF NOT EXISTS tarifa_valor_snapshot  DOUBLE PRECISION;
ALTER TABLE ticket ADD COLUMN IF NOT EXISTS tarifa_unidad_snapshot VARCHAR(50);
ALTER TABLE ticket ADD COLUMN IF NOT EXISTS tarifa_minimo_snapshot DOUBLE PRECISION;
ALTER TABLE ticket ADD COLUMN IF NOT EXISTS tarifa_gracia_snapshot INTEGER;
ALTER TABLE ticket ADD COLUMN IF NOT EXISTS tarifa_cubre_snapshot  INTEGER;

-- v39: Soft-delete + visitante en Vehiculo (G3, G5).
-- IMPORTANTE: persona_id se permite NULL para visitantes.
ALTER TABLE vehiculo ADD COLUMN IF NOT EXISTS activo            BOOLEAN DEFAULT TRUE;
ALTER TABLE vehiculo ADD COLUMN IF NOT EXISTS archivado_en      TIMESTAMP;
ALTER TABLE vehiculo ADD COLUMN IF NOT EXISTS es_visitante      BOOLEAN DEFAULT FALSE;
ALTER TABLE vehiculo ADD COLUMN IF NOT EXISTS ultima_actividad  TIMESTAMP;
UPDATE vehiculo SET activo = TRUE       WHERE activo IS NULL;
UPDATE vehiculo SET es_visitante = FALSE WHERE es_visitante IS NULL;
ALTER TABLE vehiculo ALTER COLUMN persona_id DROP NOT NULL;
CREATE INDEX IF NOT EXISTS idx_vehiculo_activo ON vehiculo (activo);

-- v39: Suscripcion con punto reservado (G7)
ALTER TABLE suscripcion ADD COLUMN IF NOT EXISTS punto_parqueo_reservado_id BIGINT
    REFERENCES punto_parqueo(id);
CREATE INDEX IF NOT EXISTS idx_suscripcion_punto_reservado
    ON suscripcion (punto_parqueo_reservado_id);

-- v39: Cierre diario (G12)
CREATE TABLE IF NOT EXISTS cierre_dia (
    id BIGSERIAL PRIMARY KEY,
    parqueadero_id BIGINT NOT NULL REFERENCES parqueadero(id),
    fecha DATE NOT NULL,
    tickets_cerrados INTEGER,
    total_cobrado    DOUBLE PRECISION,
    total_efectivo   DOUBLE PRECISION,
    total_tarjeta    DOUBLE PRECISION,
    total_otros      DOUBLE PRECISION,
    facturas_emitidas INTEGER,
    total_pendiente  DOUBLE PRECISION,
    tickets_anulados INTEGER,
    generado_en      TIMESTAMP DEFAULT NOW(),
    UNIQUE(parqueadero_id, fecha)
);
CREATE INDEX IF NOT EXISTS idx_cierre_dia_parq ON cierre_dia (parqueadero_id, fecha);

-- Tarifa franja horaria (entidad nueva)
CREATE TABLE IF NOT EXISTS tarifa_franja (
    id BIGSERIAL PRIMARY KEY,
    tarifa_id BIGINT NOT NULL REFERENCES tarifa(id),
    nombre VARCHAR(50) NOT NULL,
    hora_inicio TIME NOT NULL,
    hora_fin TIME NOT NULL,
    valor DOUBLE PRECISION NOT NULL,
    solo_fines_de_semana BOOLEAN NOT NULL DEFAULT FALSE,
    activa BOOLEAN NOT NULL DEFAULT TRUE
);
CREATE INDEX IF NOT EXISTS idx_tarifa_franja_tarifa ON tarifa_franja (tarifa_id);

-- Suscripcion (por si Hibernate no la creo aun en algun entorno)
CREATE TABLE IF NOT EXISTS suscripcion (
    id BIGSERIAL PRIMARY KEY,
    vehiculo_id    BIGINT NOT NULL REFERENCES vehiculo(id),
    parqueadero_id BIGINT NOT NULL REFERENCES parqueadero(id),
    tarifa_id      BIGINT NOT NULL REFERENCES tarifa(id),
    tipo           VARCHAR(20) NOT NULL,
    estado         VARCHAR(20) NOT NULL,
    fecha_inicio   TIMESTAMP NOT NULL,
    fecha_fin      TIMESTAMP NOT NULL,
    monto_pagado   DOUBLE PRECISION NOT NULL,
    saldo_restante DOUBLE PRECISION,
    version        BIGINT NOT NULL DEFAULT 0,
    fecha_creacion TIMESTAMP NOT NULL DEFAULT NOW()
);

-- MovimientoSaldo (Event Sourcing del saldo). Debe matchear exactamente la
-- entidad MovimientoSaldo.java. Si Hibernate ya creo la tabla con ddl-auto,
-- los ALTER posteriores aseguran columnas adicionales sin pisar datos.
CREATE TABLE IF NOT EXISTS movimiento_saldo (
    id BIGSERIAL PRIMARY KEY,
    suscripcion_id   BIGINT NOT NULL REFERENCES suscripcion(id),
    monto            DOUBLE PRECISION NOT NULL,
    ticket_id        BIGINT REFERENCES ticket(id),
    pago_id          BIGINT REFERENCES pago(id),
    saldo_resultante DOUBLE PRECISION NOT NULL,
    motivo           VARCHAR(200),
    fecha            TIMESTAMP NOT NULL DEFAULT NOW()
);
ALTER TABLE movimiento_saldo ADD COLUMN IF NOT EXISTS pago_id          BIGINT REFERENCES pago(id);
ALTER TABLE movimiento_saldo ADD COLUMN IF NOT EXISTS saldo_resultante DOUBLE PRECISION;
ALTER TABLE movimiento_saldo ADD COLUMN IF NOT EXISTS motivo           VARCHAR(200);
ALTER TABLE movimiento_saldo ADD COLUMN IF NOT EXISTS fecha            TIMESTAMP DEFAULT NOW();
ALTER TABLE movimiento_saldo ADD COLUMN IF NOT EXISTS tipo VARCHAR(20);
-- Backfill: heuristica por signo del monto
UPDATE movimiento_saldo SET tipo = CASE
  WHEN tipo IS NOT NULL THEN tipo
  WHEN monto > 0 THEN 'ABONO'
  WHEN monto < 0 THEN 'CONSUMO'
  ELSE 'AJUSTE'
END WHERE tipo IS NULL;
CREATE INDEX IF NOT EXISTS idx_mov_saldo_susc ON movimiento_saldo (suscripcion_id);
CREATE INDEX IF NOT EXISTS idx_mov_saldo_ticket ON movimiento_saldo (ticket_id);

-- Convenios y validacion de compras (descuento por ticket de comercio)
CREATE TABLE IF NOT EXISTS convenio (
    id BIGSERIAL PRIMARY KEY,
    parqueadero_id BIGINT NOT NULL REFERENCES parqueadero(id),
    nombre_comercio VARCHAR(200) NOT NULL,
    nit_comercio VARCHAR(30),
    tipo_descuento VARCHAR(30) NOT NULL,
    valor_descuento DOUBLE PRECISION,
    porcentaje_descuento DOUBLE PRECISION,
    minutos_gratis INTEGER,
    monto_minimo_compra DOUBLE PRECISION,
    fecha_inicio_vigencia TIMESTAMP,
    fecha_fin_vigencia TIMESTAMP,
    activo BOOLEAN NOT NULL DEFAULT TRUE
);
CREATE INDEX IF NOT EXISTS idx_convenio_parqueadero ON convenio (parqueadero_id);

CREATE TABLE IF NOT EXISTS validacion_compra (
    id BIGSERIAL PRIMARY KEY,
    ticket_id BIGINT NOT NULL REFERENCES ticket(id),
    convenio_id BIGINT NOT NULL REFERENCES convenio(id),
    monto_compra DOUBLE PRECISION NOT NULL,
    folio_externo VARCHAR(100),
    fecha_aplicacion TIMESTAMP NOT NULL DEFAULT NOW(),
    descuento_aplicado DOUBLE PRECISION
);
CREATE INDEX IF NOT EXISTS idx_validacion_ticket  ON validacion_compra (ticket_id);
CREATE INDEX IF NOT EXISTS idx_validacion_conv    ON validacion_compra (convenio_id);

-- ════════════════════════════════════════════════════════════════
-- v43: AUDITORIA UNIVERSAL (append-only)
-- ════════════════════════════════════════════════════════════════
CREATE TABLE IF NOT EXISTS audit_log (
    id BIGSERIAL PRIMARY KEY,
    tabla            VARCHAR(80)  NOT NULL,
    registro_id      BIGINT,
    accion           VARCHAR(30)  NOT NULL,
    usuario_id       BIGINT,
    empresa_id       BIGINT,
    origen           VARCHAR(30),
    motivo           VARCHAR(500),
    valores_antes    JSONB,
    valores_despues  JSONB,
    request_id       VARCHAR(80),
    endpoint         VARCHAR(200),
    ip               VARCHAR(45),
    user_agent       VARCHAR(300),
    fecha_hora       TIMESTAMP NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_audit_tabla_registro ON audit_log (tabla, registro_id);
CREATE INDEX IF NOT EXISTS idx_audit_usuario_fecha  ON audit_log (usuario_id, fecha_hora DESC);
CREATE INDEX IF NOT EXISTS idx_audit_empresa_fecha  ON audit_log (empresa_id, fecha_hora DESC);
CREATE INDEX IF NOT EXISTS idx_audit_fecha          ON audit_log (fecha_hora DESC);
CREATE INDEX IF NOT EXISTS idx_audit_accion         ON audit_log (accion);

-- ════════════════════════════════════════════════════════════════
-- v43: ROLES NUEVOS + asignacion granular
-- ════════════════════════════════════════════════════════════════
INSERT INTO rol (id, nombre, descripcion, estado_id) VALUES
  (4, 'ADMIN_PARQUEADERO', 'Administrador de uno o varios parqueaderos de la empresa', 1),
  (5, 'OPERARIO_CAJA',    'Cajero del turno: opera tickets, pagos y caja', 1)
ON CONFLICT (id) DO NOTHING;

SELECT setval('rol_id_seq', (SELECT COALESCE(MAX(id),0) FROM rol));

CREATE TABLE IF NOT EXISTS usuario_parqueadero (
    usuario_id              BIGINT NOT NULL REFERENCES usuario(id),
    parqueadero_id          BIGINT NOT NULL REFERENCES parqueadero(id),
    rol_id                  BIGINT NOT NULL REFERENCES rol(id),
    asignado_en             TIMESTAMP NOT NULL DEFAULT NOW(),
    asignado_por_usuario_id BIGINT REFERENCES usuario(id),
    activo                  BOOLEAN NOT NULL DEFAULT TRUE,
    motivo_desasignacion    VARCHAR(500),
    desasignado_en          TIMESTAMP,
    PRIMARY KEY (usuario_id, parqueadero_id, rol_id)
);
CREATE INDEX IF NOT EXISTS idx_usrparq_usr  ON usuario_parqueadero (usuario_id);
CREATE INDEX IF NOT EXISTS idx_usrparq_parq ON usuario_parqueadero (parqueadero_id);

-- ════════════════════════════════════════════════════════════════
-- v43: GESTION DE CAJA (G13)
-- ════════════════════════════════════════════════════════════════
CREATE TABLE IF NOT EXISTS caja (
    id BIGSERIAL PRIMARY KEY,
    parqueadero_id  BIGINT NOT NULL REFERENCES parqueadero(id),
    usuario_id      BIGINT NOT NULL REFERENCES usuario(id),
    nombre          VARCHAR(100),
    fondo_inicial   DOUBLE PRECISION NOT NULL,
    saldo_calculado DOUBLE PRECISION NOT NULL,
    saldo_contado   DOUBLE PRECISION,
    diferencia      DOUBLE PRECISION,
    estado          VARCHAR(20) NOT NULL,
    abierta_en      TIMESTAMP NOT NULL DEFAULT NOW(),
    cerrada_en      TIMESTAMP,
    observaciones_apertura TEXT,
    observaciones_cierre   TEXT
);
CREATE INDEX IF NOT EXISTS idx_caja_parq_estado   ON caja (parqueadero_id, estado);
CREATE INDEX IF NOT EXISTS idx_caja_usuario_estado ON caja (usuario_id, estado);
-- Un usuario solo puede tener 1 caja ABIERTA a la vez
CREATE UNIQUE INDEX IF NOT EXISTS uniq_caja_abierta_por_usuario
    ON caja (usuario_id) WHERE estado = 'ABIERTA';

CREATE TABLE IF NOT EXISTS movimiento_caja (
    id BIGSERIAL PRIMARY KEY,
    caja_id          BIGINT NOT NULL REFERENCES caja(id),
    tipo             VARCHAR(30) NOT NULL,
    monto            DOUBLE PRECISION NOT NULL,
    pago_id          BIGINT REFERENCES pago(id),
    motivo           VARCHAR(500),
    usuario_id       BIGINT NOT NULL REFERENCES usuario(id),
    saldo_resultante DOUBLE PRECISION NOT NULL,
    fecha_hora       TIMESTAMP NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_movcaja_caja  ON movimiento_caja (caja_id, fecha_hora);
CREATE INDEX IF NOT EXISTS idx_movcaja_pago  ON movimiento_caja (pago_id);

-- ════════════════════════════════════════════════════════════════
-- v43: SOFT-DELETE en Tarifa (era unica con DELETE fisico)
-- ════════════════════════════════════════════════════════════════
ALTER TABLE tarifa ADD COLUMN IF NOT EXISTS activo                 BOOLEAN DEFAULT TRUE;
ALTER TABLE tarifa ADD COLUMN IF NOT EXISTS archivado_en           TIMESTAMP;
ALTER TABLE tarifa ADD COLUMN IF NOT EXISTS archivado_por_usuario_id BIGINT REFERENCES usuario(id);
ALTER TABLE tarifa ADD COLUMN IF NOT EXISTS motivo_archivado       VARCHAR(500);
UPDATE tarifa SET activo = TRUE WHERE activo IS NULL;
CREATE INDEX IF NOT EXISTS idx_tarifa_activo ON tarifa (activo);
