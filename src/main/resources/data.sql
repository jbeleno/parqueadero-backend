-- ================================================================
-- Spring Boot data.sql — Seed de datos iniciales
-- Se ejecuta DESPUÉS de que Hibernate crea/actualiza las tablas
-- (spring.sql.init.mode=always + defer-datasource-initialization=true)
-- Idempotente gracias a ON CONFLICT DO NOTHING
-- ================================================================

-- ════════════════════════════════════════════════════════════════
-- v49 PRE-FIX (CRITICO, NO TOCAR): Hibernate (ddl-auto=update) puede
-- crear columnas Boolean nuevas con NOT NULL pero SIN DEFAULT al
-- detectar `private Boolean campo = true;` en una entity. Los INSERTs
-- del seed (que no incluyen esas columnas) fallan con "null value in
-- column ... violates not-null constraint".
--
-- Este bloque va ANTES del primer INSERT. Cubre las columnas Boolean
-- con default Java en entities legacy. Las v49 nuevas se cubren
-- aparte mas abajo. Idempotente.
-- ════════════════════════════════════════════════════════════════

-- Catalogos legacy (Fase 6 agrego 'activo')
ALTER TABLE estado              ALTER COLUMN activo SET DEFAULT TRUE;
UPDATE estado              SET activo = TRUE WHERE activo IS NULL;
ALTER TABLE rol                 ALTER COLUMN activo SET DEFAULT TRUE;
UPDATE rol                 SET activo = TRUE WHERE activo IS NULL;
ALTER TABLE tipo_vehiculo       ALTER COLUMN activo SET DEFAULT TRUE;
UPDATE tipo_vehiculo       SET activo = TRUE WHERE activo IS NULL;
ALTER TABLE tipo_parqueadero    ALTER COLUMN activo SET DEFAULT TRUE;
UPDATE tipo_parqueadero    SET activo = TRUE WHERE activo IS NULL;
ALTER TABLE tipo_punto_parqueo  ALTER COLUMN activo SET DEFAULT TRUE;
UPDATE tipo_punto_parqueo  SET activo = TRUE WHERE activo IS NULL;
ALTER TABLE tipo_dispositivo    ALTER COLUMN activo SET DEFAULT TRUE;
UPDATE tipo_dispositivo    SET activo = TRUE WHERE activo IS NULL;

-- Entities legacy con campos Boolean pre-existentes
ALTER TABLE vehiculo            ALTER COLUMN activo       SET DEFAULT TRUE;
UPDATE vehiculo            SET activo = TRUE WHERE activo IS NULL;
ALTER TABLE vehiculo            ALTER COLUMN es_visitante SET DEFAULT FALSE;
UPDATE vehiculo            SET es_visitante = FALSE WHERE es_visitante IS NULL;
ALTER TABLE usuario_parqueadero ALTER COLUMN activo       SET DEFAULT TRUE;
UPDATE usuario_parqueadero SET activo = TRUE WHERE activo IS NULL;
ALTER TABLE refresh_token       ALTER COLUMN revocado     SET DEFAULT FALSE;
UPDATE refresh_token       SET revocado = FALSE WHERE revocado IS NULL;
ALTER TABLE tarifa              ALTER COLUMN activo                  SET DEFAULT TRUE;
UPDATE tarifa              SET activo = TRUE WHERE activo IS NULL;
ALTER TABLE tarifa              ALTER COLUMN valor_minimo_reemplaza  SET DEFAULT FALSE;
UPDATE tarifa              SET valor_minimo_reemplaza = FALSE WHERE valor_minimo_reemplaza IS NULL;
ALTER TABLE tarifa              ALTER COLUMN aplica_iva              SET DEFAULT FALSE;
UPDATE tarifa              SET aplica_iva = FALSE WHERE aplica_iva IS NULL;
ALTER TABLE tarifa_franja       ALTER COLUMN activa                  SET DEFAULT TRUE;
UPDATE tarifa_franja       SET activa = TRUE WHERE activa IS NULL;
ALTER TABLE tarifa_franja       ALTER COLUMN solo_fines_de_semana    SET DEFAULT FALSE;
UPDATE tarifa_franja       SET solo_fines_de_semana = FALSE WHERE solo_fines_de_semana IS NULL;

-- Columnas que Hibernate puede no crear porque la entity Java no las
-- declara pero el INSERT del seed sí las usa (descripcion en catalogos
-- globales). Las agregamos defensivamente con ADD COLUMN IF NOT EXISTS.
ALTER TABLE tipo_documento         ADD COLUMN IF NOT EXISTS descripcion TEXT;
ALTER TABLE genero                 ADD COLUMN IF NOT EXISTS descripcion TEXT;
ALTER TABLE moneda                 ADD COLUMN IF NOT EXISTS descripcion TEXT;
ALTER TABLE zona_horaria           ADD COLUMN IF NOT EXISTS descripcion TEXT;
ALTER TABLE unidad_tarifa          ADD COLUMN IF NOT EXISTS descripcion TEXT;
ALTER TABLE regimen_tributario     ADD COLUMN IF NOT EXISTS descripcion TEXT;
ALTER TABLE estado_civil           ADD COLUMN IF NOT EXISTS descripcion TEXT;
ALTER TABLE pais_codigo_placa      ADD COLUMN IF NOT EXISTS regex_placa VARCHAR(200);
ALTER TABLE tipo_servicio_vehiculo ADD COLUMN IF NOT EXISTS descripcion TEXT;
ALTER TABLE tipo_acceso_dispositivo ADD COLUMN IF NOT EXISTS descripcion TEXT;
ALTER TABLE tipo_acceso_dispositivo ADD COLUMN IF NOT EXISTS icono       VARCHAR(50);
ALTER TABLE canal_origen_reserva   ADD COLUMN IF NOT EXISTS descripcion TEXT;
ALTER TABLE canal_origen_reserva   ADD COLUMN IF NOT EXISTS icono       VARCHAR(50);

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

-- ════════════════════════════════════════════════════════════════
-- v49 Fase 9: Auditoria enriquecida — catalogos accion + nivel
-- En lugar de tener accion/nivel como string libre en audit_log,
-- introducimos 2 catalogos referenciables (con FK) para que:
--   1. SUPER_ADMIN pueda definir nuevas acciones sin recompilar
--   2. Reportes/filtros usen IDs estables (no strings que pueden
--      cambiar de mayusculas/idioma)
--   3. Cada accion tenga su nivel default (INFO/WARN/CRITICAL)
-- Las columnas string actuales (accion, nivel) quedan para
-- compatibilidad — los nuevos registros pueden llenar AMBAS o solo
-- el FK. v50 las puede deprecar.
-- ════════════════════════════════════════════════════════════════

CREATE TABLE IF NOT EXISTS accion_auditable (
    id              BIGSERIAL PRIMARY KEY,
    codigo          VARCHAR(50)  NOT NULL UNIQUE,
    nombre          VARCHAR(100) NOT NULL,
    descripcion     TEXT,
    color_hex       VARCHAR(9),
    icono           VARCHAR(50),
    orden_display   INTEGER,
    activo          BOOLEAN      NOT NULL DEFAULT TRUE,
    fecha_creacion  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_actualizacion TIMESTAMP
);

CREATE TABLE IF NOT EXISTS nivel_audit_log (
    id              BIGSERIAL PRIMARY KEY,
    codigo          VARCHAR(50)  NOT NULL UNIQUE,
    nombre          VARCHAR(100) NOT NULL,
    descripcion     TEXT,
    color_hex       VARCHAR(9),
    severidad       INTEGER      NOT NULL DEFAULT 0,
    activo          BOOLEAN      NOT NULL DEFAULT TRUE,
    fecha_creacion  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_actualizacion TIMESTAMP
);

-- v49 FIX EXHAUSTIVO: garantiza DEFAULTS en TODAS las columnas NOT NULL
-- de tablas v49. Necesario porque Hibernate (ddl-auto=update) puede haber
-- creado estas tablas SIN replicar los DEFAULTs SQL, y los INSERTs del
-- seed no especifican todos los campos. Idempotente.
-- Cubre: fecha_creacion (12), activo/activa (16), editable (1), es_final (5),
-- es_ingreso (2), max_filas (1), formato_default (1), tipo (1).

-- fecha_creacion (todas las tablas v49)
ALTER TABLE accion_auditable          ALTER COLUMN fecha_creacion  SET DEFAULT CURRENT_TIMESTAMP;
UPDATE accion_auditable          SET fecha_creacion = CURRENT_TIMESTAMP WHERE fecha_creacion IS NULL;
ALTER TABLE nivel_audit_log           ALTER COLUMN fecha_creacion  SET DEFAULT CURRENT_TIMESTAMP;
UPDATE nivel_audit_log           SET fecha_creacion = CURRENT_TIMESTAMP WHERE fecha_creacion IS NULL;
ALTER TABLE tipo_documento            ALTER COLUMN fecha_creacion  SET DEFAULT CURRENT_TIMESTAMP;
UPDATE tipo_documento            SET fecha_creacion = CURRENT_TIMESTAMP WHERE fecha_creacion IS NULL;
ALTER TABLE genero                    ALTER COLUMN fecha_creacion  SET DEFAULT CURRENT_TIMESTAMP;
UPDATE genero                    SET fecha_creacion = CURRENT_TIMESTAMP WHERE fecha_creacion IS NULL;
ALTER TABLE moneda                    ALTER COLUMN fecha_creacion  SET DEFAULT CURRENT_TIMESTAMP;
UPDATE moneda                    SET fecha_creacion = CURRENT_TIMESTAMP WHERE fecha_creacion IS NULL;
ALTER TABLE zona_horaria              ALTER COLUMN fecha_creacion  SET DEFAULT CURRENT_TIMESTAMP;
UPDATE zona_horaria              SET fecha_creacion = CURRENT_TIMESTAMP WHERE fecha_creacion IS NULL;
ALTER TABLE unidad_tarifa             ALTER COLUMN fecha_creacion  SET DEFAULT CURRENT_TIMESTAMP;
UPDATE unidad_tarifa             SET fecha_creacion = CURRENT_TIMESTAMP WHERE fecha_creacion IS NULL;
ALTER TABLE regimen_tributario        ALTER COLUMN fecha_creacion  SET DEFAULT CURRENT_TIMESTAMP;
UPDATE regimen_tributario        SET fecha_creacion = CURRENT_TIMESTAMP WHERE fecha_creacion IS NULL;
ALTER TABLE empresa_metodo_pago       ALTER COLUMN fecha_creacion  SET DEFAULT CURRENT_TIMESTAMP;
UPDATE empresa_metodo_pago       SET fecha_creacion = CURRENT_TIMESTAMP WHERE fecha_creacion IS NULL;
ALTER TABLE estado_ticket             ALTER COLUMN fecha_creacion  SET DEFAULT CURRENT_TIMESTAMP;
UPDATE estado_ticket             SET fecha_creacion = CURRENT_TIMESTAMP WHERE fecha_creacion IS NULL;
ALTER TABLE estado_factura            ALTER COLUMN fecha_creacion  SET DEFAULT CURRENT_TIMESTAMP;
UPDATE estado_factura            SET fecha_creacion = CURRENT_TIMESTAMP WHERE fecha_creacion IS NULL;
ALTER TABLE estado_pago               ALTER COLUMN fecha_creacion  SET DEFAULT CURRENT_TIMESTAMP;
UPDATE estado_pago               SET fecha_creacion = CURRENT_TIMESTAMP WHERE fecha_creacion IS NULL;
ALTER TABLE estado_suscripcion        ALTER COLUMN fecha_creacion  SET DEFAULT CURRENT_TIMESTAMP;
UPDATE estado_suscripcion        SET fecha_creacion = CURRENT_TIMESTAMP WHERE fecha_creacion IS NULL;
ALTER TABLE estado_caja               ALTER COLUMN fecha_creacion  SET DEFAULT CURRENT_TIMESTAMP;
UPDATE estado_caja               SET fecha_creacion = CURRENT_TIMESTAMP WHERE fecha_creacion IS NULL;
ALTER TABLE tipo_movimiento_caja      ALTER COLUMN fecha_creacion  SET DEFAULT CURRENT_TIMESTAMP;
UPDATE tipo_movimiento_caja      SET fecha_creacion = CURRENT_TIMESTAMP WHERE fecha_creacion IS NULL;
ALTER TABLE tipo_movimiento_saldo     ALTER COLUMN fecha_creacion  SET DEFAULT CURRENT_TIMESTAMP;
UPDATE tipo_movimiento_saldo     SET fecha_creacion = CURRENT_TIMESTAMP WHERE fecha_creacion IS NULL;
ALTER TABLE tipo_descuento_convenio   ALTER COLUMN fecha_creacion  SET DEFAULT CURRENT_TIMESTAMP;
UPDATE tipo_descuento_convenio   SET fecha_creacion = CURRENT_TIMESTAMP WHERE fecha_creacion IS NULL;
ALTER TABLE origen_factura            ALTER COLUMN fecha_creacion  SET DEFAULT CURRENT_TIMESTAMP;
UPDATE origen_factura            SET fecha_creacion = CURRENT_TIMESTAMP WHERE fecha_creacion IS NULL;
ALTER TABLE empresa_config            ALTER COLUMN fecha_creacion  SET DEFAULT CURRENT_TIMESTAMP;
UPDATE empresa_config            SET fecha_creacion = CURRENT_TIMESTAMP WHERE fecha_creacion IS NULL;
ALTER TABLE empresa_validacion_campo  ALTER COLUMN fecha_creacion  SET DEFAULT CURRENT_TIMESTAMP;
UPDATE empresa_validacion_campo  SET fecha_creacion = CURRENT_TIMESTAMP WHERE fecha_creacion IS NULL;
ALTER TABLE reporte_definicion        ALTER COLUMN fecha_creacion  SET DEFAULT CURRENT_TIMESTAMP;
UPDATE reporte_definicion        SET fecha_creacion = CURRENT_TIMESTAMP WHERE fecha_creacion IS NULL;

