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

-- v46: Trazabilidad de operador. Quien registro la entrada y quien la salida.
ALTER TABLE ticket ADD COLUMN IF NOT EXISTS creado_por_usuario_id  BIGINT REFERENCES usuario(id);
ALTER TABLE ticket ADD COLUMN IF NOT EXISTS cerrado_por_usuario_id BIGINT REFERENCES usuario(id);
CREATE INDEX IF NOT EXISTS idx_ticket_creado_por  ON ticket (creado_por_usuario_id);
CREATE INDEX IF NOT EXISTS idx_ticket_cerrado_por ON ticket (cerrado_por_usuario_id);

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

-- ════════════════════════════════════════════════════════════════
-- v44: CONFIGURACION DE RECIBO editable + auditada (Resolucion DIAN)
-- ════════════════════════════════════════════════════════════════
ALTER TABLE parqueadero ADD COLUMN IF NOT EXISTS resolucion_dian    TEXT;
ALTER TABLE parqueadero ADD COLUMN IF NOT EXISTS pie_recibo         TEXT;
ALTER TABLE parqueadero ADD COLUMN IF NOT EXISTS encabezado_recibo  TEXT;
ALTER TABLE parqueadero ADD COLUMN IF NOT EXISTS regimen_tributario VARCHAR(100);
ALTER TABLE parqueadero ADD COLUMN IF NOT EXISTS logo_url           VARCHAR(300);

-- ════════════════════════════════════════════════════════════════
-- v47: Tabla resolucion_dian (multi-resolucion + selector + snapshot historico)
-- ════════════════════════════════════════════════════════════════
CREATE TABLE IF NOT EXISTS resolucion_dian (
    id                       BIGSERIAL PRIMARY KEY,
    parqueadero_id           BIGINT NOT NULL REFERENCES parqueadero(id),
    numero_resolucion        VARCHAR(50)  NOT NULL,
    fecha_resolucion         DATE         NOT NULL,
    tipo_resolucion          VARCHAR(30),
    modalidad                VARCHAR(30),
    prefijo                  VARCHAR(20),
    rango_inicial            BIGINT       NOT NULL,
    rango_final              BIGINT       NOT NULL,
    consecutivo_actual       BIGINT       DEFAULT 0,
    vigente_desde            DATE         NOT NULL,
    vigente_hasta            DATE         NOT NULL,
    nombre                   VARCHAR(150) NOT NULL,
    descripcion              TEXT,
    regimen_tributario       VARCHAR(100),
    principal                BOOLEAN      NOT NULL DEFAULT FALSE,
    creado_por_usuario_id    BIGINT REFERENCES usuario(id),
    fecha_creacion           TIMESTAMP DEFAULT NOW(),
    archivada_en             TIMESTAMP,
    motivo_archivado         VARCHAR(300),
    archivado_por_usuario_id BIGINT REFERENCES usuario(id)
);
CREATE INDEX IF NOT EXISTS idx_resol_parq      ON resolucion_dian (parqueadero_id);
CREATE INDEX IF NOT EXISTS idx_resol_principal ON resolucion_dian (parqueadero_id, principal);
-- Garantia: UNA sola resolucion principal NO archivada por parqueadero.
CREATE UNIQUE INDEX IF NOT EXISTS uniq_resol_principal_activa
    ON resolucion_dian (parqueadero_id) WHERE principal = TRUE AND archivada_en IS NULL;

-- FK snapshot historico en factura: cada factura referencia la resolucion usada.
ALTER TABLE factura ADD COLUMN IF NOT EXISTS resolucion_dian_id BIGINT REFERENCES resolucion_dian(id);
CREATE INDEX IF NOT EXISTS idx_factura_resolucion ON factura (resolucion_dian_id);

-- Seed: por cada parqueadero con resolucion_dian (TEXT) no vacio, crear 1 fila
-- inicial en resolucion_dian con principal=TRUE. Idempotente: no inserta si
-- el parqueadero ya tiene alguna resolucion.
INSERT INTO resolucion_dian (
    parqueadero_id, numero_resolucion, fecha_resolucion,
    rango_inicial, rango_final, vigente_desde, vigente_hasta,
    nombre, descripcion, regimen_tributario, principal, fecha_creacion
)
SELECT p.id,
       'MIGRADO-' || p.id                                       AS numero_resolucion,
       CURRENT_DATE                                             AS fecha_resolucion,
       1                                                        AS rango_inicial,
       50000                                                    AS rango_final,
       CURRENT_DATE                                             AS vigente_desde,
       (CURRENT_DATE + INTERVAL '24 months')::DATE              AS vigente_hasta,
       'Migrado desde texto libre'                              AS nombre,
       p.resolucion_dian                                        AS descripcion,
       p.regimen_tributario                                     AS regimen_tributario,
       TRUE                                                     AS principal,
       NOW()                                                    AS fecha_creacion
  FROM parqueadero p
 WHERE p.resolucion_dian IS NOT NULL
   AND TRIM(p.resolucion_dian) <> ''
   AND NOT EXISTS (
       SELECT 1 FROM resolucion_dian r WHERE r.parqueadero_id = p.id
   );

