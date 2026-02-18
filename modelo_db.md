# Modelo de datos – Sistema de Parqueaderos

## 1. Catálogos generales

### Tabla: estado
- id (PK)
- nombre
- descripcion

### Tabla: tipo_parqueadero
- id (PK)
- nombre
- descripcion
- estado_id (FK → estado.id)

### Tabla: tipo_vehiculo
- id (PK)
- nombre
- descripcion

### Tabla: tipo_punto_parqueo
- id (PK)
- nombre
- descripcion
- estado_id (FK → estado.id)

### Tabla: rol
- id (PK)
- nombre
- descripcion
- estado_id (FK → estado.id)

### Tabla: tipo_dispositivo
- id (PK)
- nombre
- descripcion
- estado_id (FK → estado.id)


## 2. Ubicación geográfica

### Tabla: pais
- id (PK)
- nombre
- acronimo
- identificador_internacional

### Tabla: departamento
- id (PK)
- pais_id (FK → pais.id)
- nombre
- identificador_nacional

### Tabla: ciudad
- id (PK)
- departamento_id (FK → departamento.id)
- nombre
- identificador_departamental


## 3. Empresa y parqueadero

### Tabla: empresa
- id (PK)
- nombre
- descripcion
- estado_id (FK → estado.id)

### Tabla: parqueadero
- id (PK)
- ciudad_id (FK → ciudad.id)
- empresa_id (FK → empresa.id)
- tipo_parqueadero_id (FK → tipo_parqueadero.id)
- estado_id (FK → estado.id)
- nombre
- direccion
- telefono
- latitud
- longitud
- altitud
- hora_inicio
- hora_fin
- numero_puntos_parqueo
- zona_horaria
- tiempo_gracia_minutos
- modo_cobro   // POR_HORA, POR_FRACCION, POR_DIA, PLANA, etc.


## 4. Estructura física del parqueadero

> Nota: se asume PostgreSQL + PostGIS, con tipos `geometry(Polygon, 4326)` y `geometry(Point, 4326)`.

### Tabla: nivel
- id (PK)
- parqueadero_id (FK → parqueadero.id)
- nombre
- estado_id (FK → estado.id)

### Tabla: seccion
- id (PK)
- parqueadero_id (FK → parqueadero.id)
- nivel_id (FK → nivel.id)
- nombre
- acronimo
- estado_id (FK → estado.id)
- poligono           // geometry(Polygon, 4326)
- coordenada_centro  // geometry(Point, 4326)
- descripcion

### Tabla: sub_seccion
- id (PK)
- seccion_id (FK → seccion.id)
- nombre
- acronimo
- descripcion
- estado_id (FK → estado.id)
- poligono           // geometry(Polygon, 4326)
- coordenada_centro  // geometry(Point, 4326)

### Tabla: punto_parqueo
- id (PK)
- sub_seccion_id (FK → sub_seccion.id)
- nombre
- acronimo
- descripcion
- tipo_punto_parqueo_id (FK → tipo_punto_parqueo.id)
- estado_id (FK → estado.id)
- poligono    // geometry(Polygon, 4326) – opcional si dibujas toda el área
- coordenada  // geometry(Point, 4326) – centro del puesto


## 5. Personas, usuarios y vehículos

### Tabla: persona
- id (PK)
- nombre
- apellido
- telefono
- tipo_documento
- numero_documento

### Tabla: usuario
- id (PK)
- correo
- password_hash
- estado_id (FK → estado.id)
- persona_id (FK → persona.id)
- empresa_id (FK → empresa.id)   // nullable para usuario final
- fecha_creacion

### Tabla: usuario_rol
- id (PK)
- usuario_id (FK → usuario.id)
- rol_id (FK → rol.id)

### Tabla: vehiculo
- id (PK)
- persona_id (FK → persona.id)
- placa
- color
- tipo_vehiculo_id (FK → tipo_vehiculo.id)


## 6. Tarifas y operación

### Tabla: tarifa
- id (PK)
- parqueadero_id (FK → parqueadero.id)
- nombre
- tipo_vehiculo_id (FK → tipo_vehiculo.id)
- valor
- unidad               // POR_HORA, POR_FRACCION, POR_DIA, PLANA
- minutos_fraccion     // si aplica
- fecha_inicio_vigencia
- fecha_fin_vigencia

### Tabla: ticket
- id (PK)
- parqueadero_id (FK → parqueadero.id)
- punto_parqueo_id (FK → punto_parqueo.id)
- vehiculo_id (FK → vehiculo.id)
- tarifa_id (FK → tarifa.id)
- fecha_hora_entrada
- fecha_hora_salida    // nullable mientras está en curso
- estado               // EN_CURSO, CERRADO, ANULADO
- monto_calculado

### Tabla: reserva   // opcional pero recomendada
- id (PK)
- parqueadero_id (FK → parqueadero.id)
- punto_parqueo_id (FK → punto_parqueo.id)  // nullable si asignas luego
- usuario_id (FK → usuario.id)
- vehiculo_id (FK → vehiculo.id)
- fecha_hora_inicio
- fecha_hora_fin
- estado   // PENDIENTE, CONFIRMADA, CANCELADA, EXPIRADA


## 7. Facturación y pagos

### Tabla: factura
- id (PK)
- ticket_id (FK → ticket.id)
- parqueadero_id (FK → parqueadero.id)
- vehiculo_id (FK → vehiculo.id)
- fecha_hora
- valor_total
- estado   // PENDIENTE, PAGADA, ANULADA

### Tabla: pago
- id (PK)
- factura_id (FK → factura.id)
- monto
- metodo   // EFECTIVO, TARJETA, APP, etc.
- fecha_hora
- estado   // PENDIENTE, COMPLETADO, FALLIDO


## 8. Dispositivos y sensores

### Tabla: dispositivo
- id (PK)
- nombre
- tipo_dispositivo_id (FK → tipo_dispositivo.id)
- estado_id (FK → estado.id)
- sub_seccion_id (FK → sub_seccion.id)
- parqueadero_id (FK → parqueadero.id)
- url_endpoint
- coordenada   // geometry(Point, 4326)

### Tabla: dispositivo_parqueo
- id (PK)
- dispositivo_id (FK → dispositivo.id)
- punto_parqueo_id (FK → punto_parqueo.id)