-- activo (catalogos donde INSERT no lo especifica)
ALTER TABLE empresa_metodo_pago       ALTER COLUMN activo  SET DEFAULT TRUE;
UPDATE empresa_metodo_pago       SET activo = TRUE WHERE activo IS NULL;
ALTER TABLE estado_ticket             ALTER COLUMN activo  SET DEFAULT TRUE;
UPDATE estado_ticket             SET activo = TRUE WHERE activo IS NULL;
ALTER TABLE estado_factura            ALTER COLUMN activo  SET DEFAULT TRUE;
UPDATE estado_factura            SET activo = TRUE WHERE activo IS NULL;
ALTER TABLE estado_pago               ALTER COLUMN activo  SET DEFAULT TRUE;
UPDATE estado_pago               SET activo = TRUE WHERE activo IS NULL;
ALTER TABLE estado_suscripcion        ALTER COLUMN activo  SET DEFAULT TRUE;
UPDATE estado_suscripcion        SET activo = TRUE WHERE activo IS NULL;
ALTER TABLE estado_caja               ALTER COLUMN activo  SET DEFAULT TRUE;
UPDATE estado_caja               SET activo = TRUE WHERE activo IS NULL;
ALTER TABLE tipo_movimiento_caja      ALTER COLUMN activo  SET DEFAULT TRUE;
UPDATE tipo_movimiento_caja      SET activo = TRUE WHERE activo IS NULL;
ALTER TABLE tipo_movimiento_saldo     ALTER COLUMN activo  SET DEFAULT TRUE;
UPDATE tipo_movimiento_saldo     SET activo = TRUE WHERE activo IS NULL;
ALTER TABLE tipo_descuento_convenio   ALTER COLUMN activo  SET DEFAULT TRUE;
UPDATE tipo_descuento_convenio   SET activo = TRUE WHERE activo IS NULL;
ALTER TABLE origen_factura            ALTER COLUMN activo  SET DEFAULT TRUE;
UPDATE origen_factura            SET activo = TRUE WHERE activo IS NULL;
ALTER TABLE reporte_definicion        ALTER COLUMN activo  SET DEFAULT TRUE;
UPDATE reporte_definicion        SET activo = TRUE WHERE activo IS NULL;

-- empresa_config: editable
ALTER TABLE empresa_config            ALTER COLUMN editable  SET DEFAULT TRUE;
UPDATE empresa_config            SET editable = TRUE WHERE editable IS NULL;

-- empresa_validacion_campo: activa
ALTER TABLE empresa_validacion_campo  ALTER COLUMN activa  SET DEFAULT TRUE;
UPDATE empresa_validacion_campo  SET activa = TRUE WHERE activa IS NULL;

-- reporte_definicion: max_filas + formato_default
ALTER TABLE reporte_definicion        ALTER COLUMN max_filas        SET DEFAULT 5000;
UPDATE reporte_definicion        SET max_filas = 5000 WHERE max_filas IS NULL;
ALTER TABLE reporte_definicion        ALTER COLUMN formato_default  SET DEFAULT 'JSON';
UPDATE reporte_definicion        SET formato_default = 'JSON' WHERE formato_default IS NULL;

-- reporte_ejecutado: defaults (no hay seed pero protege contra inserts viejos)
ALTER TABLE reporte_ejecutado         ALTER COLUMN formato     SET DEFAULT 'JSON';
ALTER TABLE reporte_ejecutado         ALTER COLUMN estado      SET DEFAULT 'OK';
ALTER TABLE reporte_ejecutado         ALTER COLUMN fecha_hora  SET DEFAULT CURRENT_TIMESTAMP;

-- Seed: niveles
INSERT INTO nivel_audit_log (codigo, nombre, descripcion, color_hex, severidad, activo) VALUES
    ('INFO',     'Informativo', 'Operaciones normales del sistema',       '#3b82f6', 0, true),
    ('WARN',     'Advertencia', 'Situacion sospechosa pero no critica',    '#f59e0b', 1, true),
    ('CRITICAL', 'Critico',     'Requiere atencion inmediata',             '#dc2626', 2, true),
    ('DEBUG',    'Depuracion',  'Solo para investigacion tecnica',         '#6b7280',-1, true)
ON CONFLICT (codigo) DO NOTHING;

-- Seed: acciones auditables (las que el codigo ya usa)
INSERT INTO accion_auditable (codigo, nombre, descripcion, color_hex, icono, orden_display, activo) VALUES
    ('CREATE',          'Crear',           'Insertar un nuevo registro',          '#10b981', 'plus-circle',  1, true),
    ('UPDATE',          'Actualizar',      'Modificar un registro existente',     '#3b82f6', 'edit',         2, true),
    ('CERRAR',          'Cerrar',          'Cerrar un ticket o periodo',          '#0ea5e9', 'x-circle',     3, true),
    ('ANULAR',          'Anular',          'Anular un registro (con motivo)',     '#dc2626', 'ban',          4, true),
    ('CANCELAR',        'Cancelar',        'Cancelar una reserva u operacion',    '#ef4444', 'x',            5, true),
    ('DESACTIVAR',      'Desactivar',      'Marcar registro como inactivo',       '#94a3b8', 'circle-off',   6, true),
    ('ARCHIVAR',        'Archivar',        'Soft-delete logico',                  '#94a3b8', 'archive',      7, true),
    ('MARCAR_PRINCIPAL','Marcar principal','Designar como principal/predeterminado','#fbbf24','star',         8, true),
    ('DELETE_FISICO',   'Eliminar fisico', 'Eliminacion DDL (solo SUPER_ADMIN)',  '#7f1d1d', 'trash',        9, true),
    ('LOGIN',           'Inicio sesion',   'Login exitoso',                       '#10b981', 'log-in',      10, true),
    ('LOGOUT',          'Cierre sesion',   'Logout',                              '#6b7280', 'log-out',     11, true),
    ('EXPORT',          'Exportar',        'Exportacion de datos',                '#7c3aed', 'download',    12, true),
    ('CALCULO',         'Calculo',         'Recalculo de saldos/montos',          '#0ea5e9', 'calculator',  13, true),
    ('OCR_DETECCION',   'OCR deteccion',   'Deteccion automatica por camara',     '#8b5cf6', 'camera',      14, true)
ON CONFLICT (codigo) DO NOTHING;

-- Agregar FKs en audit_log (NULLABLE para no romper registros viejos)
ALTER TABLE audit_log ADD COLUMN IF NOT EXISTS accion_auditable_id BIGINT REFERENCES accion_auditable(id);
ALTER TABLE audit_log ADD COLUMN IF NOT EXISTS nivel_audit_log_id  BIGINT REFERENCES nivel_audit_log(id);
CREATE INDEX IF NOT EXISTS idx_audit_log_accion_id ON audit_log(accion_auditable_id);
CREATE INDEX IF NOT EXISTS idx_audit_log_nivel_id  ON audit_log(nivel_audit_log_id);

-- Backfill: enlazar registros existentes que tienen accion=codigo conocido
UPDATE audit_log al SET accion_auditable_id = a.id
  FROM accion_auditable a WHERE al.accion = a.codigo AND al.accion_auditable_id IS NULL;

-- ════════════════════════════════════════════════════════════════
-- v49 Fase 1: Catalogos globales (gestionados por SUPER_ADMIN)
-- Reemplazan VARCHAR libres por FKs a catalogos para parametrizacion.
-- Implementacion inicial: 6 catalogos prioritarios.
-- ════════════════════════════════════════════════════════════════

-- 1. tipo_documento: reemplaza VARCHAR libre en persona.tipo_documento
CREATE TABLE IF NOT EXISTS tipo_documento (
    id              BIGSERIAL PRIMARY KEY,
    codigo          VARCHAR(20)  NOT NULL UNIQUE,
    nombre          VARCHAR(100) NOT NULL,
    descripcion     TEXT,
    aplica_persona  BOOLEAN      NOT NULL DEFAULT TRUE,
    aplica_empresa  BOOLEAN      NOT NULL DEFAULT FALSE,
    orden_display   INTEGER,
    activo          BOOLEAN      NOT NULL DEFAULT TRUE,
    fecha_creacion  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_actualizacion TIMESTAMP
);
INSERT INTO tipo_documento (codigo, nombre, descripcion, aplica_persona, aplica_empresa, orden_display, activo) VALUES
    ('CC',           'Cedula de Ciudadania',    'Documento de identidad colombiano', true,  false, 1, true),
    ('CE',           'Cedula de Extranjeria',   'Extranjeros residentes',            true,  false, 2, true),
    ('TI',           'Tarjeta de Identidad',    'Menores 7-17 años',                 true,  false, 3, true),
    ('RC',           'Registro Civil',          'Menores de 7 años',                 true,  false, 4, true),
    ('PA',           'Pasaporte',               'Documento internacional',           true,  false, 5, true),
    ('NIT',          'NIT',                     'Numero de Identificacion Tributaria',false,true,  6, true),
    ('RUT',          'RUT',                     'Registro Unico Tributario',         false, true,  7, true),
    ('PASAPORTE',    'Pasaporte (legacy)',      'Alias historico de PA',             true,  false, 8, false),
    ('NUIP',         'NUIP',                    'Numero Unico Identificacion Personal',true,false,9, true),
    ('PEP',          'PEP',                     'Permiso Especial de Permanencia',   true,  false, 10,true),
    ('ID_EXTRANJERO','ID Extranjero',           'Identificacion emitida en exterior',true,  false, 11,true)
ON CONFLICT (codigo) DO NOTHING;

-- 2. genero
CREATE TABLE IF NOT EXISTS genero (
    id              BIGSERIAL PRIMARY KEY,
    codigo          VARCHAR(10)  NOT NULL UNIQUE,
    nombre          VARCHAR(50)  NOT NULL,
    orden_display   INTEGER,
    activo          BOOLEAN      NOT NULL DEFAULT TRUE,
    fecha_creacion  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_actualizacion TIMESTAMP
);
INSERT INTO genero (codigo, nombre, orden_display, activo) VALUES
    ('M',     'Masculino',     1, true),
    ('F',     'Femenino',      2, true),
    ('OTRO',  'Otro',          3, true),
    ('PNR',   'Prefiero no responder', 4, true)
ON CONFLICT (codigo) DO NOTHING;

-- 3. moneda
CREATE TABLE IF NOT EXISTS moneda (
    id              BIGSERIAL PRIMARY KEY,
    codigo          VARCHAR(3)   NOT NULL UNIQUE,
    nombre          VARCHAR(50)  NOT NULL,
    simbolo         VARCHAR(5)   NOT NULL,
    decimales       INTEGER      NOT NULL DEFAULT 2,
    orden_display   INTEGER,
    activo          BOOLEAN      NOT NULL DEFAULT TRUE,
    fecha_creacion  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_actualizacion TIMESTAMP
);
INSERT INTO moneda (codigo, nombre, simbolo, decimales, orden_display, activo) VALUES
    ('COP', 'Peso Colombiano',  '$',  0, 1, true),
    ('USD', 'Dolar Americano',  'US$',2, 2, true),
    ('EUR', 'Euro',             '€',  2, 3, true),
    ('MXN', 'Peso Mexicano',    'MX$',2, 4, true),
    ('ARS', 'Peso Argentino',   'AR$',2, 5, true)
ON CONFLICT (codigo) DO NOTHING;

-- 4. zona_horaria
CREATE TABLE IF NOT EXISTS zona_horaria (
    id              BIGSERIAL PRIMARY KEY,
    codigo          VARCHAR(50)  NOT NULL UNIQUE,
    nombre          VARCHAR(100) NOT NULL,
    offset_horas    INTEGER,
    orden_display   INTEGER,
    activo          BOOLEAN      NOT NULL DEFAULT TRUE,
    fecha_creacion  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_actualizacion TIMESTAMP
);
INSERT INTO zona_horaria (codigo, nombre, offset_horas, orden_display, activo) VALUES
    ('America/Bogota',      'Colombia (Bogota)',           -5, 1, true),
    ('America/Mexico_City', 'Mexico (Ciudad de Mexico)',   -6, 2, true),
    ('America/Lima',        'Peru (Lima)',                 -5, 3, true),
    ('America/Caracas',     'Venezuela (Caracas)',         -4, 4, true),
    ('America/Buenos_Aires','Argentina (Buenos Aires)',    -3, 5, true),
    ('America/Santiago',    'Chile (Santiago)',            -3, 6, true),
    ('America/New_York',    'EE.UU. Este',                 -5, 7, true)
ON CONFLICT (codigo) DO NOTHING;

-- 5. unidad_tarifa: reemplaza tarifa.unidad VARCHAR
CREATE TABLE IF NOT EXISTS unidad_tarifa (
    id              BIGSERIAL PRIMARY KEY,
    codigo          VARCHAR(20)  NOT NULL UNIQUE,
    nombre          VARCHAR(50)  NOT NULL,
    minutos         INTEGER,
    orden_display   INTEGER,
    activo          BOOLEAN      NOT NULL DEFAULT TRUE,
    fecha_creacion  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_actualizacion TIMESTAMP
);
INSERT INTO unidad_tarifa (codigo, nombre, minutos, orden_display, activo) VALUES
    ('MINUTO',    'Por minuto',           1,    1, true),
    ('FRACCION',  'Por fraccion',         NULL, 2, true),
    ('HORA',      'Por hora',             60,   3, true),
    ('DIA',       'Por dia (24h)',        1440, 4, true),
    ('PLANA',     'Tarifa plana (unica)', NULL, 5, true),
    ('POR_HORA',  'Por hora (alias)',     60,   6, true)
ON CONFLICT (codigo) DO NOTHING;

-- 6. regimen_tributario: reemplaza parqueadero.regimen_tributario VARCHAR
CREATE TABLE IF NOT EXISTS regimen_tributario (
    id              BIGSERIAL PRIMARY KEY,
    codigo          VARCHAR(50)  NOT NULL UNIQUE,
    nombre          VARCHAR(100) NOT NULL,
    descripcion     TEXT,
    pais_codigo     VARCHAR(2),
    orden_display   INTEGER,
    activo          BOOLEAN      NOT NULL DEFAULT TRUE,
    fecha_creacion  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_actualizacion TIMESTAMP
);
INSERT INTO regimen_tributario (codigo, nombre, descripcion, pais_codigo, orden_display, activo) VALUES
    ('SIMPLIFICADO',       'Simplificado',         'Antiguo regimen simplificado',         'CO', 1, true),
    ('COMUN',              'Comun',                'Antiguo regimen comun',                'CO', 2, true),
    ('RESPONSABLE_IVA',    'Responsable de IVA',   'Persona/empresa responsable del IVA',  'CO', 3, true),
    ('NO_RESPONSABLE',     'No responsable de IVA','Persona no responsable',               'CO', 4, true),
    ('GRAN_CONTRIBUYENTE', 'Gran Contribuyente',   'Designado por DIAN',                   'CO', 5, true)