-- ════════════════════════════════════════════════════════════════
-- v48: Tracking directo de usuario en entidades operativas
-- Completa la auditoria que ya existe en audit_log con columnas en
-- la tabla principal para queries de reporteria sin JOIN.
-- ════════════════════════════════════════════════════════════════

-- Pago: quien registro el cobro
ALTER TABLE pago ADD COLUMN IF NOT EXISTS creado_por_usuario_id BIGINT REFERENCES usuario(id);
CREATE INDEX IF NOT EXISTS idx_pago_creado_por ON pago (creado_por_usuario_id);

-- Factura: quien la emitio
ALTER TABLE factura ADD COLUMN IF NOT EXISTS emitido_por_usuario_id BIGINT REFERENCES usuario(id);
CREATE INDEX IF NOT EXISTS idx_factura_emitido_por ON factura (emitido_por_usuario_id);

-- Suscripcion: quien creo y quien cancelo
ALTER TABLE suscripcion ADD COLUMN IF NOT EXISTS creado_por_usuario_id    BIGINT REFERENCES usuario(id);
ALTER TABLE suscripcion ADD COLUMN IF NOT EXISTS cancelado_por_usuario_id BIGINT REFERENCES usuario(id);
ALTER TABLE suscripcion ADD COLUMN IF NOT EXISTS cancelado_en             TIMESTAMP;
CREATE INDEX IF NOT EXISTS idx_suscripcion_creado_por    ON suscripcion (creado_por_usuario_id);
CREATE INDEX IF NOT EXISTS idx_suscripcion_cancelado_por ON suscripcion (cancelado_por_usuario_id);

-- MovimientoSaldo: quien hizo el abono/consumo/ajuste
ALTER TABLE movimiento_saldo ADD COLUMN IF NOT EXISTS registrado_por_usuario_id BIGINT REFERENCES usuario(id);
CREATE INDEX IF NOT EXISTS idx_mov_saldo_registrado_por ON movimiento_saldo (registrado_por_usuario_id);

-- Convenio: quien lo creo y quien lo desactivo
ALTER TABLE convenio ADD COLUMN IF NOT EXISTS creado_por_usuario_id      BIGINT REFERENCES usuario(id);
ALTER TABLE convenio ADD COLUMN IF NOT EXISTS desactivado_por_usuario_id BIGINT REFERENCES usuario(id);
ALTER TABLE convenio ADD COLUMN IF NOT EXISTS desactivado_en             TIMESTAMP;
CREATE INDEX IF NOT EXISTS idx_convenio_creado_por      ON convenio (creado_por_usuario_id);

-- ValidacionCompra: quien la registro
ALTER TABLE validacion_compra ADD COLUMN IF NOT EXISTS registrado_por_usuario_id BIGINT REFERENCES usuario(id);
CREATE INDEX IF NOT EXISTS idx_validacion_registrado_por ON validacion_compra (registrado_por_usuario_id);

-- ────────────────────────────────────────────────────────────────
-- BACKFILL desde audit_log: completa los registros viejos donde
-- la nueva columna esta NULL. Idempotente (solo toca NULLs).
-- ────────────────────────────────────────────────────────────────

-- Pago.creado_por_usuario_id <- audit_log de CREATE
UPDATE pago p
   SET creado_por_usuario_id = a.usuario_id
  FROM audit_log a
 WHERE a.tabla = 'pago'
   AND a.accion = 'CREATE'
   AND a.registro_id = p.id
   AND p.creado_por_usuario_id IS NULL
   AND a.usuario_id IS NOT NULL;

-- Factura.emitido_por_usuario_id <- audit_log
UPDATE factura f
   SET emitido_por_usuario_id = a.usuario_id
  FROM audit_log a
 WHERE a.tabla = 'factura'
   AND a.accion = 'CREATE'
   AND a.registro_id = f.id
   AND f.emitido_por_usuario_id IS NULL
   AND a.usuario_id IS NOT NULL;

