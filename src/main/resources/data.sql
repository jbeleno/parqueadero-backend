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