ON CONFLICT (codigo) DO NOTHING;

-- ════════════════════════════════════════════════════════════════
-- v49 Fase 3: empresa_config key-value
-- Saca todos los hardcoded del codigo (cooldowns, dias de suscripcion,
-- formatos, regex, defaults) a la BD, editable por ADMIN de empresa.
-- ════════════════════════════════════════════════════════════════

CREATE TABLE IF NOT EXISTS empresa_config (
    id              BIGSERIAL PRIMARY KEY,
    empresa_id      BIGINT       NOT NULL REFERENCES empresa(id) ON DELETE CASCADE,
    clave           VARCHAR(150) NOT NULL,
    valor           TEXT,
    tipo            VARCHAR(20)  NOT NULL DEFAULT 'STRING',
    valor_min       NUMERIC,
    valor_max       NUMERIC,
    descripcion     TEXT,
    categoria       VARCHAR(50),
    editable        BOOLEAN      NOT NULL DEFAULT TRUE,
    fecha_creacion  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_actualizacion TIMESTAMP,
    actualizado_por_usuario_id BIGINT,
    CONSTRAINT uq_empresa_config_clave UNIQUE (empresa_id, clave),
    CONSTRAINT ck_empresa_config_tipo CHECK (tipo IN ('STRING','INTEGER','DECIMAL','BOOLEAN','REGEX'))
);
CREATE INDEX IF NOT EXISTS idx_empresa_config_empresa ON empresa_config(empresa_id);
CREATE INDEX IF NOT EXISTS idx_empresa_config_categoria ON empresa_config(categoria);

-- Seed: 26 configs default para CADA empresa existente
-- (loop implicito via INSERT SELECT)
INSERT INTO empresa_config (empresa_id, clave, valor, tipo, valor_min, valor_max, descripcion, categoria)
SELECT e.id, c.clave, c.valor, c.tipo, c.valor_min, c.valor_max, c.descripcion, c.categoria
FROM empresa e
CROSS JOIN (VALUES
    ('motivo.min_chars',                   '10',        'INTEGER', 5,    50,   'Largo minimo del motivo en anulaciones', 'motivos'),
    ('motivo.max_chars',                   '500',       'INTEGER', 100,  2000, 'Largo maximo del motivo', 'motivos'),
    ('placa.regex',                        '^[A-Z]{3}\d{3}$|^[A-Z]{3}\d{2}[A-Z]$', 'REGEX', NULL, NULL, 'Formato valido de placa', 'vehiculo'),
    ('placa.visitante_prefix',             'VIS-',      'STRING',  NULL, NULL, 'Prefijo para placas generadas a visitantes', 'vehiculo'),
    ('placa.requiere_uppercase',           'true',      'BOOLEAN', NULL, NULL, 'Normalizar placas a mayusculas al guardar', 'vehiculo'),
    ('suscripcion.mensual_dias',           '30',        'INTEGER', 1,    365,  'Duracion en dias de una suscripcion mensual', 'suscripciones'),
    ('suscripcion.pase_dia_horas',         '24',        'INTEGER', 1,    168,  'Duracion en horas del pase de dia', 'suscripciones'),
    ('suscripcion.abono_dias_vigencia',    '365',       'INTEGER', 30,   3650, 'Vigencia del saldo prepagado', 'suscripciones'),
    ('suscripcion.aviso_vencimiento_dias_antes', '3',   'INTEGER', 0,    30,   'Dias antes para avisar vencimiento', 'suscripciones'),
    ('ocr.cooldown_segundos',              '30',        'INTEGER', 5,    600,  'Segundos antes de procesar misma placa de nuevo', 'ocr'),
    ('ocr.min_voting_confidence',          '0.66',      'DECIMAL', 0,    1,    'Confianza minima de voting para aceptar lectura', 'ocr'),
    ('audit_log.default_page_size',        '50',        'INTEGER', 10,   200,  'Tamaño de pagina por defecto', 'auditoria'),
    ('audit_log.max_page_size',            '200',       'INTEGER', 50,   1000, 'Tamaño maximo de pagina', 'auditoria'),
    ('reportes.max_filas',                 '5000',      'INTEGER', 100,  100000,'Limite de filas por reporte', 'reportes'),
    ('formato.fecha',                      'dd/MM/yyyy','STRING',  NULL, NULL, 'Formato de fecha para presentacion', 'formato'),
    ('formato.fecha_hora',                 'dd/MM/yyyy HH:mm', 'STRING', NULL, NULL, 'Formato fecha+hora', 'formato'),
    ('formato.moneda_codigo',              'COP',       'STRING',  NULL, NULL, 'Codigo de moneda por defecto', 'formato'),
    ('formato.moneda_simbolo',             '$',         'STRING',  NULL, NULL, 'Simbolo de moneda', 'formato'),
    ('formato.decimal_separator',          ',',         'STRING',  NULL, NULL, 'Separador decimal', 'formato'),
    ('formato.thousand_separator',         '.',         'STRING',  NULL, NULL, 'Separador de miles', 'formato'),
    ('formato.zona_horaria',               'America/Bogota', 'STRING', NULL, NULL, 'Zona horaria por defecto', 'formato'),
    ('registro.email_obligatorio',         'false',     'BOOLEAN', NULL, NULL, 'Email obligatorio al registrar persona', 'registro'),
    ('registro.telefono_obligatorio',      'true',      'BOOLEAN', NULL, NULL, 'Telefono obligatorio', 'registro'),
    ('registro.fecha_nacimiento_obligatoria','false',   'BOOLEAN', NULL, NULL, 'Fecha de nacimiento obligatoria', 'registro'),
    ('factura.numerar_automatico',         'true',      'BOOLEAN', NULL, NULL, 'Numeracion automatica de facturas', 'facturacion'),
    ('ticket.tiempo_gracia_minutos_default','5',        'INTEGER', 0,    60,   'Minutos de gracia default al cerrar ticket', 'ticket')
) AS c(clave, valor, tipo, valor_min, valor_max, descripcion, categoria)
ON CONFLICT DO NOTHING;

-- ════════════════════════════════════════════════════════════════
-- v49 Fase 2: Catalogos por empresa (10 tablas)
-- Cada empresa gestiona SUS estados/tipos/metodos. Defaults seed por
-- empresa al ser creada (aqui los aplicamos via INSERT...CROSS JOIN
-- para empresas existentes).
-- Estructura uniforme: id, empresa_id, codigo, nombre, descripcion,
-- color_hex, icono, orden_display, activo, fecha_creacion,
-- fecha_actualizacion. UNIQUE(empresa_id, codigo).
-- ════════════════════════════════════════════════════════════════

-- 1. empresa_metodo_pago
CREATE TABLE IF NOT EXISTS empresa_metodo_pago (
    id BIGSERIAL PRIMARY KEY,
    empresa_id BIGINT NOT NULL REFERENCES empresa(id) ON DELETE CASCADE,
    codigo VARCHAR(50) NOT NULL,
    nombre VARCHAR(100) NOT NULL,
    descripcion TEXT,
    color_hex VARCHAR(9),
    icono VARCHAR(50),
    orden_display INTEGER,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    fecha_creacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_actualizacion TIMESTAMP,
    CONSTRAINT uq_emp_metodo_pago UNIQUE (empresa_id, codigo)
);

-- 2. estado_ticket
CREATE TABLE IF NOT EXISTS estado_ticket (
    id BIGSERIAL PRIMARY KEY,
    empresa_id BIGINT NOT NULL REFERENCES empresa(id) ON DELETE CASCADE,
    codigo VARCHAR(50) NOT NULL,
    nombre VARCHAR(100) NOT NULL,
    descripcion TEXT,
    color_hex VARCHAR(9),
    icono VARCHAR(50),
    orden_display INTEGER,
    es_final BOOLEAN NOT NULL DEFAULT FALSE,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    fecha_creacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_actualizacion TIMESTAMP,
    CONSTRAINT uq_estado_ticket UNIQUE (empresa_id, codigo)
);

-- 3. estado_factura
CREATE TABLE IF NOT EXISTS estado_factura (
    id BIGSERIAL PRIMARY KEY,
    empresa_id BIGINT NOT NULL REFERENCES empresa(id) ON DELETE CASCADE,
    codigo VARCHAR(50) NOT NULL,
    nombre VARCHAR(100) NOT NULL,
    descripcion TEXT,
    color_hex VARCHAR(9),
    icono VARCHAR(50),
    orden_display INTEGER,
    es_final BOOLEAN NOT NULL DEFAULT FALSE,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    fecha_creacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_actualizacion TIMESTAMP,
    CONSTRAINT uq_estado_factura UNIQUE (empresa_id, codigo)
);

-- 4. estado_pago
CREATE TABLE IF NOT EXISTS estado_pago (
    id BIGSERIAL PRIMARY KEY,
    empresa_id BIGINT NOT NULL REFERENCES empresa(id) ON DELETE CASCADE,
    codigo VARCHAR(50) NOT NULL,
    nombre VARCHAR(100) NOT NULL,
    descripcion TEXT,
    color_hex VARCHAR(9),
    icono VARCHAR(50),
    orden_display INTEGER,
    es_final BOOLEAN NOT NULL DEFAULT FALSE,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    fecha_creacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_actualizacion TIMESTAMP,
    CONSTRAINT uq_estado_pago UNIQUE (empresa_id, codigo)
);

-- 5. estado_suscripcion
CREATE TABLE IF NOT EXISTS estado_suscripcion (
    id BIGSERIAL PRIMARY KEY,
    empresa_id BIGINT NOT NULL REFERENCES empresa(id) ON DELETE CASCADE,
    codigo VARCHAR(50) NOT NULL,
    nombre VARCHAR(100) NOT NULL,
    descripcion TEXT,
    color_hex VARCHAR(9),
    icono VARCHAR(50),
    orden_display INTEGER,
    es_final BOOLEAN NOT NULL DEFAULT FALSE,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    fecha_creacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_actualizacion TIMESTAMP,
    CONSTRAINT uq_estado_suscripcion UNIQUE (empresa_id, codigo)
);

-- 6. estado_caja
CREATE TABLE IF NOT EXISTS estado_caja (
    id BIGSERIAL PRIMARY KEY,
    empresa_id BIGINT NOT NULL REFERENCES empresa(id) ON DELETE CASCADE,
    codigo VARCHAR(50) NOT NULL,
    nombre VARCHAR(100) NOT NULL,
    descripcion TEXT,
    color_hex VARCHAR(9),
    icono VARCHAR(50),
    orden_display INTEGER,
    es_final BOOLEAN NOT NULL DEFAULT FALSE,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    fecha_creacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_actualizacion TIMESTAMP,
    CONSTRAINT uq_estado_caja UNIQUE (empresa_id, codigo)
);

-- 7. tipo_movimiento_caja
CREATE TABLE IF NOT EXISTS tipo_movimiento_caja (
    id BIGSERIAL PRIMARY KEY,
    empresa_id BIGINT NOT NULL REFERENCES empresa(id) ON DELETE CASCADE,
    codigo VARCHAR(50) NOT NULL,
    nombre VARCHAR(100) NOT NULL,
    descripcion TEXT,
    color_hex VARCHAR(9),
    icono VARCHAR(50),
    orden_display INTEGER,
    es_ingreso BOOLEAN NOT NULL DEFAULT TRUE,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    fecha_creacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_actualizacion TIMESTAMP,
    CONSTRAINT uq_tipo_mov_caja UNIQUE (empresa_id, codigo)
);

-- 8. tipo_movimiento_saldo
CREATE TABLE IF NOT EXISTS tipo_movimiento_saldo (
    id BIGSERIAL PRIMARY KEY,
    empresa_id BIGINT NOT NULL REFERENCES empresa(id) ON DELETE CASCADE,
    codigo VARCHAR(50) NOT NULL,
    nombre VARCHAR(100) NOT NULL,
    descripcion TEXT,
    color_hex VARCHAR(9),
    icono VARCHAR(50),
    orden_display INTEGER,
    es_ingreso BOOLEAN NOT NULL DEFAULT TRUE,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    fecha_creacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_actualizacion TIMESTAMP,
    CONSTRAINT uq_tipo_mov_saldo UNIQUE (empresa_id, codigo)
);

-- 9. tipo_descuento_convenio
CREATE TABLE IF NOT EXISTS tipo_descuento_convenio (
    id BIGSERIAL PRIMARY KEY,
    empresa_id BIGINT NOT NULL REFERENCES empresa(id) ON DELETE CASCADE,
    codigo VARCHAR(50) NOT NULL,
    nombre VARCHAR(100) NOT NULL,
    descripcion TEXT,
    color_hex VARCHAR(9),
    icono VARCHAR(50),
    orden_display INTEGER,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    fecha_creacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_actualizacion TIMESTAMP,
    CONSTRAINT uq_tipo_desc_convenio UNIQUE (empresa_id, codigo)
);

-- 10. origen_factura
CREATE TABLE IF NOT EXISTS origen_factura (
    id BIGSERIAL PRIMARY KEY,
    empresa_id BIGINT NOT NULL REFERENCES empresa(id) ON DELETE CASCADE,
    codigo VARCHAR(50) NOT NULL,
    nombre VARCHAR(100) NOT NULL,
    descripcion TEXT,
    color_hex VARCHAR(9),
    icono VARCHAR(50),
    orden_display INTEGER,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    fecha_creacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_actualizacion TIMESTAMP,
    CONSTRAINT uq_origen_factura UNIQUE (empresa_id, codigo)
);

-- Seed: para cada empresa existente, sembrar los catalogos con defaults
INSERT INTO empresa_metodo_pago (empresa_id, codigo, nombre, color_hex, icono, orden_display)
SELECT e.id, c.codigo, c.nombre, c.color_hex, c.icono, c.orden_display FROM empresa e
CROSS JOIN (VALUES
    ('EFECTIVO',    'Efectivo',           '#10b981', 'banknote',     1),
    ('TARJETA',     'Tarjeta',            '#3b82f6', 'credit-card',  2),
    ('NEQUI',       'Nequi',              '#ec4899', 'smartphone',   3),
    ('DAVIPLATA',   'Daviplata',          '#dc2626', 'smartphone',   4),
    ('PSE',         'PSE',                '#0ea5e9', 'banknote',     5),
    ('TRANSFERENCIA','Transferencia',     '#6366f1', 'arrow-right',  6),
    ('CHEQUE',      'Cheque',             '#94a3b8', 'file-text',    7),
    ('SALDO_PREPAGO','Saldo prepago',     '#fbbf24', 'wallet',       8),
    ('APP',         'App movil',          '#8b5cf6', 'smartphone',   9),
    ('OTRO',        'Otro',               '#6b7280', 'help-circle', 10)
) AS c(codigo, nombre, color_hex, icono, orden_display)
ON CONFLICT (empresa_id, codigo) DO NOTHING;