-- Suscripcion.creado_por_usuario_id
UPDATE suscripcion s
   SET creado_por_usuario_id = a.usuario_id
  FROM audit_log a
 WHERE a.tabla = 'suscripcion'
   AND a.accion = 'CREATE'
   AND a.registro_id = s.id
   AND s.creado_por_usuario_id IS NULL
   AND a.usuario_id IS NOT NULL;

-- Convenio.creado_por_usuario_id
UPDATE convenio c
   SET creado_por_usuario_id = a.usuario_id
  FROM audit_log a
 WHERE a.tabla = 'convenio'
   AND a.accion = 'CREATE'
   AND a.registro_id = c.id
   AND c.creado_por_usuario_id IS NULL
   AND a.usuario_id IS NOT NULL;

-- ValidacionCompra.registrado_por_usuario_id
UPDATE validacion_compra v
   SET registrado_por_usuario_id = a.usuario_id
  FROM audit_log a
 WHERE a.tabla = 'validacion_compra'
   AND a.accion = 'CREATE'
   AND a.registro_id = v.id
   AND v.registrado_por_usuario_id IS NULL
   AND a.usuario_id IS NOT NULL;

-- ════════════════════════════════════════════════════════════════
-- v48.5: CHECK constraints para estados + cleanup de bugs
-- Asegura que los strings de estado en BD solo tomen valores válidos.
-- Idempotente: se borran y recrean (NO drop si no existe).
-- ════════════════════════════════════════════════════════════════

-- Cleanup: arreglar filas con estado NULL o vacío en reserva (bug detectado)
UPDATE reserva SET estado = 'PENDIENTE'
 WHERE estado IS NULL OR TRIM(estado) = '';

-- Cleanup: cualquier fila con estado en blanco en pago/factura/ticket/caja
UPDATE pago     SET estado = 'COMPLETADO' WHERE estado IS NULL OR TRIM(estado) = '';
UPDATE factura  SET estado = 'PENDIENTE'  WHERE estado IS NULL OR TRIM(estado) = '';
UPDATE ticket   SET estado = 'EN_CURSO'   WHERE estado IS NULL OR TRIM(estado) = '';
UPDATE caja     SET estado = 'CERRADA'    WHERE estado IS NULL OR TRIM(estado) = '';

-- CHECK constraints (idempotente via DROP IF EXISTS + ADD).
-- Nota: NO usar DO $$ ... $$ aqui porque Spring ScriptUtils rompe el dollar
-- quoting (parte el SQL en cada ';' y los ';' internos del DO descuadran).
ALTER TABLE ticket DROP CONSTRAINT IF EXISTS ck_ticket_estado;
ALTER TABLE ticket ADD CONSTRAINT ck_ticket_estado CHECK (estado IN ('EN_CURSO','CERRADO','ANULADO'));

ALTER TABLE factura DROP CONSTRAINT IF EXISTS ck_factura_estado;
ALTER TABLE factura ADD CONSTRAINT ck_factura_estado CHECK (estado IN ('PENDIENTE','PAGADA','ANULADA','VENCIDA'));

ALTER TABLE pago DROP CONSTRAINT IF EXISTS ck_pago_estado;
ALTER TABLE pago ADD CONSTRAINT ck_pago_estado CHECK (estado IN ('PENDIENTE','COMPLETADO','FALLIDO','ANULADO'));

ALTER TABLE caja DROP CONSTRAINT IF EXISTS ck_caja_estado;
ALTER TABLE caja ADD CONSTRAINT ck_caja_estado CHECK (estado IN ('ABIERTA','CERRADA'));

ALTER TABLE reserva DROP CONSTRAINT IF EXISTS ck_reserva_estado;
ALTER TABLE reserva ADD CONSTRAINT ck_reserva_estado CHECK (estado IN ('PENDIENTE','CONFIRMADA','CANCELADA','EXPIRADA','COMPLETADA'));

ALTER TABLE convenio DROP CONSTRAINT IF EXISTS ck_convenio_tipo_descuento;
ALTER TABLE convenio ADD CONSTRAINT ck_convenio_tipo_descuento CHECK (tipo_descuento IN ('MONTO_FIJO','PORCENTAJE','MINUTOS_GRATIS'));