INSERT INTO estado_ticket (empresa_id, codigo, nombre, color_hex, icono, orden_display, es_final)
SELECT e.id, c.codigo, c.nombre, c.color_hex, c.icono, c.orden_display, c.es_final FROM empresa e
CROSS JOIN (VALUES
    ('EN_CURSO',   'En curso',   '#3b82f6', 'play',         1, false),
    ('CERRADO',    'Cerrado',    '#10b981', 'check-circle', 2, true),
    ('ANULADO',    'Anulado',    '#dc2626', 'ban',          3, true),
    ('ABANDONADO', 'Abandonado', '#94a3b8', 'archive',      4, true)
) AS c(codigo, nombre, color_hex, icono, orden_display, es_final)
ON CONFLICT (empresa_id, codigo) DO NOTHING;

INSERT INTO estado_factura (empresa_id, codigo, nombre, color_hex, icono, orden_display, es_final)
SELECT e.id, c.codigo, c.nombre, c.color_hex, c.icono, c.orden_display, c.es_final FROM empresa e
CROSS JOIN (VALUES
    ('PENDIENTE',  'Pendiente',  '#f59e0b', 'clock',        1, false),
    ('PAGADA',     'Pagada',     '#10b981', 'check-circle', 2, true),
    ('ANULADA',    'Anulada',    '#dc2626', 'ban',          3, true),
    ('VENCIDA',    'Vencida',    '#7f1d1d', 'alert-circle', 4, false)
) AS c(codigo, nombre, color_hex, icono, orden_display, es_final)
ON CONFLICT (empresa_id, codigo) DO NOTHING;

INSERT INTO estado_pago (empresa_id, codigo, nombre, color_hex, icono, orden_display, es_final)
SELECT e.id, c.codigo, c.nombre, c.color_hex, c.icono, c.orden_display, c.es_final FROM empresa e
CROSS JOIN (VALUES
    ('PENDIENTE',  'Pendiente',  '#f59e0b', 'clock',        1, false),
    ('COMPLETADO', 'Completado', '#10b981', 'check-circle', 2, true),
    ('FALLIDO',    'Fallido',    '#dc2626', 'x-circle',     3, true),
    ('ANULADO',    'Anulado',    '#7f1d1d', 'ban',          4, true),
    ('REVERSADO',  'Reversado',  '#94a3b8', 'undo-2',       5, true)
) AS c(codigo, nombre, color_hex, icono, orden_display, es_final)
ON CONFLICT (empresa_id, codigo) DO NOTHING;

INSERT INTO estado_suscripcion (empresa_id, codigo, nombre, color_hex, icono, orden_display, es_final)
SELECT e.id, c.codigo, c.nombre, c.color_hex, c.icono, c.orden_display, c.es_final FROM empresa e
CROSS JOIN (VALUES
    ('ACTIVA',     'Activa',     '#10b981', 'play',         1, false),
    ('VENCIDA',    'Vencida',    '#f59e0b', 'clock',        2, true),
    ('CANCELADA',  'Cancelada',  '#dc2626', 'x-circle',     3, true),
    ('AGOTADA',    'Agotada',    '#94a3b8', 'minus-circle', 4, true),
    ('SUSPENDIDA', 'Suspendida', '#7c3aed', 'pause',        5, false)
) AS c(codigo, nombre, color_hex, icono, orden_display, es_final)
ON CONFLICT (empresa_id, codigo) DO NOTHING;

INSERT INTO estado_caja (empresa_id, codigo, nombre, color_hex, icono, orden_display, es_final)
SELECT e.id, c.codigo, c.nombre, c.color_hex, c.icono, c.orden_display, c.es_final FROM empresa e
CROSS JOIN (VALUES
    ('ABIERTA',     'Abierta',     '#10b981', 'play',         1, false),
    ('CERRADA',     'Cerrada',     '#94a3b8', 'lock',         2, true),
    ('EN_REVISION', 'En revision', '#f59e0b', 'eye',          3, false),
    ('AUDITADA',    'Auditada',    '#3b82f6', 'check-circle', 4, true)
) AS c(codigo, nombre, color_hex, icono, orden_display, es_final)
ON CONFLICT (empresa_id, codigo) DO NOTHING;

INSERT INTO tipo_movimiento_caja (empresa_id, codigo, nombre, color_hex, icono, orden_display, es_ingreso)
SELECT e.id, c.codigo, c.nombre, c.color_hex, c.icono, c.orden_display, c.es_ingreso FROM empresa e
CROSS JOIN (VALUES
    ('INGRESO_PAGO', 'Ingreso por pago', '#10b981', 'plus-circle',  1, true),
    ('RETIRO',       'Retiro',           '#dc2626', 'minus-circle', 2, false),
    ('DEPOSITO',     'Deposito',         '#3b82f6', 'arrow-down',   3, true),
    ('AJUSTE',       'Ajuste',           '#f59e0b', 'edit',         4, true),
    ('REVERSO',      'Reverso',          '#94a3b8', 'undo-2',       5, false)
) AS c(codigo, nombre, color_hex, icono, orden_display, es_ingreso)
ON CONFLICT (empresa_id, codigo) DO NOTHING;

INSERT INTO tipo_movimiento_saldo (empresa_id, codigo, nombre, color_hex, icono, orden_display, es_ingreso)
SELECT e.id, c.codigo, c.nombre, c.color_hex, c.icono, c.orden_display, c.es_ingreso FROM empresa e
CROSS JOIN (VALUES
    ('ABONO',          'Abono',          '#10b981', 'plus-circle',  1, true),
    ('CONSUMO',        'Consumo',        '#3b82f6', 'arrow-right',  2, false),
    ('REVERSO',        'Reverso',        '#94a3b8', 'undo-2',       3, true),
    ('AJUSTE_MANUAL',  'Ajuste manual',  '#f59e0b', 'edit',         4, true)
) AS c(codigo, nombre, color_hex, icono, orden_display, es_ingreso)
ON CONFLICT (empresa_id, codigo) DO NOTHING;

INSERT INTO tipo_descuento_convenio (empresa_id, codigo, nombre, color_hex, icono, orden_display)
SELECT e.id, c.codigo, c.nombre, c.color_hex, c.icono, c.orden_display FROM empresa e
CROSS JOIN (VALUES
    ('MONTO_FIJO',     'Monto fijo',     '#3b82f6', 'dollar-sign',  1),
    ('PORCENTAJE',     'Porcentaje',     '#10b981', 'percent',      2),
    ('MINUTOS_GRATIS', 'Minutos gratis', '#fbbf24', 'clock',        3)
) AS c(codigo, nombre, color_hex, icono, orden_display)
ON CONFLICT (empresa_id, codigo) DO NOTHING;

INSERT INTO origen_factura (empresa_id, codigo, nombre, color_hex, icono, orden_display)
SELECT e.id, c.codigo, c.nombre, c.color_hex, c.icono, c.orden_display FROM empresa e
CROSS JOIN (VALUES
    ('MANUAL',   'Manual',                  '#3b82f6', 'edit',         1),
    ('AUTO',     'Automatica (cierre)',     '#10b981', 'zap',          2),
    ('BACKFILL', 'Backfill (correccion)',   '#f59e0b', 'history',      3),
    ('OCR',      'OCR (deteccion)',         '#8b5cf6', 'camera',       4)
) AS c(codigo, nombre, color_hex, icono, orden_display)
ON CONFLICT (empresa_id, codigo) DO NOTHING;

-- ════════════════════════════════════════════════════════════════
-- v49 Fase 10: Soft-delete uniforme
-- Estandariza el soft-delete a (archivado_en TIMESTAMP, archivado_por_usuario_id)
-- en todas las tablas que hoy usan estado_id=3 o flags propios.
-- Las columnas son NULLABLE; cuando llenan se considera archivado.
-- ════════════════════════════════════════════════════════════════

ALTER TABLE empresa             ADD COLUMN IF NOT EXISTS archivado_en              TIMESTAMP;
ALTER TABLE empresa             ADD COLUMN IF NOT EXISTS archivado_por_usuario_id  BIGINT;
ALTER TABLE parqueadero         ADD COLUMN IF NOT EXISTS archivado_en              TIMESTAMP;
ALTER TABLE parqueadero         ADD COLUMN IF NOT EXISTS archivado_por_usuario_id  BIGINT;
ALTER TABLE nivel               ADD COLUMN IF NOT EXISTS archivado_en              TIMESTAMP;
ALTER TABLE nivel               ADD COLUMN IF NOT EXISTS archivado_por_usuario_id  BIGINT;
ALTER TABLE seccion             ADD COLUMN IF NOT EXISTS archivado_en              TIMESTAMP;
ALTER TABLE seccion             ADD COLUMN IF NOT EXISTS archivado_por_usuario_id  BIGINT;
ALTER TABLE sub_seccion         ADD COLUMN IF NOT EXISTS archivado_en              TIMESTAMP;
ALTER TABLE sub_seccion         ADD COLUMN IF NOT EXISTS archivado_por_usuario_id  BIGINT;
ALTER TABLE punto_parqueo       ADD COLUMN IF NOT EXISTS archivado_en              TIMESTAMP;
ALTER TABLE punto_parqueo       ADD COLUMN IF NOT EXISTS archivado_por_usuario_id  BIGINT;
ALTER TABLE camara              ADD COLUMN IF NOT EXISTS archivado_en              TIMESTAMP;
ALTER TABLE camara              ADD COLUMN IF NOT EXISTS archivado_por_usuario_id  BIGINT;
ALTER TABLE tarifa              ADD COLUMN IF NOT EXISTS archivado_por_usuario_id  BIGINT;
ALTER TABLE persona             ADD COLUMN IF NOT EXISTS archivado_en              TIMESTAMP;
ALTER TABLE persona             ADD COLUMN IF NOT EXISTS archivado_por_usuario_id  BIGINT;
ALTER TABLE usuario             ADD COLUMN IF NOT EXISTS archivado_en              TIMESTAMP;
ALTER TABLE usuario             ADD COLUMN IF NOT EXISTS archivado_por_usuario_id  BIGINT;
ALTER TABLE convenio            ADD COLUMN IF NOT EXISTS archivado_en              TIMESTAMP;
ALTER TABLE convenio            ADD COLUMN IF NOT EXISTS archivado_por_usuario_id  BIGINT;
ALTER TABLE dispositivo         ADD COLUMN IF NOT EXISTS archivado_en              TIMESTAMP;
ALTER TABLE dispositivo         ADD COLUMN IF NOT EXISTS archivado_por_usuario_id  BIGINT;

-- Backfill: entidades con estado_id=3 (ARCHIVADO) heredan archivado_en.
-- Asumimos que en este punto del proyecto TODAS estas tablas tienen
-- estado_id (creado en su CREATE TABLE inicial). Si alguna no la tiene,
-- el UPDATE fallaria — pero no es el caso aqui.
UPDATE empresa       SET archivado_en = CURRENT_TIMESTAMP WHERE estado_id = 3 AND archivado_en IS NULL;
UPDATE parqueadero   SET archivado_en = CURRENT_TIMESTAMP WHERE estado_id = 3 AND archivado_en IS NULL;
UPDATE nivel         SET archivado_en = CURRENT_TIMESTAMP WHERE estado_id = 3 AND archivado_en IS NULL;
UPDATE seccion       SET archivado_en = CURRENT_TIMESTAMP WHERE estado_id = 3 AND archivado_en IS NULL;
UPDATE sub_seccion   SET archivado_en = CURRENT_TIMESTAMP WHERE estado_id = 3 AND archivado_en IS NULL;
UPDATE punto_parqueo SET archivado_en = CURRENT_TIMESTAMP WHERE estado_id = 3 AND archivado_en IS NULL;

-- Indices parciales para queries de "no archivados"
CREATE INDEX IF NOT EXISTS idx_empresa_vigente     ON empresa(id)     WHERE archivado_en IS NULL;
CREATE INDEX IF NOT EXISTS idx_parqueadero_vigente ON parqueadero(id) WHERE archivado_en IS NULL;
CREATE INDEX IF NOT EXISTS idx_punto_vigente       ON punto_parqueo(id) WHERE archivado_en IS NULL;
CREATE INDEX IF NOT EXISTS idx_convenio_vigente    ON convenio(id)    WHERE archivado_en IS NULL;

-- ════════════════════════════════════════════════════════════════
-- v49 Fase 5: Enriquecimiento de entities pobres
-- Agrega columnas faltantes que el negocio real necesita pero el
-- modelo MVP no tenia. Todas NULLABLE para retrocompatibilidad.
-- ════════════════════════════════════════════════════════════════

-- Persona (6 → 14 cols)
ALTER TABLE persona ADD COLUMN IF NOT EXISTS segundo_nombre       VARCHAR(100);
ALTER TABLE persona ADD COLUMN IF NOT EXISTS segundo_apellido     VARCHAR(100);
ALTER TABLE persona ADD COLUMN IF NOT EXISTS correo               VARCHAR(200);
ALTER TABLE persona ADD COLUMN IF NOT EXISTS fecha_nacimiento     DATE;
ALTER TABLE persona ADD COLUMN IF NOT EXISTS genero_id            BIGINT REFERENCES genero(id);
ALTER TABLE persona ADD COLUMN IF NOT EXISTS tipo_documento_id    BIGINT REFERENCES tipo_documento(id);
ALTER TABLE persona ADD COLUMN IF NOT EXISTS direccion            VARCHAR(300);
ALTER TABLE persona ADD COLUMN IF NOT EXISTS ciudad_id            BIGINT REFERENCES ciudad(id);
CREATE INDEX IF NOT EXISTS idx_persona_correo ON persona(correo) WHERE correo IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_persona_documento ON persona(numero_documento) WHERE numero_documento IS NOT NULL;