ALTER TABLE resolucion_dian DROP CONSTRAINT IF EXISTS ck_resol_tipo_resolucion;
ALTER TABLE resolucion_dian ADD CONSTRAINT ck_resol_tipo_resolucion CHECK (tipo_resolucion IS NULL OR tipo_resolucion IN ('POS','FACTURA_ELECTRONICA','CONTINGENCIA'));

ALTER TABLE resolucion_dian DROP CONSTRAINT IF EXISTS ck_resol_modalidad;
ALTER TABLE resolucion_dian ADD CONSTRAINT ck_resol_modalidad CHECK (modalidad IS NULL OR modalidad IN ('POS_VENTA','FACTURACION_ELECTRONICA'));

ALTER TABLE resolucion_dian DROP CONSTRAINT IF EXISTS ck_resol_rango;
ALTER TABLE resolucion_dian ADD CONSTRAINT ck_resol_rango CHECK (rango_final >= rango_inicial);

ALTER TABLE resolucion_dian DROP CONSTRAINT IF EXISTS ck_resol_vigencia;
ALTER TABLE resolucion_dian ADD CONSTRAINT ck_resol_vigencia CHECK (vigente_hasta >= vigente_desde);

ALTER TABLE pago DROP CONSTRAINT IF EXISTS ck_pago_monto_positivo;
ALTER TABLE pago ADD CONSTRAINT ck_pago_monto_positivo CHECK (monto > 0);

ALTER TABLE factura DROP CONSTRAINT IF EXISTS ck_factura_valor_total_nonneg;
ALTER TABLE factura ADD CONSTRAINT ck_factura_valor_total_nonneg CHECK (valor_total >= 0);

ALTER TABLE tarifa DROP CONSTRAINT IF EXISTS ck_tarifa_valor_nonneg;
ALTER TABLE tarifa ADD CONSTRAINT ck_tarifa_valor_nonneg CHECK (valor >= 0);

-- ════════════════════════════════════════════════════════════════
-- v49 Sprint A: Snapshots de historicidad en ticket/factura/pago
-- Preservan los valores legibles en el momento del evento para que
-- los reportes antiguos NO muestren al dueño/operador/placa actual
-- cuando alguno cambia con el tiempo (vehiculo cambia de persona,
-- operador renombrado, tarifa renombrada, etc).
--
-- Todas las columnas son NULLABLE para retrocompatibilidad: registros
-- pre-v49 quedan con snapshot=NULL y los DTOs caen al FK actual como
-- fallback. Registros post-v49 siempre tendran snapshot poblado.
-- ════════════════════════════════════════════════════════════════

-- ticket: snapshots del momento de la entrada
ALTER TABLE ticket ADD COLUMN IF NOT EXISTS placa_snapshot                    VARCHAR(20);
ALTER TABLE ticket ADD COLUMN IF NOT EXISTS dueno_nombre_snapshot             VARCHAR(200);
ALTER TABLE ticket ADD COLUMN IF NOT EXISTS dueno_documento_snapshot          VARCHAR(50);
ALTER TABLE ticket ADD COLUMN IF NOT EXISTS tipo_vehiculo_snapshot            VARCHAR(100);
ALTER TABLE ticket ADD COLUMN IF NOT EXISTS tarifa_nombre_snapshot            VARCHAR(100);
ALTER TABLE ticket ADD COLUMN IF NOT EXISTS punto_parqueo_nombre_snapshot     VARCHAR(100);
ALTER TABLE ticket ADD COLUMN IF NOT EXISTS operador_entrada_nombre_snapshot  VARCHAR(200);
ALTER TABLE ticket ADD COLUMN IF NOT EXISTS operador_salida_nombre_snapshot   VARCHAR(200);

-- factura: snapshots del cliente y operador emisor en el momento de la emision
ALTER TABLE factura ADD COLUMN IF NOT EXISTS cliente_nombre_snapshot      VARCHAR(200);
ALTER TABLE factura ADD COLUMN IF NOT EXISTS cliente_documento_snapshot   VARCHAR(50);
ALTER TABLE factura ADD COLUMN IF NOT EXISTS placa_snapshot               VARCHAR(20);
ALTER TABLE factura ADD COLUMN IF NOT EXISTS emitido_por_nombre_snapshot  VARCHAR(200);

-- pago: snapshot del operador que registro el pago
ALTER TABLE pago ADD COLUMN IF NOT EXISTS operador_nombre_snapshot VARCHAR(200);

-- Indice util para reportes que filtran por placa historica (no requiere unique)
CREATE INDEX IF NOT EXISTS idx_ticket_placa_snapshot   ON ticket(placa_snapshot);
CREATE INDEX IF NOT EXISTS idx_factura_placa_snapshot  ON factura(placa_snapshot);

-- ════════════════════════════════════════════════════════════════
-- v49 Fase 0: Auditoria temporal universal (BaseEntity)
-- Agrega fecha_creacion + fecha_actualizacion a todas las tablas de
-- negocio que no las tienen. DEFAULT CURRENT_TIMESTAMP para que los
-- registros existentes queden con un valor razonable (su fecha de
-- carga, no su fecha real — limitacion conocida del backfill).
--
-- Despues de esta migracion, Hibernate maneja los timestamps via
-- @PrePersist/@PreUpdate de BaseEntity.
-- ════════════════════════════════════════════════════════════════

-- Estructura
ALTER TABLE empresa             ADD COLUMN IF NOT EXISTS fecha_creacion      TIMESTAMP DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE empresa             ADD COLUMN IF NOT EXISTS fecha_actualizacion TIMESTAMP;
ALTER TABLE parqueadero         ADD COLUMN IF NOT EXISTS fecha_creacion      TIMESTAMP DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE parqueadero         ADD COLUMN IF NOT EXISTS fecha_actualizacion TIMESTAMP;
ALTER TABLE nivel               ADD COLUMN IF NOT EXISTS fecha_creacion      TIMESTAMP DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE nivel               ADD COLUMN IF NOT EXISTS fecha_actualizacion TIMESTAMP;
ALTER TABLE seccion             ADD COLUMN IF NOT EXISTS fecha_creacion      TIMESTAMP DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE seccion             ADD COLUMN IF NOT EXISTS fecha_actualizacion TIMESTAMP;
ALTER TABLE sub_seccion         ADD COLUMN IF NOT EXISTS fecha_creacion      TIMESTAMP DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE sub_seccion         ADD COLUMN IF NOT EXISTS fecha_actualizacion TIMESTAMP;
ALTER TABLE punto_parqueo       ADD COLUMN IF NOT EXISTS fecha_creacion      TIMESTAMP DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE punto_parqueo       ADD COLUMN IF NOT EXISTS fecha_actualizacion TIMESTAMP;
ALTER TABLE camara              ADD COLUMN IF NOT EXISTS fecha_creacion      TIMESTAMP DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE camara              ADD COLUMN IF NOT EXISTS fecha_actualizacion TIMESTAMP;
ALTER TABLE camino              ADD COLUMN IF NOT EXISTS fecha_creacion      TIMESTAMP DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE camino              ADD COLUMN IF NOT EXISTS fecha_actualizacion TIMESTAMP;

-- Identity
ALTER TABLE usuario             ADD COLUMN IF NOT EXISTS fecha_actualizacion TIMESTAMP;
ALTER TABLE persona             ADD COLUMN IF NOT EXISTS fecha_creacion      TIMESTAMP DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE persona             ADD COLUMN IF NOT EXISTS fecha_actualizacion TIMESTAMP;
ALTER TABLE usuario_rol         ADD COLUMN IF NOT EXISTS fecha_creacion      TIMESTAMP DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE usuario_rol         ADD COLUMN IF NOT EXISTS fecha_actualizacion TIMESTAMP;
ALTER TABLE usuario_parqueadero ADD COLUMN IF NOT EXISTS fecha_creacion      TIMESTAMP DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE usuario_parqueadero ADD COLUMN IF NOT EXISTS fecha_actualizacion TIMESTAMP;

-- Operativa
ALTER TABLE ticket              ADD COLUMN IF NOT EXISTS fecha_creacion      TIMESTAMP DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE ticket              ADD COLUMN IF NOT EXISTS fecha_actualizacion TIMESTAMP;
ALTER TABLE factura             ADD COLUMN IF NOT EXISTS fecha_creacion      TIMESTAMP DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE factura             ADD COLUMN IF NOT EXISTS fecha_actualizacion TIMESTAMP;
ALTER TABLE pago                ADD COLUMN IF NOT EXISTS fecha_creacion      TIMESTAMP DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE pago                ADD COLUMN IF NOT EXISTS fecha_actualizacion TIMESTAMP;
ALTER TABLE vehiculo            ADD COLUMN IF NOT EXISTS fecha_creacion      TIMESTAMP DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE vehiculo            ADD COLUMN IF NOT EXISTS fecha_actualizacion TIMESTAMP;
ALTER TABLE reserva             ADD COLUMN IF NOT EXISTS fecha_creacion      TIMESTAMP DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE reserva             ADD COLUMN IF NOT EXISTS fecha_actualizacion TIMESTAMP;