-- Empresa (6 → 14 cols)
ALTER TABLE empresa ADD COLUMN IF NOT EXISTS tipo_documento_id    BIGINT REFERENCES tipo_documento(id);
ALTER TABLE empresa ADD COLUMN IF NOT EXISTS regimen_tributario_id BIGINT REFERENCES regimen_tributario(id);
ALTER TABLE empresa ADD COLUMN IF NOT EXISTS moneda_id            BIGINT REFERENCES moneda(id);
ALTER TABLE empresa ADD COLUMN IF NOT EXISTS direccion            VARCHAR(300);
ALTER TABLE empresa ADD COLUMN IF NOT EXISTS ciudad_id            BIGINT REFERENCES ciudad(id);
ALTER TABLE empresa ADD COLUMN IF NOT EXISTS correo_contacto      VARCHAR(200);
ALTER TABLE empresa ADD COLUMN IF NOT EXISTS telefono_contacto    VARCHAR(20);
ALTER TABLE empresa ADD COLUMN IF NOT EXISTS sitio_web            VARCHAR(300);
ALTER TABLE empresa ADD COLUMN IF NOT EXISTS logo_url             VARCHAR(300);

-- Vehiculo (9 → 17 cols)
ALTER TABLE vehiculo ADD COLUMN IF NOT EXISTS marca               VARCHAR(100);
ALTER TABLE vehiculo ADD COLUMN IF NOT EXISTS modelo              VARCHAR(100);
ALTER TABLE vehiculo ADD COLUMN IF NOT EXISTS anio                INTEGER;
ALTER TABLE vehiculo ADD COLUMN IF NOT EXISTS placa_pais          VARCHAR(2) DEFAULT 'CO';
ALTER TABLE vehiculo ADD COLUMN IF NOT EXISTS soat_vence          DATE;
ALTER TABLE vehiculo ADD COLUMN IF NOT EXISTS tecnomecanica_vence DATE;
ALTER TABLE vehiculo ADD COLUMN IF NOT EXISTS observaciones       TEXT;
ALTER TABLE vehiculo ADD COLUMN IF NOT EXISTS imagen_url          VARCHAR(300);

-- Reserva (8 → 14 cols)
ALTER TABLE reserva ADD COLUMN IF NOT EXISTS canal_origen         VARCHAR(50);
ALTER TABLE reserva ADD COLUMN IF NOT EXISTS notas                TEXT;
ALTER TABLE reserva ADD COLUMN IF NOT EXISTS confirmada_en        TIMESTAMP;
ALTER TABLE reserva ADD COLUMN IF NOT EXISTS cancelada_en         TIMESTAMP;
ALTER TABLE reserva ADD COLUMN IF NOT EXISTS cancelada_por_usuario_id BIGINT;
ALTER TABLE reserva ADD COLUMN IF NOT EXISTS motivo_cancelacion   TEXT;

-- Camara (12 → 17 cols)
ALTER TABLE camara ADD COLUMN IF NOT EXISTS marca                 VARCHAR(100);
ALTER TABLE camara ADD COLUMN IF NOT EXISTS modelo                VARCHAR(100);
ALTER TABLE camara ADD COLUMN IF NOT EXISTS ip                    VARCHAR(45);
ALTER TABLE camara ADD COLUMN IF NOT EXISTS mac                   VARCHAR(17);
ALTER TABLE camara ADD COLUMN IF NOT EXISTS resolucion            VARCHAR(20);

-- Dispositivo (8 → 14 cols)
ALTER TABLE dispositivo ADD COLUMN IF NOT EXISTS marca            VARCHAR(100);
ALTER TABLE dispositivo ADD COLUMN IF NOT EXISTS modelo           VARCHAR(100);
ALTER TABLE dispositivo ADD COLUMN IF NOT EXISTS serial           VARCHAR(100);
ALTER TABLE dispositivo ADD COLUMN IF NOT EXISTS firmware_version VARCHAR(50);
ALTER TABLE dispositivo ADD COLUMN IF NOT EXISTS ultima_lectura   TIMESTAMP;

-- PuntoParqueo (10 → 13 cols)
ALTER TABLE punto_parqueo ADD COLUMN IF NOT EXISTS reservable     BOOLEAN DEFAULT TRUE;
ALTER TABLE punto_parqueo ADD COLUMN IF NOT EXISTS observaciones  TEXT;

-- Factura (24 → 26 cols)
ALTER TABLE factura ADD COLUMN IF NOT EXISTS fecha_vencimiento    DATE;
ALTER TABLE factura ADD COLUMN IF NOT EXISTS observaciones        TEXT;

-- Pago (18 → 20 cols)
ALTER TABLE pago ADD COLUMN IF NOT EXISTS referencia_externa      VARCHAR(200);
ALTER TABLE pago ADD COLUMN IF NOT EXISTS observaciones           TEXT;

-- ════════════════════════════════════════════════════════════════
-- v49 Fase 4: empresa_validacion_campo
-- Sistema de validacion por campo editable por empresa. Cada empresa
-- puede definir reglas (required, min, max, regex, longitud) para los
-- campos de sus DTOs. El backend resuelve en runtime via service.
-- ════════════════════════════════════════════════════════════════

CREATE TABLE IF NOT EXISTS empresa_validacion_campo (
    id              BIGSERIAL PRIMARY KEY,
    empresa_id      BIGINT NOT NULL REFERENCES empresa(id) ON DELETE CASCADE,
    entidad         VARCHAR(80)  NOT NULL,
    campo           VARCHAR(80)  NOT NULL,
    requerido       BOOLEAN      NOT NULL DEFAULT FALSE,
    longitud_min    INTEGER,
    longitud_max    INTEGER,
    valor_min       NUMERIC,
    valor_max       NUMERIC,
    regex           TEXT,
    mensaje_error   VARCHAR(500),
    activa          BOOLEAN      NOT NULL DEFAULT TRUE,
    fecha_creacion  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_actualizacion TIMESTAMP,
    actualizado_por_usuario_id BIGINT,
    CONSTRAINT uq_emp_validacion UNIQUE (empresa_id, entidad, campo)
);
CREATE INDEX IF NOT EXISTS idx_validacion_campo_empresa ON empresa_validacion_campo(empresa_id);

-- Seed: 20 reglas default para CADA empresa (las mas comunes)
INSERT INTO empresa_validacion_campo (empresa_id, entidad, campo, requerido, longitud_min, longitud_max, valor_min, valor_max, regex, mensaje_error)
SELECT e.id, c.entidad, c.campo, c.requerido, c.longitud_min, c.longitud_max, c.valor_min, c.valor_max, c.regex, c.mensaje_error
FROM empresa e
CROSS JOIN (VALUES
    ('persona', 'nombre',           true,  2,    100,  NULL, NULL, NULL, 'El nombre es obligatorio (2-100 chars)'),
    ('persona', 'apellido',         true,  2,    100,  NULL, NULL, NULL, 'El apellido es obligatorio (2-100 chars)'),
    ('persona', 'correo',           false, 5,    200,  NULL, NULL, '^[^@\s]+@[^@\s]+\.[^@\s]+$', 'Correo invalido'),
    ('persona', 'telefono',         true,  7,    20,   NULL, NULL, '^[0-9+\-\s()]+$', 'Telefono: solo numeros, +, -, espacios, parentesis'),
    ('persona', 'numero_documento', true,  4,    50,   NULL, NULL, '^[A-Za-z0-9\-]+$', 'Documento alfanumerico'),
    ('vehiculo', 'placa',           true,  4,    20,   NULL, NULL, '^[A-Z]{3}\d{3}$|^[A-Z]{3}\d{2}[A-Z]$|^VIS-.*$', 'Placa colombiana o visitante'),
    ('vehiculo', 'color',           false, 2,    50,   NULL, NULL, NULL, 'Color: 2-50 chars'),
    ('vehiculo', 'marca',           false, 1,    100,  NULL, NULL, NULL, 'Marca: 1-100 chars'),
    ('vehiculo', 'modelo',          false, 1,    100,  NULL, NULL, NULL, 'Modelo: 1-100 chars'),
    ('empresa', 'nombre',           true,  3,    200,  NULL, NULL, NULL, 'Nombre empresa: 3-200 chars'),
    ('empresa', 'nit',              false, 6,    30,   NULL, NULL, '^[0-9\-]+$', 'NIT: solo numeros y guion'),
    ('empresa', 'correo_contacto',  false, 5,    200,  NULL, NULL, '^[^@\s]+@[^@\s]+\.[^@\s]+$', 'Correo de contacto invalido'),
    ('parqueadero', 'nombre',       true,  3,    200,  NULL, NULL, NULL, 'Nombre parqueadero: 3-200 chars'),
    ('parqueadero', 'direccion',    false, 5,    300,  NULL, NULL, NULL, 'Direccion: 5-300 chars'),
    ('tarifa', 'valor',             true,  NULL, NULL, 0,    1000000, NULL, 'Valor 0 a 1M'),
    ('tarifa', 'iva_porcentaje',    false, NULL, NULL, 0,    100,  NULL, 'IVA 0-100%'),
    ('factura', 'observaciones',    false, NULL, 1000, NULL, NULL, NULL, 'Max 1000 chars'),
    ('pago', 'monto',               true,  NULL, NULL, 0.01, 100000000, NULL, 'Monto > 0'),
    ('reserva', 'notas',            false, NULL, 1000, NULL, NULL, NULL, 'Max 1000 chars'),
    ('convenio', 'nombre_comercio', true,  3,    200,  NULL, NULL, NULL, 'Nombre comercio: 3-200 chars')
) AS c(entidad, campo, requerido, longitud_min, longitud_max, valor_min, valor_max, regex, mensaje_error)
ON CONFLICT (empresa_id, entidad, campo) DO NOTHING;

-- ════════════════════════════════════════════════════════════════
-- v49 Fase 8: Reportes parametrizables
-- En lugar de tener reportes hardcoded en ReportesSpecs.java, los
-- reportes son filas en BD con SQL template + filtros + columnas. El
-- service ejecuta SQL parametrizado de forma segura.
-- ════════════════════════════════════════════════════════════════

CREATE TABLE IF NOT EXISTS reporte_definicion (
    id              BIGSERIAL PRIMARY KEY,
    empresa_id      BIGINT REFERENCES empresa(id) ON DELETE CASCADE,
    clave           VARCHAR(80)  NOT NULL,
    nombre          VARCHAR(200) NOT NULL,
    descripcion     TEXT,
    sql_template    TEXT         NOT NULL,
    filtros_json    TEXT,
    columnas_json   TEXT,
    roles_permitidos VARCHAR(300),
    max_filas       INTEGER      NOT NULL DEFAULT 5000,
    formato_default VARCHAR(10)  NOT NULL DEFAULT 'JSON',
    activo          BOOLEAN      NOT NULL DEFAULT TRUE,
    fecha_creacion  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_actualizacion TIMESTAMP,
    creado_por_usuario_id BIGINT,
    actualizado_por_usuario_id BIGINT,
    CONSTRAINT uq_reporte_definicion UNIQUE (empresa_id, clave),
    CONSTRAINT ck_reporte_formato CHECK (formato_default IN ('JSON','CSV','PDF'))
);
CREATE INDEX IF NOT EXISTS idx_reporte_definicion_empresa ON reporte_definicion(empresa_id);

CREATE TABLE IF NOT EXISTS reporte_ejecutado (
    id              BIGSERIAL PRIMARY KEY,
    reporte_definicion_id BIGINT REFERENCES reporte_definicion(id) ON DELETE SET NULL,
    clave_reporte   VARCHAR(80)  NOT NULL,
    empresa_id      BIGINT REFERENCES empresa(id) ON DELETE CASCADE,
    parqueadero_id  BIGINT,
    parametros_json TEXT,
    formato         VARCHAR(10)  NOT NULL DEFAULT 'JSON',
    filas_devueltas INTEGER,
    duracion_ms     BIGINT,
    estado          VARCHAR(20)  NOT NULL DEFAULT 'OK',
    error_mensaje   TEXT,
    ejecutado_por_usuario_id BIGINT,
    fecha_hora      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_reporte_ejecutado_estado CHECK (estado IN ('OK','ERROR','TIMEOUT','CANCELADO'))
);
CREATE INDEX IF NOT EXISTS idx_reporte_ejecutado_clave_fecha  ON reporte_ejecutado(clave_reporte, fecha_hora DESC);
CREATE INDEX IF NOT EXISTS idx_reporte_ejecutado_empresa      ON reporte_ejecutado(empresa_id, fecha_hora DESC);

-- Seed: 6 reportes globales (empresa_id NULL = todos pueden usarlos)
-- Usamos WHERE NOT EXISTS para idempotencia. ON CONFLICT no funciona aqui
-- porque PostgreSQL trata NULLs como distintos en la UNIQUE constraint.
INSERT INTO reporte_definicion (empresa_id, clave, nombre, descripcion, sql_template, filtros_json, columnas_json, roles_permitidos)
SELECT * FROM (VALUES
(CAST(NULL AS BIGINT), 'tickets_por_dia',
    'Tickets por dia',
    'Conteo de tickets entrados/cerrados/anulados por dia para un parqueadero y rango.',
    'SELECT DATE(fecha_hora_entrada) AS dia, estado, COUNT(*) AS cant
     FROM ticket
     WHERE parqueadero_id = :parqueaderoId
       AND fecha_hora_entrada BETWEEN :desde AND :hasta
     GROUP BY DATE(fecha_hora_entrada), estado
     ORDER BY dia, estado',
    '[{"clave":"parqueaderoId","tipo":"long","requerido":true},{"clave":"desde","tipo":"timestamp","requerido":true},{"clave":"hasta","tipo":"timestamp","requerido":true}]',
    '[{"clave":"dia","tipo":"date"},{"clave":"estado","tipo":"string"},{"clave":"cant","tipo":"long"}]',
    'ADMIN,SUPER_ADMIN,ADMIN_PARQUEADERO'),

(CAST(NULL AS BIGINT), 'ingresos_por_metodo',
    'Ingresos por metodo de pago',
    'Suma de pagos completados por metodo en un periodo.',
    'SELECT metodo, SUM(monto) AS total, COUNT(*) AS cant
     FROM pago
     WHERE estado = ''COMPLETADO''
       AND fecha_hora BETWEEN :desde AND :hasta
     GROUP BY metodo
     ORDER BY total DESC',
    '[{"clave":"desde","tipo":"timestamp","requerido":true},{"clave":"hasta","tipo":"timestamp","requerido":true}]',
    '[{"clave":"metodo","tipo":"string"},{"clave":"total","tipo":"decimal"},{"clave":"cant","tipo":"long"}]',
    'ADMIN,SUPER_ADMIN'),

(CAST(NULL AS BIGINT), 'top_vehiculos',
    'Top vehiculos por visitas',
    'Vehiculos con mas tickets en el periodo.',
    'SELECT v.placa, COUNT(*) AS visitas, SUM(t.monto_calculado) AS gasto_total
     FROM ticket t JOIN vehiculo v ON t.vehiculo_id = v.id
     WHERE t.fecha_hora_entrada BETWEEN :desde AND :hasta
     GROUP BY v.placa
     ORDER BY visitas DESC
     LIMIT :limite',
    '[{"clave":"desde","tipo":"timestamp","requerido":true},{"clave":"hasta","tipo":"timestamp","requerido":true},{"clave":"limite","tipo":"integer","default":50}]',
    '[{"clave":"placa","tipo":"string"},{"clave":"visitas","tipo":"long"},{"clave":"gasto_total","tipo":"decimal"}]',
    'ADMIN,SUPER_ADMIN'),

(CAST(NULL AS BIGINT), 'ocupacion_actual',
    'Ocupacion actual de puntos',
    'Cuantos puntos ocupados vs libres por parqueadero.',
    'SELECT pa.id AS parqueadero_id, pa.nombre AS parqueadero,
            COUNT(DISTINCT pp.id) AS total_puntos,
            COUNT(DISTINCT t.id) AS ocupados
     FROM parqueadero pa
     LEFT JOIN nivel n ON n.parqueadero_id = pa.id
     LEFT JOIN seccion s ON s.nivel_id = n.id
     LEFT JOIN sub_seccion ss ON ss.seccion_id = s.id
     LEFT JOIN punto_parqueo pp ON pp.sub_seccion_id = ss.id
     LEFT JOIN ticket t ON t.punto_parqueo_id = pp.id AND t.estado = ''EN_CURSO''
     WHERE pa.id = :parqueaderoId
     GROUP BY pa.id, pa.nombre',
    '[{"clave":"parqueaderoId","tipo":"long","requerido":true}]',
    '[{"clave":"parqueadero_id","tipo":"long"},{"clave":"parqueadero","tipo":"string"},{"clave":"total_puntos","tipo":"long"},{"clave":"ocupados","tipo":"long"}]',
    'ADMIN,SUPER_ADMIN,ADMIN_PARQUEADERO,OPERARIO_CAJA'),

(CAST(NULL AS BIGINT), 'facturas_pendientes',
    'Facturas pendientes de pago',
    'Facturas en estado PENDIENTE con monto y antiguedad.',
    'SELECT f.id, f.valor_total, f.fecha_hora,
            EXTRACT(DAY FROM (CURRENT_TIMESTAMP - f.fecha_hora))::integer AS dias_atras,
            f.placa_snapshot AS placa
     FROM factura f
     WHERE f.estado = ''PENDIENTE''
       AND f.parqueadero_id = :parqueaderoId
     ORDER BY f.fecha_hora ASC
     LIMIT :limite',
    '[{"clave":"parqueaderoId","tipo":"long","requerido":true},{"clave":"limite","tipo":"integer","default":200}]',
    '[{"clave":"id","tipo":"long"},{"clave":"valor_total","tipo":"decimal"},{"clave":"fecha_hora","tipo":"timestamp"},{"clave":"dias_atras","tipo":"integer"},{"clave":"placa","tipo":"string"}]',
    'ADMIN,SUPER_ADMIN,ADMIN_PARQUEADERO'),

(CAST(NULL AS BIGINT), 'operadores_actividad',
    'Actividad de operadores',
    'Tickets cerrados y pagos cobrados por operador.',
    'SELECT t.cerrado_por_usuario_id AS usuario_id,
            t.operador_salida_nombre_snapshot AS operador,
            COUNT(*) AS tickets_cerrados,
            SUM(t.monto_calculado) AS total_cobrado
     FROM ticket t
     WHERE t.estado = ''CERRADO''
       AND t.fecha_hora_salida BETWEEN :desde AND :hasta
       AND t.parqueadero_id = :parqueaderoId
     GROUP BY t.cerrado_por_usuario_id, t.operador_salida_nombre_snapshot
     ORDER BY tickets_cerrados DESC',
    '[{"clave":"parqueaderoId","tipo":"long","requerido":true},{"clave":"desde","tipo":"timestamp","requerido":true},{"clave":"hasta","tipo":"timestamp","requerido":true}]',
    '[{"clave":"usuario_id","tipo":"long"},{"clave":"operador","tipo":"string"},{"clave":"tickets_cerrados","tipo":"long"},{"clave":"total_cobrado","tipo":"decimal"}]',
    'ADMIN,SUPER_ADMIN,ADMIN_PARQUEADERO')
) AS r(empresa_id, clave, nombre, descripcion, sql_template, filtros_json, columnas_json, roles_permitidos)
WHERE NOT EXISTS (
    SELECT 1 FROM reporte_definicion rd WHERE rd.empresa_id IS NULL AND rd.clave = r.clave
);

-- ════════════════════════════════════════════════════════════════
-- v49 COMPLETAR — Bloque A: catálogos globales faltantes (5)
-- ════════════════════════════════════════════════════════════════

-- 1. estado_civil
CREATE TABLE IF NOT EXISTS estado_civil (
    id BIGSERIAL PRIMARY KEY,
    codigo VARCHAR(20) NOT NULL UNIQUE,
    nombre VARCHAR(100) NOT NULL,
    descripcion TEXT,
    orden_display INTEGER,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    fecha_creacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_actualizacion TIMESTAMP
);
ALTER TABLE estado_civil ALTER COLUMN fecha_creacion SET DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE estado_civil ALTER COLUMN activo SET DEFAULT TRUE;
UPDATE estado_civil SET fecha_creacion = CURRENT_TIMESTAMP WHERE fecha_creacion IS NULL;
UPDATE estado_civil SET activo = TRUE WHERE activo IS NULL;
INSERT INTO estado_civil (codigo, nombre, orden_display) VALUES
    ('SOLTERO',     'Soltero/a',     1),
    ('CASADO',      'Casado/a',      2),
    ('UNION_LIBRE', 'Union libre',   3),
    ('DIVORCIADO',  'Divorciado/a',  4),
    ('VIUDO',       'Viudo/a',       5),
    ('SEPARADO',    'Separado/a',    6)
ON CONFLICT (codigo) DO NOTHING;

-- 2. pais_codigo_placa (para Vehiculo.placa_pais_id)
CREATE TABLE IF NOT EXISTS pais_codigo_placa (
    id BIGSERIAL PRIMARY KEY,
    codigo VARCHAR(2) NOT NULL UNIQUE,
    nombre VARCHAR(100) NOT NULL,
    regex_placa VARCHAR(200),
    orden_display INTEGER,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    fecha_creacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_actualizacion TIMESTAMP
);
ALTER TABLE pais_codigo_placa ALTER COLUMN fecha_creacion SET DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE pais_codigo_placa ALTER COLUMN activo SET DEFAULT TRUE;
UPDATE pais_codigo_placa SET fecha_creacion = CURRENT_TIMESTAMP WHERE fecha_creacion IS NULL;
UPDATE pais_codigo_placa SET activo = TRUE WHERE activo IS NULL;
INSERT INTO pais_codigo_placa (codigo, nombre, regex_placa, orden_display) VALUES
    ('CO', 'Colombia',  '^[A-Z]{3}\d{3}$|^[A-Z]{3}\d{2}[A-Z]$', 1),
    ('EC', 'Ecuador',   '^[A-Z]{3}\d{3,4}$',                    2),
    ('VE', 'Venezuela', '^[A-Z]{2,3}\d{2,3}[A-Z]?$',            3),
    ('PE', 'Peru',      '^[A-Z]\d[A-Z]-\d{3}$',                 4),
    ('PA', 'Panama',    '^[A-Z]{2}\d{3,4}$',                    5),
    ('MX', 'Mexico',    '^[A-Z]{3}-\d{3}-[A-Z]$',               6),
    ('US', 'EE.UU.',    '^[A-Z0-9]{1,7}$',                      7),
    ('BR', 'Brasil',    '^[A-Z]{3}\d[A-Z]\d{2}$|^[A-Z]{3}\d{4}$', 8)
ON CONFLICT (codigo) DO NOTHING;

-- 3. tipo_servicio_vehiculo (para Vehiculo.tipo_servicio_id)
CREATE TABLE IF NOT EXISTS tipo_servicio_vehiculo (
    id BIGSERIAL PRIMARY KEY,
    codigo VARCHAR(50) NOT NULL UNIQUE,
    nombre VARCHAR(100) NOT NULL,
    descripcion TEXT,
    orden_display INTEGER,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    fecha_creacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_actualizacion TIMESTAMP
);
ALTER TABLE tipo_servicio_vehiculo ALTER COLUMN fecha_creacion SET DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE tipo_servicio_vehiculo ALTER COLUMN activo SET DEFAULT TRUE;
UPDATE tipo_servicio_vehiculo SET fecha_creacion = CURRENT_TIMESTAMP WHERE fecha_creacion IS NULL;
UPDATE tipo_servicio_vehiculo SET activo = TRUE WHERE activo IS NULL;
INSERT INTO tipo_servicio_vehiculo (codigo, nombre, descripcion, orden_display) VALUES
    ('PARTICULAR',   'Particular',   'Uso personal del propietario',                1),
    ('PUBLICO',      'Publico',      'Taxi, transporte de pasajeros',               2),
    ('OFICIAL',      'Oficial',      'Entidades gubernamentales',                   3),
    ('DIPLOMATICO',  'Diplomatico',  'Cuerpo diplomatico',                          4),
    ('CARGA',        'Carga',        'Transporte de carga',                         5),
    ('EMERGENCIA',   'Emergencia',   'Ambulancia, bomberos, policia',               6)
ON CONFLICT (codigo) DO NOTHING;

-- 4. tipo_acceso_dispositivo (para Dispositivo.tipo_acceso_id)
CREATE TABLE IF NOT EXISTS tipo_acceso_dispositivo (
    id BIGSERIAL PRIMARY KEY,
    codigo VARCHAR(50) NOT NULL UNIQUE,
    nombre VARCHAR(100) NOT NULL,
    descripcion TEXT,
    icono VARCHAR(50),
    orden_display INTEGER,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    fecha_creacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_actualizacion TIMESTAMP
);
ALTER TABLE tipo_acceso_dispositivo ALTER COLUMN fecha_creacion SET DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE tipo_acceso_dispositivo ALTER COLUMN activo SET DEFAULT TRUE;
UPDATE tipo_acceso_dispositivo SET fecha_creacion = CURRENT_TIMESTAMP WHERE fecha_creacion IS NULL;
UPDATE tipo_acceso_dispositivo SET activo = TRUE WHERE activo IS NULL;
INSERT INTO tipo_acceso_dispositivo (codigo, nombre, descripcion, icono, orden_display) VALUES
    ('RFID',      'RFID',           'Tarjeta o tag de proximidad',     'credit-card',  1),
    ('MANUAL',    'Manual',         'Apertura manual del operador',    'hand',         2),
    ('OCR',       'OCR',            'Reconocimiento de placa',         'camera',       3),
    ('AUTO',      'Auto',           'Apertura automatica por sensor',  'zap',          4),
    ('BLUETOOTH', 'Bluetooth',      'Pareo con dispositivo movil',     'bluetooth',    5),
    ('QR',        'QR',             'Codigo QR escaneado',             'qr-code',      6),
    ('TICKET',    'Ticket impreso', 'Codigo de barras del ticket',     'ticket',       7)
ON CONFLICT (codigo) DO NOTHING;

-- 5. canal_origen_reserva (para Reserva.canal_origen_id)
CREATE TABLE IF NOT EXISTS canal_origen_reserva (
    id BIGSERIAL PRIMARY KEY,
    codigo VARCHAR(50) NOT NULL UNIQUE,
    nombre VARCHAR(100) NOT NULL,
    descripcion TEXT,
    icono VARCHAR(50),
    orden_display INTEGER,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    fecha_creacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_actualizacion TIMESTAMP
);
ALTER TABLE canal_origen_reserva ALTER COLUMN fecha_creacion SET DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE canal_origen_reserva ALTER COLUMN activo SET DEFAULT TRUE;
UPDATE canal_origen_reserva SET fecha_creacion = CURRENT_TIMESTAMP WHERE fecha_creacion IS NULL;
UPDATE canal_origen_reserva SET activo = TRUE WHERE activo IS NULL;
INSERT INTO canal_origen_reserva (codigo, nombre, icono, orden_display) VALUES
    ('WEB',         'Web',           'globe',       1),
    ('APP_ANDROID', 'App Android',   'smartphone',  2),
    ('APP_IOS',     'App iOS',       'smartphone',  3),
    ('TELEFONO',    'Telefono',      'phone',       4),
    ('PRESENCIAL',  'Presencial',    'user',        5),
    ('WHATSAPP',    'WhatsApp',      'message-circle', 6),
    ('EMAIL',       'Email',         'mail',        7)
ON CONFLICT (codigo) DO NOTHING;

-- ════════════════════════════════════════════════════════════════
-- v49 COMPLETAR — Bloque A: tipo_resolucion_dian por empresa (Fase 2)
-- ════════════════════════════════════════════════════════════════
CREATE TABLE IF NOT EXISTS tipo_resolucion_dian (
    id BIGSERIAL PRIMARY KEY,
    empresa_id BIGINT NOT NULL REFERENCES empresa(id) ON DELETE CASCADE,
    codigo VARCHAR(50) NOT NULL,
    nombre VARCHAR(100) NOT NULL,
    descripcion TEXT,
    color_hex VARCHAR(9),
    icono VARCHAR(50),
    orden_display INTEGER,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    fecha_creacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_actualizacion TIMESTAMP,
    CONSTRAINT uq_tipo_resolucion_dian UNIQUE (empresa_id, codigo)
);
ALTER TABLE tipo_resolucion_dian ALTER COLUMN fecha_creacion SET DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE tipo_resolucion_dian ALTER COLUMN activo SET DEFAULT TRUE;
UPDATE tipo_resolucion_dian SET fecha_creacion = CURRENT_TIMESTAMP WHERE fecha_creacion IS NULL;
UPDATE tipo_resolucion_dian SET activo = TRUE WHERE activo IS NULL;
INSERT INTO tipo_resolucion_dian (empresa_id, codigo, nombre, color_hex, icono, orden_display)
SELECT e.id, c.codigo, c.nombre, c.color_hex, c.icono, c.orden_display FROM empresa e
CROSS JOIN (VALUES
    ('POS',                  'POS',                       '#3b82f6', 'receipt',    1),
    ('FACTURA_ELECTRONICA',  'Factura electronica',       '#10b981', 'file-text',  2),
    ('CONTINGENCIA',         'Contingencia',              '#f59e0b', 'alert-triangle', 3)
) AS c(codigo, nombre, color_hex, icono, orden_display)
ON CONFLICT (empresa_id, codigo) DO NOTHING;