-- Suscripciones / saldos / caja
ALTER TABLE suscripcion         ADD COLUMN IF NOT EXISTS fecha_actualizacion TIMESTAMP;
ALTER TABLE movimiento_saldo    ADD COLUMN IF NOT EXISTS fecha_creacion      TIMESTAMP DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE movimiento_saldo    ADD COLUMN IF NOT EXISTS fecha_actualizacion TIMESTAMP;
ALTER TABLE caja                ADD COLUMN IF NOT EXISTS fecha_creacion      TIMESTAMP DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE caja                ADD COLUMN IF NOT EXISTS fecha_actualizacion TIMESTAMP;
ALTER TABLE movimiento_caja     ADD COLUMN IF NOT EXISTS fecha_creacion      TIMESTAMP DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE movimiento_caja     ADD COLUMN IF NOT EXISTS fecha_actualizacion TIMESTAMP;

-- Tarifa / convenios / facturacion fiscal
ALTER TABLE tarifa              ADD COLUMN IF NOT EXISTS fecha_creacion      TIMESTAMP DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE tarifa              ADD COLUMN IF NOT EXISTS fecha_actualizacion TIMESTAMP;
ALTER TABLE tarifa_franja       ADD COLUMN IF NOT EXISTS fecha_creacion      TIMESTAMP DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE tarifa_franja       ADD COLUMN IF NOT EXISTS fecha_actualizacion TIMESTAMP;
ALTER TABLE convenio            ADD COLUMN IF NOT EXISTS fecha_actualizacion TIMESTAMP;
ALTER TABLE validacion_compra   ADD COLUMN IF NOT EXISTS fecha_creacion      TIMESTAMP DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE validacion_compra   ADD COLUMN IF NOT EXISTS fecha_actualizacion TIMESTAMP;
ALTER TABLE resolucion_dian     ADD COLUMN IF NOT EXISTS fecha_actualizacion TIMESTAMP;

-- Reportes / auditoria
ALTER TABLE cierre_dia          ADD COLUMN IF NOT EXISTS fecha_creacion      TIMESTAMP DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE cierre_dia          ADD COLUMN IF NOT EXISTS fecha_actualizacion TIMESTAMP;

-- Geo
ALTER TABLE pais                ADD COLUMN IF NOT EXISTS fecha_creacion      TIMESTAMP DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE pais                ADD COLUMN IF NOT EXISTS fecha_actualizacion TIMESTAMP;
ALTER TABLE departamento        ADD COLUMN IF NOT EXISTS fecha_creacion      TIMESTAMP DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE departamento        ADD COLUMN IF NOT EXISTS fecha_actualizacion TIMESTAMP;
ALTER TABLE ciudad              ADD COLUMN IF NOT EXISTS fecha_creacion      TIMESTAMP DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE ciudad              ADD COLUMN IF NOT EXISTS fecha_actualizacion TIMESTAMP;

-- Catalogos
ALTER TABLE estado              ADD COLUMN IF NOT EXISTS fecha_creacion      TIMESTAMP DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE estado              ADD COLUMN IF NOT EXISTS fecha_actualizacion TIMESTAMP;
ALTER TABLE rol                 ADD COLUMN IF NOT EXISTS fecha_creacion      TIMESTAMP DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE rol                 ADD COLUMN IF NOT EXISTS fecha_actualizacion TIMESTAMP;
ALTER TABLE tipo_vehiculo       ADD COLUMN IF NOT EXISTS fecha_creacion      TIMESTAMP DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE tipo_vehiculo       ADD COLUMN IF NOT EXISTS fecha_actualizacion TIMESTAMP;
ALTER TABLE tipo_parqueadero    ADD COLUMN IF NOT EXISTS fecha_creacion      TIMESTAMP DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE tipo_parqueadero    ADD COLUMN IF NOT EXISTS fecha_actualizacion TIMESTAMP;
ALTER TABLE tipo_punto_parqueo  ADD COLUMN IF NOT EXISTS fecha_creacion      TIMESTAMP DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE tipo_punto_parqueo  ADD COLUMN IF NOT EXISTS fecha_actualizacion TIMESTAMP;
ALTER TABLE tipo_dispositivo    ADD COLUMN IF NOT EXISTS fecha_creacion      TIMESTAMP DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE tipo_dispositivo    ADD COLUMN IF NOT EXISTS fecha_actualizacion TIMESTAMP;

-- Dispositivos IoT
ALTER TABLE dispositivo         ADD COLUMN IF NOT EXISTS fecha_creacion      TIMESTAMP DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE dispositivo         ADD COLUMN IF NOT EXISTS fecha_actualizacion TIMESTAMP;
ALTER TABLE dispositivo_parqueo ADD COLUMN IF NOT EXISTS fecha_creacion      TIMESTAMP DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE dispositivo_parqueo ADD COLUMN IF NOT EXISTS fecha_actualizacion TIMESTAMP;

-- ════════════════════════════════════════════════════════════════
-- v49 Fase 6: Enriquecimiento de catalogos legacy
-- Agrega codigo, color_hex, icono, orden_display, activo a los 6
-- catalogos basicos para que el front pueda mostrar UI rica
-- (chips de colores, iconos, ordenamiento estable) sin hardcodear.
-- ════════════════════════════════════════════════════════════════

ALTER TABLE estado              ADD COLUMN IF NOT EXISTS codigo         VARCHAR(50);
ALTER TABLE estado              ADD COLUMN IF NOT EXISTS color_hex      VARCHAR(9);
ALTER TABLE estado              ADD COLUMN IF NOT EXISTS icono          VARCHAR(50);
ALTER TABLE estado              ADD COLUMN IF NOT EXISTS orden_display  INTEGER;
ALTER TABLE estado              ADD COLUMN IF NOT EXISTS activo         BOOLEAN DEFAULT TRUE NOT NULL;

ALTER TABLE rol                 ADD COLUMN IF NOT EXISTS codigo         VARCHAR(50);
ALTER TABLE rol                 ADD COLUMN IF NOT EXISTS color_hex      VARCHAR(9);
ALTER TABLE rol                 ADD COLUMN IF NOT EXISTS icono          VARCHAR(50);
ALTER TABLE rol                 ADD COLUMN IF NOT EXISTS orden_display  INTEGER;
ALTER TABLE rol                 ADD COLUMN IF NOT EXISTS activo         BOOLEAN DEFAULT TRUE NOT NULL;

ALTER TABLE tipo_vehiculo       ADD COLUMN IF NOT EXISTS codigo         VARCHAR(50);
ALTER TABLE tipo_vehiculo       ADD COLUMN IF NOT EXISTS color_hex      VARCHAR(9);
ALTER TABLE tipo_vehiculo       ADD COLUMN IF NOT EXISTS icono          VARCHAR(50);
ALTER TABLE tipo_vehiculo       ADD COLUMN IF NOT EXISTS orden_display  INTEGER;
ALTER TABLE tipo_vehiculo       ADD COLUMN IF NOT EXISTS activo         BOOLEAN DEFAULT TRUE NOT NULL;

ALTER TABLE tipo_parqueadero    ADD COLUMN IF NOT EXISTS codigo         VARCHAR(50);
ALTER TABLE tipo_parqueadero    ADD COLUMN IF NOT EXISTS color_hex      VARCHAR(9);
ALTER TABLE tipo_parqueadero    ADD COLUMN IF NOT EXISTS icono          VARCHAR(50);
ALTER TABLE tipo_parqueadero    ADD COLUMN IF NOT EXISTS orden_display  INTEGER;
ALTER TABLE tipo_parqueadero    ADD COLUMN IF NOT EXISTS activo         BOOLEAN DEFAULT TRUE NOT NULL;

ALTER TABLE tipo_punto_parqueo  ADD COLUMN IF NOT EXISTS codigo         VARCHAR(50);
ALTER TABLE tipo_punto_parqueo  ADD COLUMN IF NOT EXISTS color_hex      VARCHAR(9);
ALTER TABLE tipo_punto_parqueo  ADD COLUMN IF NOT EXISTS icono          VARCHAR(50);
ALTER TABLE tipo_punto_parqueo  ADD COLUMN IF NOT EXISTS orden_display  INTEGER;
ALTER TABLE tipo_punto_parqueo  ADD COLUMN IF NOT EXISTS activo         BOOLEAN DEFAULT TRUE NOT NULL;

ALTER TABLE tipo_dispositivo    ADD COLUMN IF NOT EXISTS codigo         VARCHAR(50);
ALTER TABLE tipo_dispositivo    ADD COLUMN IF NOT EXISTS color_hex      VARCHAR(9);
ALTER TABLE tipo_dispositivo    ADD COLUMN IF NOT EXISTS icono          VARCHAR(50);
ALTER TABLE tipo_dispositivo    ADD COLUMN IF NOT EXISTS orden_display  INTEGER;
ALTER TABLE tipo_dispositivo    ADD COLUMN IF NOT EXISTS activo         BOOLEAN DEFAULT TRUE NOT NULL;