-- ════════════════════════════════════════════════════════════════
-- v49 COMPLETAR — Bloque A: 11 settings empresa_config faltantes
-- ════════════════════════════════════════════════════════════════
INSERT INTO empresa_config (empresa_id, clave, valor, tipo, valor_min, valor_max, descripcion, categoria)
SELECT e.id, c.clave, c.valor, c.tipo, c.valor_min, c.valor_max, c.descripcion, c.categoria
FROM empresa e
CROSS JOIN (VALUES
    ('factura.email_automatico',                  'false',                             'BOOLEAN', NULL, NULL, 'Enviar factura por email automaticamente al emitir', 'facturacion'),
    ('factura.formato_numero',                    '{prefijo}-{consecutivo:06d}',       'STRING',  NULL, NULL, 'Plantilla para el numero de factura', 'facturacion'),
    ('caja.requiere_apertura_para_cobrar',        'false',                             'BOOLEAN', NULL, NULL, 'Operador necesita caja abierta para registrar pagos', 'caja'),
    ('caja.max_diferencia_warn',                  '1000',                              'DECIMAL', 0,    NULL, 'Diferencia maxima de arqueo sin advertencia', 'caja'),
    ('caja.cierre_obligatorio_diario',            'true',                              'BOOLEAN', NULL, NULL, 'Forzar cierre de caja al fin del dia', 'caja'),
    ('seguridad.password_min_length',             '8',                                 'INTEGER', 6,    32,   'Longitud minima de password al registrar', 'seguridad'),
    ('seguridad.password_requiere_simbolos',      'false',                             'BOOLEAN', NULL, NULL, 'Password debe incluir caracteres especiales', 'seguridad'),
    ('seguridad.jwt_expiracion_minutos',          '60',                                'INTEGER', 5,    1440, 'Expiracion del access token en minutos', 'seguridad'),
    ('seguridad.bloqueo_intentos_fallidos',       '5',                                 'INTEGER', 3,    20,   'Numero de intentos fallidos antes de bloquear cuenta', 'seguridad'),
    ('notificacion.email_habilitado',             'true',                              'BOOLEAN', NULL, NULL, 'Enviar notificaciones por email', 'notificacion'),
    ('notificacion.websocket_habilitado',         'true',                              'BOOLEAN', NULL, NULL, 'Push notifications via WebSocket', 'notificacion')
) AS c(clave, valor, tipo, valor_min, valor_max, descripcion, categoria)
ON CONFLICT (empresa_id, clave) DO NOTHING;

-- ════════════════════════════════════════════════════════════════
-- v49 COMPLETAR — Bloque A: 8 reportes globales adicionales
-- ════════════════════════════════════════════════════════════════
INSERT INTO reporte_definicion (empresa_id, clave, nombre, descripcion, sql_template, filtros_json, columnas_json, roles_permitidos)
SELECT * FROM (VALUES
(CAST(NULL AS BIGINT), 'vehiculos_visitantes_activos',
    'Vehiculos visitantes activos',
    'Vehiculos marcados como visitantes (sin persona) con actividad reciente.',
    'SELECT v.placa, v.color, v.ultima_actividad,
            tv.nombre AS tipo_vehiculo
     FROM vehiculo v
     LEFT JOIN tipo_vehiculo tv ON v.tipo_vehiculo_id = tv.id
     WHERE v.es_visitante = true
       AND v.activo = true
       AND v.ultima_actividad >= :desde
     ORDER BY v.ultima_actividad DESC
     LIMIT :limite',
    '[{"clave":"desde","tipo":"timestamp","requerido":true},{"clave":"limite","tipo":"integer","default":200}]',
    '[{"clave":"placa","tipo":"string"},{"clave":"color","tipo":"string"},{"clave":"ultima_actividad","tipo":"timestamp"},{"clave":"tipo_vehiculo","tipo":"string"}]',
    'ADMIN,SUPER_ADMIN,ADMIN_PARQUEADERO'),

(CAST(NULL AS BIGINT), 'ingresos_diarios',
    'Ingresos diarios',
    'Total cobrado por dia (pagos completados) en un rango.',
    'SELECT DATE(fecha_hora) AS dia, SUM(monto) AS total, COUNT(*) AS cant_pagos
     FROM pago
     WHERE estado = ''COMPLETADO''
       AND fecha_hora BETWEEN :desde AND :hasta
     GROUP BY DATE(fecha_hora)
     ORDER BY dia',
    '[{"clave":"desde","tipo":"timestamp","requerido":true},{"clave":"hasta","tipo":"timestamp","requerido":true}]',
    '[{"clave":"dia","tipo":"date"},{"clave":"total","tipo":"decimal"},{"clave":"cant_pagos","tipo":"long"}]',
    'ADMIN,SUPER_ADMIN,ADMIN_PARQUEADERO'),

(CAST(NULL AS BIGINT), 'suscripciones_por_vencer',
    'Suscripciones por vencer',
    'Suscripciones que vencen en los proximos N dias.',
    'SELECT s.id, v.placa, s.tipo, s.fecha_fin,
            EXTRACT(DAY FROM (s.fecha_fin - CURRENT_TIMESTAMP))::integer AS dias_para_vencer
     FROM suscripcion s
     JOIN vehiculo v ON s.vehiculo_id = v.id
     WHERE s.estado = ''ACTIVA''
       AND s.fecha_fin BETWEEN CURRENT_TIMESTAMP AND CURRENT_TIMESTAMP + (:dias || '' days'')::interval
     ORDER BY s.fecha_fin ASC',
    '[{"clave":"dias","tipo":"integer","default":7}]',
    '[{"clave":"id","tipo":"long"},{"clave":"placa","tipo":"string"},{"clave":"tipo","tipo":"string"},{"clave":"fecha_fin","tipo":"timestamp"},{"clave":"dias_para_vencer","tipo":"integer"}]',
    'ADMIN,SUPER_ADMIN,ADMIN_PARQUEADERO'),

(CAST(NULL AS BIGINT), 'cierre_caja_pendiente',
    'Cajas con cierre pendiente',
    'Cajas abiertas que ya deberian estar cerradas (mas de 12h).',
    'SELECT c.id, c.usuario_id, c.fecha_apertura, c.fondo_inicial,
            EXTRACT(HOUR FROM (CURRENT_TIMESTAMP - c.fecha_apertura))::integer AS horas_abierta
     FROM caja c
     WHERE c.estado = ''ABIERTA''
       AND c.fecha_apertura < CURRENT_TIMESTAMP - INTERVAL ''12 hours''
     ORDER BY c.fecha_apertura ASC',
    '[]',
    '[{"clave":"id","tipo":"long"},{"clave":"usuario_id","tipo":"long"},{"clave":"fecha_apertura","tipo":"timestamp"},{"clave":"fondo_inicial","tipo":"decimal"},{"clave":"horas_abierta","tipo":"integer"}]',
    'ADMIN,SUPER_ADMIN,ADMIN_PARQUEADERO'),

(CAST(NULL AS BIGINT), 'tickets_anulados',
    'Tickets anulados con motivos',
    'Listado de tickets anulados con motivo y operador.',
    'SELECT t.id, t.placa_snapshot AS placa, t.fecha_hora_entrada, t.anulado_en,
            t.motivo_anulacion, t.operador_entrada_nombre_snapshot AS operador
     FROM ticket t
     WHERE t.estado = ''ANULADO''
       AND t.anulado_en BETWEEN :desde AND :hasta
       AND t.parqueadero_id = :parqueaderoId
     ORDER BY t.anulado_en DESC',
    '[{"clave":"parqueaderoId","tipo":"long","requerido":true},{"clave":"desde","tipo":"timestamp","requerido":true},{"clave":"hasta","tipo":"timestamp","requerido":true}]',
    '[{"clave":"id","tipo":"long"},{"clave":"placa","tipo":"string"},{"clave":"fecha_hora_entrada","tipo":"timestamp"},{"clave":"anulado_en","tipo":"timestamp"},{"clave":"motivo_anulacion","tipo":"string"},{"clave":"operador","tipo":"string"}]',
    'ADMIN,SUPER_ADMIN,ADMIN_PARQUEADERO'),

(CAST(NULL AS BIGINT), 'resoluciones_dian_agotandose',
    'Resoluciones DIAN agotandose',
    'Resoluciones vigentes con consecutivo cercano al rango_final.',
    'SELECT rd.id, rd.numero_resolucion, rd.consecutivo_actual, rd.rango_final,
            (rd.rango_final - rd.consecutivo_actual) AS disponibles,
            rd.vigente_hasta
     FROM resolucion_dian rd
     WHERE rd.archivada_en IS NULL
       AND (rd.rango_final - rd.consecutivo_actual) < :umbral
     ORDER BY disponibles ASC',
    '[{"clave":"umbral","tipo":"integer","default":100}]',
    '[{"clave":"id","tipo":"long"},{"clave":"numero_resolucion","tipo":"string"},{"clave":"consecutivo_actual","tipo":"long"},{"clave":"rango_final","tipo":"long"},{"clave":"disponibles","tipo":"long"},{"clave":"vigente_hasta","tipo":"date"}]',
    'ADMIN,SUPER_ADMIN'),

(CAST(NULL AS BIGINT), 'convenios_uso_mensual',
    'Uso de convenios mensual',
    'Conteo de validaciones de convenio por mes.',
    'SELECT c.nombre_comercio, DATE_TRUNC(''month'', vc.fecha_hora) AS mes,
            COUNT(*) AS validaciones, SUM(vc.descuento_aplicado) AS descuento_total
     FROM validacion_compra vc
     JOIN convenio c ON vc.convenio_id = c.id
     WHERE vc.fecha_hora BETWEEN :desde AND :hasta
     GROUP BY c.nombre_comercio, DATE_TRUNC(''month'', vc.fecha_hora)
     ORDER BY mes DESC, validaciones DESC',
    '[{"clave":"desde","tipo":"timestamp","requerido":true},{"clave":"hasta","tipo":"timestamp","requerido":true}]',
    '[{"clave":"nombre_comercio","tipo":"string"},{"clave":"mes","tipo":"date"},{"clave":"validaciones","tipo":"long"},{"clave":"descuento_total","tipo":"decimal"}]',
    'ADMIN,SUPER_ADMIN,ADMIN_PARQUEADERO'),

(CAST(NULL AS BIGINT), 'rotacion_promedio',
    'Rotacion promedio por punto',
    'Promedio de tickets cerrados por punto en el periodo.',
    'SELECT pp.nombre AS punto, COUNT(t.id) AS tickets,
            AVG(EXTRACT(EPOCH FROM (t.fecha_hora_salida - t.fecha_hora_entrada))/60)::integer AS minutos_promedio
     FROM punto_parqueo pp
     LEFT JOIN ticket t ON t.punto_parqueo_id = pp.id
       AND t.estado = ''CERRADO''
       AND t.fecha_hora_entrada BETWEEN :desde AND :hasta
     WHERE pp.id IN (SELECT id FROM punto_parqueo WHERE archivado_en IS NULL)
     GROUP BY pp.nombre
     ORDER BY tickets DESC
     LIMIT :limite',
    '[{"clave":"desde","tipo":"timestamp","requerido":true},{"clave":"hasta","tipo":"timestamp","requerido":true},{"clave":"limite","tipo":"integer","default":50}]',
    '[{"clave":"punto","tipo":"string"},{"clave":"tickets","tipo":"long"},{"clave":"minutos_promedio","tipo":"integer"}]',
    'ADMIN,SUPER_ADMIN,ADMIN_PARQUEADERO')

) AS r(empresa_id, clave, nombre, descripcion, sql_template, filtros_json, columnas_json, roles_permitidos)
WHERE NOT EXISTS (
    SELECT 1 FROM reporte_definicion rd WHERE rd.empresa_id IS NULL AND rd.clave = r.clave
);

-- ════════════════════════════════════════════════════════════════
-- v49 COMPLETAR — Bloque C: Fase 5 entities pobres restantes
-- ════════════════════════════════════════════════════════════════

-- Suscripcion (16 → 22 cols): contrato + auto-renovacion + descuento
ALTER TABLE suscripcion ADD COLUMN IF NOT EXISTS numero_contrato                       VARCHAR(100);
ALTER TABLE suscripcion ADD COLUMN IF NOT EXISTS archivo_contrato_url                  VARCHAR(500);
ALTER TABLE suscripcion ADD COLUMN IF NOT EXISTS motivo_cancelacion                    TEXT;
ALTER TABLE suscripcion ADD COLUMN IF NOT EXISTS auto_renovar                          BOOLEAN DEFAULT FALSE;
ALTER TABLE suscripcion ADD COLUMN IF NOT EXISTS notificar_proximidad_vencimiento      BOOLEAN DEFAULT TRUE;
ALTER TABLE suscripcion ADD COLUMN IF NOT EXISTS descuento_pronto_pago_porcentaje      NUMERIC(5,2);

-- Tarifa (21 → 26 cols): franjas horarias + dias aplicables + unidad FK
ALTER TABLE tarifa ADD COLUMN IF NOT EXISTS descripcion             TEXT;
ALTER TABLE tarifa ADD COLUMN IF NOT EXISTS dias_semana_aplica      INTEGER;
ALTER TABLE tarifa ADD COLUMN IF NOT EXISTS hora_inicio_aplica      TIME;
ALTER TABLE tarifa ADD COLUMN IF NOT EXISTS hora_fin_aplica         TIME;
ALTER TABLE tarifa ADD COLUMN IF NOT EXISTS usar_franjas            BOOLEAN DEFAULT FALSE;
ALTER TABLE tarifa ADD COLUMN IF NOT EXISTS unidad_id               BIGINT REFERENCES unidad_tarifa(id);

-- Ticket (21 → 23 cols): estado FK + observaciones
ALTER TABLE ticket ADD COLUMN IF NOT EXISTS observaciones           TEXT;
ALTER TABLE ticket ADD COLUMN IF NOT EXISTS estado_ticket_id        BIGINT REFERENCES estado_ticket(id);