-- Seed: backfill de codigos y colores razonables para los datos existentes
UPDATE estado SET codigo = 'ACTIVO',     color_hex = '#10b981', icono = 'check-circle',  orden_display = 1, activo = true WHERE id = 1 AND codigo IS NULL;
UPDATE estado SET codigo = 'INACTIVO',   color_hex = '#6b7280', icono = 'circle-off',    orden_display = 2, activo = true WHERE id = 2 AND codigo IS NULL;
UPDATE estado SET codigo = 'ARCHIVADO',  color_hex = '#94a3b8', icono = 'archive',       orden_display = 3, activo = true WHERE id = 3 AND codigo IS NULL;

UPDATE rol SET codigo = 'USER',               color_hex = '#3b82f6', icono = 'user',         orden_display = 1, activo = true WHERE id = 1 AND codigo IS NULL;
UPDATE rol SET codigo = 'ADMIN',              color_hex = '#f59e0b', icono = 'shield',       orden_display = 2, activo = true WHERE id = 2 AND codigo IS NULL;
UPDATE rol SET codigo = 'SUPER_ADMIN',        color_hex = '#dc2626', icono = 'shield-check', orden_display = 3, activo = true WHERE id = 3 AND codigo IS NULL;
UPDATE rol SET codigo = 'ADMIN_PARQUEADERO',  color_hex = '#7c3aed', icono = 'building',     orden_display = 4, activo = true WHERE id = 4 AND codigo IS NULL;
UPDATE rol SET codigo = 'OPERARIO_CAJA',      color_hex = '#0ea5e9', icono = 'cash-register',orden_display = 5, activo = true WHERE id = 5 AND codigo IS NULL;

UPDATE tipo_vehiculo SET codigo = 'CARRO',     color_hex = '#3b82f6', icono = 'car',         orden_display = 1, activo = true WHERE id = 1 AND codigo IS NULL;
UPDATE tipo_vehiculo SET codigo = 'MOTO',      color_hex = '#ef4444', icono = 'motorcycle',  orden_display = 2, activo = true WHERE id = 2 AND codigo IS NULL;
UPDATE tipo_vehiculo SET codigo = 'CAMIONETA', color_hex = '#f59e0b', icono = 'truck',       orden_display = 3, activo = true WHERE id = 3 AND codigo IS NULL;

UPDATE tipo_parqueadero SET codigo = 'PUBLICO',  color_hex = '#10b981', icono = 'globe',     orden_display = 1, activo = true WHERE id = 1 AND codigo IS NULL;
UPDATE tipo_parqueadero SET codigo = 'PRIVADO',  color_hex = '#6366f1', icono = 'lock',      orden_display = 2, activo = true WHERE id = 2 AND codigo IS NULL;
UPDATE tipo_parqueadero SET codigo = 'EMPRESA',  color_hex = '#f59e0b', icono = 'briefcase', orden_display = 3, activo = true WHERE id = 3 AND codigo IS NULL;

UPDATE tipo_punto_parqueo SET codigo = 'NORMAL',         color_hex = '#3b82f6', icono = 'square',         orden_display = 1, activo = true WHERE id = 1 AND codigo IS NULL;
UPDATE tipo_punto_parqueo SET codigo = 'DISCAPACITADOS', color_hex = '#0ea5e9', icono = 'accessibility',  orden_display = 2, activo = true WHERE id = 2 AND codigo IS NULL;
UPDATE tipo_punto_parqueo SET codigo = 'VIP',            color_hex = '#fbbf24', icono = 'star',           orden_display = 3, activo = true WHERE id = 3 AND codigo IS NULL;

UPDATE tipo_dispositivo SET codigo = 'CAMARA',  color_hex = '#3b82f6', icono = 'video',       orden_display = 1, activo = true WHERE id = 1 AND codigo IS NULL;
UPDATE tipo_dispositivo SET codigo = 'SENSOR',  color_hex = '#10b981', icono = 'radio',       orden_display = 2, activo = true WHERE id = 2 AND codigo IS NULL;
UPDATE tipo_dispositivo SET codigo = 'BARRERA', color_hex = '#ef4444', icono = 'gate',        orden_display = 3, activo = true WHERE id = 3 AND codigo IS NULL;