-- Persona (8 → 14+): completar cols faltantes del plan
ALTER TABLE persona ADD COLUMN IF NOT EXISTS digito_verificacion    VARCHAR(2);
ALTER TABLE persona ADD COLUMN IF NOT EXISTS email_secundario       VARCHAR(200);
ALTER TABLE persona ADD COLUMN IF NOT EXISTS telefono_secundario    VARCHAR(20);
ALTER TABLE persona ADD COLUMN IF NOT EXISTS nacionalidad_pais_id   BIGINT REFERENCES pais(id);
ALTER TABLE persona ADD COLUMN IF NOT EXISTS estado_civil_id        BIGINT REFERENCES estado_civil(id);
ALTER TABLE persona ADD COLUMN IF NOT EXISTS foto_url               VARCHAR(500);
ALTER TABLE persona ADD COLUMN IF NOT EXISTS ocupacion              VARCHAR(150);
ALTER TABLE persona ADD COLUMN IF NOT EXISTS observaciones          TEXT;

-- Empresa: cols faltantes del plan
ALTER TABLE empresa ADD COLUMN IF NOT EXISTS digito_verificacion           VARCHAR(2);
ALTER TABLE empresa ADD COLUMN IF NOT EXISTS razon_social                  VARCHAR(300);
ALTER TABLE empresa ADD COLUMN IF NOT EXISTS nombre_comercial              VARCHAR(200);
ALTER TABLE empresa ADD COLUMN IF NOT EXISTS representante_legal_persona_id BIGINT REFERENCES persona(id);
ALTER TABLE empresa ADD COLUMN IF NOT EXISTS fecha_constitucion            DATE;
ALTER TABLE empresa ADD COLUMN IF NOT EXISTS zona_horaria_id               BIGINT REFERENCES zona_horaria(id);
ALTER TABLE empresa ADD COLUMN IF NOT EXISTS email_facturacion             VARCHAR(200);

-- Vehiculo: cols faltantes del plan
ALTER TABLE vehiculo ADD COLUMN IF NOT EXISTS linea                  VARCHAR(100);
ALTER TABLE vehiculo ADD COLUMN IF NOT EXISTS cilindraje             INTEGER;
ALTER TABLE vehiculo ADD COLUMN IF NOT EXISTS kilometraje_ultimo     INTEGER;
ALTER TABLE vehiculo ADD COLUMN IF NOT EXISTS numero_chasis          VARCHAR(50);
ALTER TABLE vehiculo ADD COLUMN IF NOT EXISTS numero_motor           VARCHAR(50);
ALTER TABLE vehiculo ADD COLUMN IF NOT EXISTS placa_pais_id          BIGINT REFERENCES pais_codigo_placa(id);
ALTER TABLE vehiculo ADD COLUMN IF NOT EXISTS tipo_servicio_id       BIGINT REFERENCES tipo_servicio_vehiculo(id);
ALTER TABLE vehiculo ADD COLUMN IF NOT EXISTS color_pintura          VARCHAR(50);

-- Reserva: cols faltantes del plan
ALTER TABLE reserva ADD COLUMN IF NOT EXISTS monto_estimado                NUMERIC(12,2);
ALTER TABLE reserva ADD COLUMN IF NOT EXISTS monto_anticipo_pagado         NUMERIC(12,2);
ALTER TABLE reserva ADD COLUMN IF NOT EXISTS pago_anticipo_id              BIGINT REFERENCES pago(id);
ALTER TABLE reserva ADD COLUMN IF NOT EXISTS codigo_confirmacion           VARCHAR(50);
ALTER TABLE reserva ADD COLUMN IF NOT EXISTS hora_real_llegada             TIMESTAMP;
ALTER TABLE reserva ADD COLUMN IF NOT EXISTS monto_penalizacion_no_show    NUMERIC(12,2);
ALTER TABLE reserva ADD COLUMN IF NOT EXISTS canal_origen_id               BIGINT REFERENCES canal_origen_reserva(id);
ALTER TABLE reserva ADD COLUMN IF NOT EXISTS notificaciones_enabled        BOOLEAN DEFAULT TRUE;
ALTER TABLE reserva ADD COLUMN IF NOT EXISTS recordatorios_enviados        INTEGER DEFAULT 0;

-- Camara: cols faltantes del plan
ALTER TABLE camara ADD COLUMN IF NOT EXISTS numero_serie           VARCHAR(100);
ALTER TABLE camara ADD COLUMN IF NOT EXISTS fecha_instalacion      DATE;
ALTER TABLE camara ADD COLUMN IF NOT EXISTS fecha_ultima_revision  DATE;
ALTER TABLE camara ADD COLUMN IF NOT EXISTS tipo_lente             VARCHAR(50);
ALTER TABLE camara ADD COLUMN IF NOT EXISTS proteccion_ip          VARCHAR(10);
ALTER TABLE camara ADD COLUMN IF NOT EXISTS ubicacion_descripcion  TEXT;
ALTER TABLE camara ADD COLUMN IF NOT EXISTS url_stream_rtsp        VARCHAR(500);
ALTER TABLE camara ADD COLUMN IF NOT EXISTS url_stream_http        VARCHAR(500);
ALTER TABLE camara ADD COLUMN IF NOT EXISTS usuario_acceso         VARCHAR(100);
ALTER TABLE camara ADD COLUMN IF NOT EXISTS password_acceso_cifrado VARCHAR(500);
ALTER TABLE camara ADD COLUMN IF NOT EXISTS observaciones          TEXT;

-- Dispositivo: cols faltantes
ALTER TABLE dispositivo ADD COLUMN IF NOT EXISTS mac_address              VARCHAR(17);
ALTER TABLE dispositivo ADD COLUMN IF NOT EXISTS ip_local                 VARCHAR(45);
ALTER TABLE dispositivo ADD COLUMN IF NOT EXISTS puerto                   INTEGER;
ALTER TABLE dispositivo ADD COLUMN IF NOT EXISTS fecha_instalacion        DATE;
ALTER TABLE dispositivo ADD COLUMN IF NOT EXISTS fecha_ultima_revision    DATE;
ALTER TABLE dispositivo ADD COLUMN IF NOT EXISTS protocolo_comunicacion   VARCHAR(50);
ALTER TABLE dispositivo ADD COLUMN IF NOT EXISTS tipo_acceso_id           BIGINT REFERENCES tipo_acceso_dispositivo(id);

-- Pago: cols faltantes del plan
ALTER TABLE pago ADD COLUMN IF NOT EXISTS entidad_emisora           VARCHAR(100);
ALTER TABLE pago ADD COLUMN IF NOT EXISTS ultimos_4_digitos_tarjeta VARCHAR(4);
ALTER TABLE pago ADD COLUMN IF NOT EXISTS codigo_autorizacion       VARCHAR(50);
ALTER TABLE pago ADD COLUMN IF NOT EXISTS archivo_comprobante_url   VARCHAR(500);

-- Factura: cols faltantes del plan
ALTER TABLE factura ADD COLUMN IF NOT EXISTS numero_factura         VARCHAR(50);
ALTER TABLE factura ADD COLUMN IF NOT EXISTS prefijo                VARCHAR(20);
ALTER TABLE factura ADD COLUMN IF NOT EXISTS consecutivo            BIGINT;
ALTER TABLE factura ADD COLUMN IF NOT EXISTS cufe                   VARCHAR(200);
ALTER TABLE factura ADD COLUMN IF NOT EXISTS subtotal               NUMERIC(14,2);
ALTER TABLE factura ADD COLUMN IF NOT EXISTS descuento_aplicado     NUMERIC(14,2);
ALTER TABLE factura ADD COLUMN IF NOT EXISTS valor_total_letras     TEXT;
ALTER TABLE factura ADD COLUMN IF NOT EXISTS email_destinatario     VARCHAR(200);
ALTER TABLE factura ADD COLUMN IF NOT EXISTS fecha_envio_email      TIMESTAMP;
ALTER TABLE factura ADD COLUMN IF NOT EXISTS archivo_pdf_url        VARCHAR(500);
CREATE UNIQUE INDEX IF NOT EXISTS uq_factura_numero ON factura(numero_factura) WHERE numero_factura IS NOT NULL;

-- PuntoParqueo: cols faltantes
ALTER TABLE punto_parqueo ADD COLUMN IF NOT EXISTS numero_visible          VARCHAR(20);
ALTER TABLE punto_parqueo ADD COLUMN IF NOT EXISTS coordenadas_layout_x    NUMERIC(10,2);
ALTER TABLE punto_parqueo ADD COLUMN IF NOT EXISTS coordenadas_layout_y    NUMERIC(10,2);
ALTER TABLE punto_parqueo ADD COLUMN IF NOT EXISTS ancho_metros            NUMERIC(5,2);
ALTER TABLE punto_parqueo ADD COLUMN IF NOT EXISTS largo_metros            NUMERIC(5,2);
ALTER TABLE punto_parqueo ADD COLUMN IF NOT EXISTS con_techo               BOOLEAN DEFAULT FALSE;
ALTER TABLE punto_parqueo ADD COLUMN IF NOT EXISTS con_carga_electrica     BOOLEAN DEFAULT FALSE;
ALTER TABLE punto_parqueo ADD COLUMN IF NOT EXISTS cerca_de_acceso         BOOLEAN DEFAULT FALSE;
ALTER TABLE punto_parqueo ADD COLUMN IF NOT EXISTS para_discapacitados     BOOLEAN DEFAULT FALSE;

-- ════════════════════════════════════════════════════════════════
-- v50 Sprint 1: Catalogos globales parametrizables por empresa
--
-- Agrega empresa_id NULLABLE a los 11 catalogos globales:
--   - empresa_id IS NULL  → item canonico global (visible para todas)
--   - empresa_id IS NOT NULL → item custom de esa empresa
--
-- Y crea tabla puente empresa_catalogo_global_activo para que cada
-- empresa decida cuales globales acepta. Si una empresa no tiene
-- filas para un catalogo → acepta TODOS los globales (retrocompat).
-- ════════════════════════════════════════════════════════════════

ALTER TABLE tipo_documento          ADD COLUMN IF NOT EXISTS empresa_id BIGINT REFERENCES empresa(id) ON DELETE CASCADE;
ALTER TABLE genero                  ADD COLUMN IF NOT EXISTS empresa_id BIGINT REFERENCES empresa(id) ON DELETE CASCADE;
ALTER TABLE moneda                  ADD COLUMN IF NOT EXISTS empresa_id BIGINT REFERENCES empresa(id) ON DELETE CASCADE;
ALTER TABLE zona_horaria            ADD COLUMN IF NOT EXISTS empresa_id BIGINT REFERENCES empresa(id) ON DELETE CASCADE;
ALTER TABLE unidad_tarifa           ADD COLUMN IF NOT EXISTS empresa_id BIGINT REFERENCES empresa(id) ON DELETE CASCADE;
ALTER TABLE regimen_tributario      ADD COLUMN IF NOT EXISTS empresa_id BIGINT REFERENCES empresa(id) ON DELETE CASCADE;
ALTER TABLE estado_civil            ADD COLUMN IF NOT EXISTS empresa_id BIGINT REFERENCES empresa(id) ON DELETE CASCADE;
ALTER TABLE pais_codigo_placa       ADD COLUMN IF NOT EXISTS empresa_id BIGINT REFERENCES empresa(id) ON DELETE CASCADE;
ALTER TABLE tipo_servicio_vehiculo  ADD COLUMN IF NOT EXISTS empresa_id BIGINT REFERENCES empresa(id) ON DELETE CASCADE;
ALTER TABLE tipo_acceso_dispositivo ADD COLUMN IF NOT EXISTS empresa_id BIGINT REFERENCES empresa(id) ON DELETE CASCADE;
ALTER TABLE canal_origen_reserva    ADD COLUMN IF NOT EXISTS empresa_id BIGINT REFERENCES empresa(id) ON DELETE CASCADE;

-- Indices por empresa_id (perf de queries del resolver)
CREATE INDEX IF NOT EXISTS idx_tipo_documento_empresa          ON tipo_documento(empresa_id);
CREATE INDEX IF NOT EXISTS idx_genero_empresa                  ON genero(empresa_id);
CREATE INDEX IF NOT EXISTS idx_moneda_empresa                  ON moneda(empresa_id);
CREATE INDEX IF NOT EXISTS idx_zona_horaria_empresa            ON zona_horaria(empresa_id);
CREATE INDEX IF NOT EXISTS idx_unidad_tarifa_empresa           ON unidad_tarifa(empresa_id);
CREATE INDEX IF NOT EXISTS idx_regimen_tributario_empresa      ON regimen_tributario(empresa_id);
CREATE INDEX IF NOT EXISTS idx_estado_civil_empresa            ON estado_civil(empresa_id);
CREATE INDEX IF NOT EXISTS idx_pais_codigo_placa_empresa       ON pais_codigo_placa(empresa_id);
CREATE INDEX IF NOT EXISTS idx_tipo_servicio_vehiculo_empresa  ON tipo_servicio_vehiculo(empresa_id);
CREATE INDEX IF NOT EXISTS idx_tipo_acceso_dispositivo_empresa ON tipo_acceso_dispositivo(empresa_id);
CREATE INDEX IF NOT EXISTS idx_canal_origen_reserva_empresa    ON canal_origen_reserva(empresa_id);

-- Tabla puente: cada empresa marca cuales globales acepta
CREATE TABLE IF NOT EXISTS empresa_catalogo_global_activo (
    id BIGSERIAL PRIMARY KEY,
    empresa_id BIGINT NOT NULL REFERENCES empresa(id) ON DELETE CASCADE,
    catalogo   VARCHAR(80)  NOT NULL,
    item_id    BIGINT       NOT NULL,
    activo     BOOLEAN      NOT NULL DEFAULT TRUE,
    fecha_creacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_actualizacion TIMESTAMP,
    actualizado_por_usuario_id BIGINT,
    CONSTRAINT uq_emp_cat_global UNIQUE (empresa_id, catalogo, item_id)
);
CREATE INDEX IF NOT EXISTS idx_emp_cat_global_empresa ON empresa_catalogo_global_activo (empresa_id, catalogo);
ALTER TABLE empresa_catalogo_global_activo ALTER COLUMN fecha_creacion SET DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE empresa_catalogo_global_activo ALTER COLUMN activo SET DEFAULT TRUE;
UPDATE empresa_catalogo_global_activo SET fecha_creacion = CURRENT_TIMESTAMP WHERE fecha_creacion IS NULL;
UPDATE empresa_catalogo_global_activo SET activo = TRUE WHERE activo IS NULL;
