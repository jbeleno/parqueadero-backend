# API Endpoints - Sistema de Parqueaderos

**Base URL:** `http://localhost:8080/api`

---

## 🏥 Health Check

### Verificar estado de la API
```http
GET /api/health
```

**Response 200 OK:**
```json
{
  "status": "UP",
  "timestamp": "2026-02-02T16:00:00",
  "message": "API de Parqueaderos funcionando correctamente"
}
```

---

## 📊 Estados

### Listar todos los estados
```http
GET /api/estados
```

**Response 200 OK:**
```json
[
  {
    "id": 1,
    "nombre": "ACTIVO",
    "descripcion": "Estado activo"
  },
  {
    "id": 2,
    "nombre": "INACTIVO",
    "descripcion": "Estado inactivo"
  }
]
```

### Obtener estado por ID
```http
GET /api/estados/{id}
```

**Response 200 OK:**
```json
{
  "id": 1,
  "nombre": "ACTIVO",
  "descripcion": "Estado activo"
}
```

### Crear estado
```http
POST /api/estados
Content-Type: application/json
```

**Request Body:**
```json
{
  "nombre": "ACTIVO",
  "descripcion": "Estado activo"
}
```

**Response 201 Created:**
```json
{
  "id": 1,
  "nombre": "ACTIVO",
  "descripcion": "Estado activo"
}
```

### Actualizar estado
```http
PUT /api/estados/{id}
Content-Type: application/json
```

**Request Body:**
```json
{
  "nombre": "ACTIVO",
  "descripcion": "Estado activo modificado"
}
```

**Response 200 OK:**
```json
{
  "id": 1,
  "nombre": "ACTIVO",
  "descripcion": "Estado activo modificado"
}
```

### Eliminar estado
```http
DELETE /api/estados/{id}
```

**Response 204 No Content**

---

## 🏢 Parqueaderos

### Listar todos los parqueaderos
```http
GET /api/parqueaderos
```

**Response 200 OK:**
```json
[
  {
    "id": 1,
    "nombre": "Parqueadero Centro",
    "direccion": "Calle 10 # 5-20",
    "telefono": "3001234567",
    "latitud": 2.9273,
    "longitud": -75.2819,
    "horaInicio": "06:00",
    "horaFin": "22:00",
    "numeroPuntosParqueo": 100,
    "tiempoGraciaMinutos": 15,
    "modoCobro": "POR_HORA",
    "ciudadId": 1,
    "ciudadNombre": "Neiva",
    "empresaId": 1,
    "empresaNombre": "Parqueaderos SA",
    "tipoParqueaderoId": 1,
    "tipoParqueaderoNombre": "Cubierto",
    "estadoId": 1,
    "estadoNombre": "ACTIVO"
  }
]
```

### Obtener parqueadero por ID
```http
GET /api/parqueaderos/{id}
```

**Response 200 OK:**
```json
{
  "id": 1,
  "nombre": "Parqueadero Centro",
  "direccion": "Calle 10 # 5-20",
  "telefono": "3001234567",
  "latitud": 2.9273,
  "longitud": -75.2819,
  "horaInicio": "06:00",
  "horaFin": "22:00",
  "numeroPuntosParqueo": 100,
  "tiempoGraciaMinutos": 15,
  "modoCobro": "POR_HORA",
  "ciudadId": 1,
  "ciudadNombre": "Neiva",
  "empresaId": 1,
  "empresaNombre": "Parqueaderos SA",
  "tipoParqueaderoId": 1,
  "tipoParqueaderoNombre": "Cubierto",
  "estadoId": 1,
  "estadoNombre": "ACTIVO"
}
```

### Crear parqueadero
```http
POST /api/parqueaderos
Content-Type: application/json
```

**Request Body:**
```json
{
  "nombre": "Parqueadero Centro",
  "direccion": "Calle 10 # 5-20",
  "telefono": "3001234567",
  "latitud": 2.9273,
  "longitud": -75.2819,
  "horaInicio": "06:00",
  "horaFin": "22:00",
  "numeroPuntosParqueo": 100,
  "tiempoGraciaMinutos": 15,
  "modoCobro": "POR_HORA",
  "ciudadId": 1,
  "empresaId": 1,
  "tipoParqueaderoId": 1,
  "estadoId": 1
}
```

**Response 201 Created:**
```json
{
  "id": 1,
  "nombre": "Parqueadero Centro",
  "direccion": "Calle 10 # 5-20",
  "telefono": "3001234567",
  "latitud": 2.9273,
  "longitud": -75.2819,
  "horaInicio": "06:00",
  "horaFin": "22:00",
  "numeroPuntosParqueo": 100,
  "tiempoGraciaMinutos": 15,
  "modoCobro": "POR_HORA",
  "ciudadId": 1,
  "ciudadNombre": "Neiva",
  "empresaId": 1,
  "empresaNombre": "Parqueaderos SA",
  "tipoParqueaderoId": 1,
  "tipoParqueaderoNombre": "Cubierto",
  "estadoId": 1,
  "estadoNombre": "ACTIVO"
}
```

### Actualizar parqueadero
```http
PUT /api/parqueaderos/{id}
Content-Type: application/json
```

**Request Body:**
```json
{
  "nombre": "Parqueadero Centro Actualizado",
  "direccion": "Calle 10 # 5-20",
  "telefono": "3001234567",
  "latitud": 2.9273,
  "longitud": -75.2819,
  "horaInicio": "05:00",
  "horaFin": "23:00",
  "numeroPuntosParqueo": 120,
  "tiempoGraciaMinutos": 10,
  "modoCobro": "POR_FRACCION"
}
```

**Response 200 OK:**
```json
{
  "id": 1,
  "nombre": "Parqueadero Centro Actualizado",
  "direccion": "Calle 10 # 5-20",
  "telefono": "3001234567",
  "latitud": 2.9273,
  "longitud": -75.2819,
  "horaInicio": "05:00",
  "horaFin": "23:00",
  "numeroPuntosParqueo": 120,
  "tiempoGraciaMinutos": 10,
  "modoCobro": "POR_FRACCION",
  "ciudadId": 1,
  "ciudadNombre": "Neiva",
  "empresaId": 1,
  "empresaNombre": "Parqueaderos SA",
  "tipoParqueaderoId": 1,
  "tipoParqueaderoNombre": "Cubierto",
  "estadoId": 1,
  "estadoNombre": "ACTIVO"
}
```

### Eliminar parqueadero
```http
DELETE /api/parqueaderos/{id}
```

**Response 204 No Content**

---

## 💰 Tarifas

### Listar todas las tarifas
```http
GET /api/tarifas
```

**Response 200 OK:**
```json
[
  {
    "id": 1,
    "nombre": "Tarifa Carro Día",
    "valor": 5000.0,
    "unidad": "POR_HORA",
    "minutosFraccion": null,
    "fechaInicioVigencia": "2026-01-01",
    "fechaFinVigencia": "2026-12-31",
    "parqueaderoId": 1,
    "parqueaderoNombre": "Parqueadero Centro",
    "tipoVehiculoId": 1,
    "tipoVehiculoNombre": "Automóvil"
  },
  {
    "id": 2,
    "nombre": "Tarifa Moto Día",
    "valor": 2000.0,
    "unidad": "POR_HORA",
    "minutosFraccion": null,
    "fechaInicioVigencia": "2026-01-01",
    "fechaFinVigencia": "2026-12-31",
    "parqueaderoId": 1,
    "parqueaderoNombre": "Parqueadero Centro",
    "tipoVehiculoId": 2,
    "tipoVehiculoNombre": "Motocicleta"
  }
]
```

### Obtener tarifa por ID
```http
GET /api/tarifas/{id}
```

**Response 200 OK:**
```json
{
  "id": 1,
  "nombre": "Tarifa Carro Día",
  "valor": 5000.0,
  "unidad": "POR_HORA",
  "minutosFraccion": null,
  "fechaInicioVigencia": "2026-01-01",
  "fechaFinVigencia": "2026-12-31",
  "parqueaderoId": 1,
  "parqueaderoNombre": "Parqueadero Centro",
  "tipoVehiculoId": 1,
  "tipoVehiculoNombre": "Automóvil"
}
```

### Crear tarifa
```http
POST /api/tarifas
Content-Type: application/json
```

**Request Body:**
```json
{
  "nombre": "Tarifa Carro Día",
  "valor": 5000.0,
  "unidad": "POR_HORA",
  "minutosFraccion": null,
  "fechaInicioVigencia": "2026-01-01",
  "fechaFinVigencia": "2026-12-31",
  "parqueaderoId": 1,
  "tipoVehiculoId": 1
}
```

**Response 201 Created:**
```json
{
  "id": 1,
  "nombre": "Tarifa Carro Día",
  "valor": 5000.0,
  "unidad": "POR_HORA",
  "minutosFraccion": null,
  "fechaInicioVigencia": "2026-01-01",
  "fechaFinVigencia": "2026-12-31",
  "parqueaderoId": 1,
  "parqueaderoNombre": "Parqueadero Centro",
  "tipoVehiculoId": 1,
  "tipoVehiculoNombre": "Automóvil"
}
```

### Actualizar tarifa
```http
PUT /api/tarifas/{id}
Content-Type: application/json
```

**Request Body:**
```json
{
  "nombre": "Tarifa Carro Día Actualizada",
  "valor": 6000.0,
  "unidad": "POR_FRACCION",
  "minutosFraccion": 30,
  "fechaInicioVigencia": "2026-01-01",
  "fechaFinVigencia": "2026-12-31"
}
```

**Response 200 OK:**
```json
{
  "id": 1,
  "nombre": "Tarifa Carro Día Actualizada",
  "valor": 6000.0,
  "unidad": "POR_FRACCION",
  "minutosFraccion": 30,
  "fechaInicioVigencia": "2026-01-01",
  "fechaFinVigencia": "2026-12-31",
  "parqueaderoId": 1,
  "parqueaderoNombre": "Parqueadero Centro",
  "tipoVehiculoId": 1,
  "tipoVehiculoNombre": "Automóvil"
}
```

### Eliminar tarifa
```http
DELETE /api/tarifas/{id}
```

**Response 204 No Content**

---

## 🏗️ Niveles

### Listar todos los niveles
```http
GET /api/niveles
```

**Response 200 OK:**
```json
[
  {
    "id": 1,
    "nombre": "Nivel 1",
    "parqueadero": {
      "id": 1,
      "nombre": "Parqueadero Centro"
    },
    "estado": {
      "id": 1,
      "nombre": "ACTIVO"
    }
  },
  {
    "id": 2,
    "nombre": "Nivel 2",
    "parqueadero": {
      "id": 1,
      "nombre": "Parqueadero Centro"
    },
    "estado": {
      "id": 1,
      "nombre": "ACTIVO"
    }
  }
]
```

### Obtener nivel por ID
```http
GET /api/niveles/{id}
```

**Response 200 OK:**
```json
{
  "id": 1,
  "nombre": "Nivel 1",
  "parqueadero": {
    "id": 1,
    "nombre": "Parqueadero Centro"
  },
  "estado": {
    "id": 1,
    "nombre": "ACTIVO"
  }
}
```

### Crear nivel
```http
POST /api/niveles
Content-Type: application/json
```

**Request Body:**
```json
{
  "nombre": "Nivel 1",
  "parqueadero": {
    "id": 1
  },
  "estado": {
    "id": 1
  }
}
```

**Response 201 Created:**
```json
{
  "id": 1,
  "nombre": "Nivel 1",
  "parqueadero": {
    "id": 1,
    "nombre": "Parqueadero Centro"
  },
  "estado": {
    "id": 1,
    "nombre": "ACTIVO"
  }
}
```

### Actualizar nivel
```http
PUT /api/niveles/{id}
Content-Type: application/json
```

**Request Body:**
```json
{
  "nombre": "Nivel 1 - Planta Baja"
}
```

**Response 200 OK:**
```json
{
  "id": 1,
  "nombre": "Nivel 1 - Planta Baja",
  "parqueadero": {
    "id": 1,
    "nombre": "Parqueadero Centro"
  },
  "estado": {
    "id": 1,
    "nombre": "ACTIVO"
  }
}
```

### Eliminar nivel
```http
DELETE /api/niveles/{id}
```

**Response 204 No Content**

---

## ⚠️ Manejo de Errores

### Error 400 - Bad Request
```json
{
  "timestamp": "2026-02-02T16:00:00",
  "message": "Parqueadero no encontrado",
  "status": 400
}
```

### Error 500 - Internal Server Error
```json
{
  "timestamp": "2026-02-02T16:00:00",
  "message": "Error interno del servidor",
  "details": "Detalles del error...",
  "status": 500
}
```

---

## 📝 Notas

- Todos los endpoints tienen **CORS** habilitado (`@CrossOrigin(origins = "*")`)
- Las respuestas exitosas de creación devuelven **HTTP 200 OK** (con el recurso creado en el body)
- Las respuestas exitosas de actualización devuelven **HTTP 200 OK**
- Las respuestas exitosas de eliminación devuelven **HTTP 204 No Content**
- Los errores son manejados por `GlobalExceptionHandler`
- Las fechas se manejan en formato **ISO 8601** (`YYYY-MM-DD`)
- Las horas y fechas/horas se manejan en formato **ISO 8601** (`YYYY-MM-DDTHH:mm:ss`)

### ⚠️ Importante: Estructura de Request/Response

**Los endpoints implementados trabajan directamente con las entidades** (no usan DTOs simplificados). Esto significa que al crear o actualizar recursos, debes enviar la estructura completa de la entidad incluyendo las relaciones.

**Ejemplo - Crear un Ticket:**
```json
{
  "parqueadero": { "id": 1 },
  "puntoParqueo": { "id": 5 },
  "vehiculo": { "id": 3 },
  "tarifa": { "id": 1 },
  "estado": "EN_CURSO"
}
```

**Ejemplo - Crear un Usuario:**
```json
{
  "correo": "usuario@example.com",
  "passwordHash": "$2a$10$...",
  "persona": { "id": 1 },
  "estado": { "id": 1 }
}
```

**Campos String vs Relaciones:**
Algunas entidades usan campos `String` para estados en lugar de relaciones `Entity`:
- `Ticket.estado`: String ("EN_CURSO", "CERRADO", "ANULADO")
- `Factura.estado`: String ("PENDIENTE", "PAGADA", "ANULADA")
- `Pago.estado`: String ("PENDIENTE", "COMPLETADO", "FALLIDO")
- `Reserva.estado`: String ("PENDIENTE", "CONFIRMADA", "CANCELADA", "EXPIRADA")

**Geometrías PostGIS:**
- Los campos de geometría (`Point`, `Polygon`) se serializan/deserializan en formato WKT
- Ejemplo Point: `"POINT(-75.291389 2.927222)"`
- Ejemplo Polygon: `"POLYGON((x1 y1, x2 y2, x3 y3, x1 y1))"`

---

## 6. Países

### GET /api/paises
**Descripción:** Obtener todos los países

**Request:**
```bash
GET http://localhost:8080/api/paises
```

**Response (200 OK):**
```json
[
  {
    "id": 1,
    "nombre": "Colombia",
    "codigo": "COL",
    "codigoIso": "CO"
  }
]
```

### GET /api/paises/{id}
**Descripción:** Obtener país por ID

**Response (200 OK):**
```json
{
  "id": 1,
  "nombre": "Colombia",
  "codigo": "COL",
  "codigoIso": "CO"
}
```

### POST /api/paises
**Descripción:** Crear nuevo país

**Request Body:**
```json
{
  "nombre": "Colombia",
  "codigo": "COL",
  "codigoIso": "CO"
}
```

### PUT /api/paises/{id}
**Descripción:** Actualizar país existente

### DELETE /api/paises/{id}
**Descripción:** Eliminar país

---

## 7. Departamentos

### GET /api/departamentos
**Descripción:** Obtener todos los departamentos

**Response (200 OK):**
```json
[
  {
    "id": 1,
    "nombre": "Huila",
    "codigo": "HUI",
    "pais": {
      "id": 1,
      "nombre": "Colombia"
    }
  }
]
```

### POST /api/departamentos
**Request Body:**
```json
{
  "nombre": "Huila",
  "codigo": "HUI",
  "pais": {
    "id": 1
  }
}
```

---

## 8. Ciudades

### GET /api/ciudades
**Descripción:** Obtener todas las ciudades

**Response (200 OK):**
```json
[
  {
    "id": 1,
    "nombre": "Neiva",
    "codigo": "NEI",
    "departamento": {
      "id": 1,
      "nombre": "Huila"
    }
  }
]
```

### POST /api/ciudades
**Request Body:**
```json
{
  "nombre": "Neiva",
  "codigo": "NEI",
  "departamento": {
    "id": 1
  }
}
```

---

## 9. Empresas

### GET /api/empresas
**Descripción:** Obtener todas las empresas (excluye archivadas)

**Response (200 OK):**
```json
{
  "success": true,
  "data": [
    {
      "id": 1,
      "nombre": "Parqueaderos USCO S.A.S",
      "descripcion": "Empresa de parqueaderos",
      "estadoId": 1,
      "estadoNombre": "ACTIVO"
    }
  ]
}
```

### GET /api/empresas/{id}
**Descripción:** Obtener empresa por ID

### POST /api/empresas
**Request Body:**
```json
{
  "nombre": "Parqueaderos USCO S.A.S",
  "descripcion": "Empresa de parqueaderos",
  "estadoId": 1
}
```

### PUT /api/empresas/{id}
**Descripción:** Actualizar empresa

### PATCH /api/empresas/{id}/archivar
**Descripción:** Soft-delete — archiva la empresa (solo ADMIN/SUPER_ADMIN)
**Headers:** `Authorization: Bearer <token>`

**Response (200 OK):**
```json
{
  "success": true,
  "data": "Empresa archivada correctamente"
}
```

---

## 10. Tipos de Parqueadero

### GET /api/tipos-parqueadero
**Descripción:** Obtener todos los tipos de parqueadero

**Response (200 OK):**
```json
[
  {
    "id": 1,
    "nombre": "Público",
    "descripcion": "Parqueadero de acceso público"
  },
  {
    "id": 2,
    "nombre": "Privado",
    "descripcion": "Parqueadero privado con restricción"
  }
]
```

### POST /api/tipos-parqueadero
**Request Body:**
```json
{
  "nombre": "Público",
  "descripcion": "Parqueadero de acceso público"
}
```

---

## 11. Tipos de Vehículo

### GET /api/tipos-vehiculo
**Descripción:** Obtener todos los tipos de vehículo

**Response (200 OK):**
```json
[
  {
    "id": 1,
    "nombre": "Automóvil",
    "descripcion": "Vehículo de 4 ruedas"
  },
  {
    "id": 2,
    "nombre": "Motocicleta",
    "descripcion": "Vehículo de 2 ruedas"
  },
  {
    "id": 3,
    "nombre": "Bicicleta",
    "descripcion": "Vehículo no motorizado"
  }
]
```

### POST /api/tipos-vehiculo
**Request Body:**
```json
{
  "nombre": "Automóvil",
  "descripcion": "Vehículo de 4 ruedas"
}
```

---

## 12. Tipos de Punto de Parqueo

### GET /api/tipos-punto-parqueo
**Descripción:** Obtener todos los tipos de punto de parqueo

**Response (200 OK):**
```json
[
  {
    "id": 1,
    "nombre": "Regular",
    "descripcion": "Espacio de parqueo estándar"
  },
  {
    "id": 2,
    "nombre": "Preferencial",
    "descripcion": "Espacio para personas con movilidad reducida"
  },
  {
    "id": 3,
    "nombre": "Motocicletas",
    "descripcion": "Espacio exclusivo para motocicletas"
  }
]
```

### POST /api/tipos-punto-parqueo
**Request Body:**
```json
{
  "nombre": "Regular",
  "descripcion": "Espacio de parqueo estándar"
}
```

---

## 13. Secciones

### GET /api/secciones
**Descripción:** Obtener todas las secciones

**Response (200 OK):**
```json
[
  {
    "id": 1,
    "codigo": "SEC-A",
    "nombre": "Sección A",
    "descripcion": "Sección zona norte",
    "nivelId": 1,
    "nivelNombre": "Nivel 1"
  }
]
```

### POST /api/secciones
**Request Body:**
```json
{
  "codigo": "SEC-A",
  "nombre": "Sección A",
  "descripcion": "Sección zona norte",
  "nivelId": 1
}
```

### PUT /api/secciones/{id}
**Request Body:**
```json
{
  "codigo": "SEC-A",
  "nombre": "Sección A Modificada",
  "descripcion": "Nueva descripción",
  "nivelId": 1
}
```

---

## 14. Sub-Secciones

### GET /api/sub-secciones
**Descripción:** Obtener todas las sub-secciones

**Response (200 OK):**
```json
[
  {
    "id": 1,
    "codigo": "SUBSEC-A1",
    "nombre": "Sub-Sección A1",
    "descripcion": "Zona de autos",
    "seccionId": 1,
    "seccionNombre": "Sección A"
  }
]
```

### POST /api/sub-secciones
**Request Body:**
```json
{
  "codigo": "SUBSEC-A1",
  "nombre": "Sub-Sección A1",
  "descripcion": "Zona de autos",
  "seccionId": 1
}
```

---

## 15. Puntos de Parqueo

### GET /api/puntos-parqueo
**Descripción:** Obtener todos los puntos de parqueo

**Response (200 OK):**
```json
[
  {
    "id": 1,
    "codigo": "A1-001",
    "nombre": "Punto A1-001",
    "subSeccionId": 1,
    "subSeccionNombre": "Sub-Sección A1",
    "tipoPuntoParqueoId": 1,
    "tipoPuntoParqueoNombre": "Regular",
    "estadoId": 1,
    "estadoNombre": "Disponible",
    "ubicacionWkt": "POINT(-75.291389 2.927222)"
  }
]
```

### POST /api/puntos-parqueo
**Request Body:**
```json
{
  "codigo": "A1-001",
  "nombre": "Punto A1-001",
  "subSeccionId": 1,
  "tipoPuntoParqueoId": 1,
  "estadoId": 1,
  "ubicacionWkt": "POINT(-75.291389 2.927222)"
}
```

**Nota:** El campo `ubicacionWkt` debe estar en formato WKT (Well-Known Text) para coordenadas geográficas en SRID 4326 (lon lat).

---

## 16. Roles

### GET /api/roles
**Descripción:** Obtener todos los roles

**Response (200 OK):**
```json
[
  {
    "id": 1,
    "nombre": "ADMIN",
    "descripcion": "Administrador del sistema"
  },
  {
    "id": 2,
    "nombre": "OPERADOR",
    "descripcion": "Operador de parqueadero"
  },
  {
    "id": 3,
    "nombre": "CLIENTE",
    "descripcion": "Usuario cliente"
  }
]
```

### POST /api/roles
**Request Body:**
```json
{
  "nombre": "ADMIN",
  "descripcion": "Administrador del sistema"
}
```

---

## 17. Tipos de Dispositivo

### GET /api/tipos-dispositivo
**Descripción:** Obtener todos los tipos de dispositivo

**Response (200 OK):**
```json
[
  {
    "id": 1,
    "nombre": "Sensor de ocupación",
    "descripcion": "Detecta si un espacio está ocupado"
  },
  {
    "id": 2,
    "nombre": "Cámara LPR",
    "descripcion": "Reconocimiento de placas vehiculares"
  },
  {
    "id": 3,
    "nombre": "Barrera automática",
    "descripcion": "Control de entrada/salida"
  }
]
```

### POST /api/tipos-dispositivo
**Request Body:**
```json
{
  "nombre": "Sensor de ocupación",
  "descripcion": "Detecta si un espacio está ocupado"
}
```

---

## 18. Personas

### GET /api/personas
**Descripción:** Obtener todas las personas

**Response (200 OK):**
```json
[
  {
    "id": 1,
    "nombre": "Juan",
    "apellido": "Pérez",
    "documento": "1234567890",
    "tipoDocumento": "CC",
    "telefono": "3001234567",
    "email": "juan.perez@example.com",
    "direccion": "Calle 10 # 20-30",
    "fechaNacimiento": "1990-05-15"
  }
]
```

### POST /api/personas
**Request Body:**
```json
{
  "nombre": "Juan",
  "apellido": "Pérez",
  "documento": "1234567890",
  "tipoDocumento": "CC",
  "telefono": "3001234567",
  "email": "juan.perez@example.com",
  "direccion": "Calle 10 # 20-30",
  "fechaNacimiento": "1990-05-15"
}
```

---

## 19. Usuarios

### GET /api/usuarios
**Descripción:** Obtener todos los usuarios

**Response (200 OK):**
```json
[
  {
    "id": 1,
    "username": "jperez",
    "email": "jperez@example.com",
    "personaId": 1,
    "personaNombre": "Juan Pérez",
    "personaDocumento": "1234567890",
    "estadoId": 1,
    "estadoNombre": "Activo",
    "fechaCreacion": "2024-01-15"
  }
]
```

### POST /api/usuarios
**Request Body:**
```json
{
  "username": "jperez",
  "email": "jperez@example.com",
  "personaId": 1,
  "estadoId": 1
}
```

**Nota:** La fecha de creación se asigna automáticamente al crear el usuario.

---

## 20. Vehículos

### GET /api/vehiculos
**Descripción:** Obtener todos los vehículos

**Response (200 OK):**
```json
[
  {
    "id": 1,
    "placa": "ABC123",
    "marca": "Toyota",
    "modelo": "Corolla",
    "color": "Blanco",
    "tipoVehiculoId": 1,
    "tipoVehiculoNombre": "Automóvil",
    "personaId": 1,
    "personaNombre": "Juan Pérez",
    "personaDocumento": "1234567890"
  }
]
```

### POST /api/vehiculos
**Request Body:**
```json
{
  "placa": "ABC123",
  "marca": "Toyota",
  "modelo": "Corolla",
  "color": "Blanco",
  "tipoVehiculoId": 1,
  "personaId": 1
}
```

### PUT /api/vehiculos/{id}
**Request Body:**
```json
{
  "placa": "ABC123",
  "marca": "Toyota",
  "modelo": "Corolla 2024",
  "color": "Negro",
  "tipoVehiculoId": 1,
  "personaId": 1
}
```

---

## 21. Tickets (Entrada/Salida)

### GET /api/tickets
**Descripción:** Obtener todos los tickets

**Response (200 OK):**
```json
[
  {
    "id": 1,
    "codigo": "TKT-20240115-001",
    "fechaHoraEntrada": "2024-01-15T08:30:00",
    "fechaHoraSalida": "2024-01-15T12:45:00",
    "vehiculoId": 1,
    "vehiculoPlaca": "ABC123",
    "puntoParqueoId": 1,
    "puntoParqueoNombre": "Punto A1-001",
    "tarifaId": 1,
    "tarifaNombre": "Tarifa por hora",
    "estadoId": 2,
    "estadoNombre": "Cerrado",
    "montoTotal": 15000.0
  }
]
```

### POST /api/tickets
**Descripción:** Crear ticket (registrar entrada)

**Request Body:**
```json
{
  "codigo": "TKT-20240115-001",
  "vehiculoId": 1,
  "puntoParqueoId": 1,
  "tarifaId": 1,
  "estadoId": 1
}
```

**Nota:** La fecha/hora de entrada se asigna automáticamente al momento de crear el ticket.

### PUT /api/tickets/{id}
**Descripción:** Actualizar ticket (registrar salida y calcular monto)

**Request Body:**
```json
{
  "codigo": "TKT-20240115-001",
  "fechaHoraEntrada": "2024-01-15T08:30:00",
  "fechaHoraSalida": "2024-01-15T12:45:00",
  "vehiculoId": 1,
  "puntoParqueoId": 1,
  "tarifaId": 1,
  "estadoId": 2,
  "montoTotal": 15000.0
}
```

---

## 22. Reservas

### GET /api/reservas
**Descripción:** Obtener todas las reservas

**Response (200 OK):**
```json
[
  {
    "id": 1,
    "codigo": "RES-20240115-001",
    "fechaHoraInicio": "2024-01-15T14:00:00",
    "fechaHoraFin": "2024-01-15T18:00:00",
    "usuarioId": 1,
    "usuarioNombre": "jperez",
    "puntoParqueoId": 1,
    "puntoParqueoNombre": "Punto A1-001",
    "estadoId": 1,
    "estadoNombre": "Confirmada",
    "monto": 20000.0
  }
]
```

### POST /api/reservas
**Descripción:** Crear nueva reserva

**Request Body:**
```json
{
  "codigo": "RES-20240115-001",
  "fechaHoraInicio": "2024-01-15T14:00:00",
  "fechaHoraFin": "2024-01-15T18:00:00",
  "usuarioId": 1,
  "puntoParqueoId": 1,
  "estadoId": 1,
  "monto": 20000.0
}
```

### PUT /api/reservas/{id}
**Descripción:** Actualizar reserva (por ejemplo, cancelar)

**Request Body:**
```json
{
  "codigo": "RES-20240115-001",
  "fechaHoraInicio": "2024-01-15T14:00:00",
  "fechaHoraFin": "2024-01-15T18:00:00",
  "usuarioId": 1,
  "puntoParqueoId": 1,
  "estadoId": 3,
  "monto": 20000.0
}
```

---

## 23. Facturas

### GET /api/facturas
**Descripción:** Obtener todas las facturas

**Response (200 OK):**
```json
[
  {
    "id": 1,
    "numero": "FAC-2024-001",
    "fecha": "2024-01-15",
    "ticketId": 1,
    "ticketCodigo": "TKT-20240115-001",
    "usuarioId": 1,
    "usuarioNombre": "jperez",
    "subtotal": 15000.0,
    "impuesto": 2850.0,
    "total": 17850.0,
    "estadoId": 1,
    "estadoNombre": "Pendiente"
  }
]
```

### POST /api/facturas
**Descripción:** Crear factura

**Request Body:**
```json
{
  "numero": "FAC-2024-001",
  "ticketId": 1,
  "usuarioId": 1,
  "subtotal": 15000.0,
  "impuesto": 2850.0,
  "total": 17850.0,
  "estadoId": 1
}
```

**Nota:** La fecha de la factura se asigna automáticamente al momento de crearla.

### PUT /api/facturas/{id}
**Descripción:** Actualizar factura (por ejemplo, marcar como pagada)

**Request Body:**
```json
{
  "numero": "FAC-2024-001",
  "fecha": "2024-01-15",
  "ticketId": 1,
  "usuarioId": 1,
  "subtotal": 15000.0,
  "impuesto": 2850.0,
  "total": 17850.0,
  "estadoId": 2
}
```

---

## 24. Pagos

### GET /api/pagos
**Descripción:** Obtener todos los pagos

**Response (200 OK):**
```json
[
  {
    "id": 1,
    "codigo": "PAG-20240115-001",
    "fechaHora": "2024-01-15T12:50:00",
    "facturaId": 1,
    "facturaNumero": "FAC-2024-001",
    "monto": 17850.0,
    "metodoPago": "Tarjeta de crédito",
    "referencia": "REF123456789",
    "estadoId": 1,
    "estadoNombre": "Aprobado"
  }
]
```

### POST /api/pagos
**Descripción:** Registrar pago

**Request Body:**
```json
{
  "codigo": "PAG-20240115-001",
  "facturaId": 1,
  "monto": 17850.0,
  "metodoPago": "Tarjeta de crédito",
  "referencia": "REF123456789",
  "estadoId": 1
}
```

**Nota:** La fecha/hora del pago se asigna automáticamente al momento de registrarlo.

---

## 25. Dispositivos

### GET /api/dispositivos
**Descripción:** Obtener todos los dispositivos

**Response (200 OK):**
```json
[
  {
    "id": 1,
    "codigo": "SENS-001",
    "nombre": "Sensor Entrada Principal",
    "modelo": "S-100X",
    "tipoDispositivoId": 1,
    "tipoDispositivoNombre": "Sensor de ocupación",
    "parqueaderoId": 1,
    "parqueaderoNombre": "Parqueadero Central",
    "estadoId": 1,
    "estadoNombre": "Activo",
    "fechaInstalacion": "2024-01-10"
  }
]
```

### POST /api/dispositivos
**Descripción:** Registrar dispositivo

**Request Body:**
```json
{
  "codigo": "SENS-001",
  "nombre": "Sensor Entrada Principal",
  "modelo": "S-100X",
  "tipoDispositivoId": 1,
  "parqueaderoId": 1,
  "estadoId": 1,
  "fechaInstalacion": "2024-01-10"
}
```

### PUT /api/dispositivos/{id}
**Descripción:** Actualizar dispositivo

**Request Body:**
```json
{
  "codigo": "SENS-001",
  "nombre": "Sensor Entrada Principal - Actualizado",
  "modelo": "S-100X",
  "tipoDispositivoId": 1,
  "parqueaderoId": 1,
  "estadoId": 2,
  "fechaInstalacion": "2024-01-10"
}
```

---

## �️ Configuración de Parqueaderos (Diseño / Canvas)

### POST /api/parqueaderos/configuracion
**Descripción:** Guardar la configuración completa de un parqueadero (pisos, secciones, subsecciones, puntos, caminos). Crea el parqueadero si no existe.
**Headers:** `Authorization: Bearer <token>`

**Request Body:**
```json
{
  "parkingLot": {
    "id": null,
    "name": "Central Universidad Surcolombiana",
    "empresaId": 1,
    "ciudadId": 1,
    "tipoParqueaderoId": 1,
    "latitud": 2.9273,
    "longitud": -75.2819,
    "direccion": "Calle 9 # 15 - 00",
    "telefono": "3001234567",
    "horaInicio": "06:00",
    "horaFin": "22:00",
    "numeroPuntosParqueo": 50,
    "zonaHoraria": "America/Bogota",
    "tiempoGraciaMinutos": 15,
    "modoCobro": "POR_HORA"
  },
  "floors": [
    {
      "id": null,
      "name": "Piso 1",
      "sections": [
        {
          "id": "section-uuid-1",
          "name": "Sección A",
          "description": "Zona norte",
          "acronym": "SA",
          "coordinates": [{"x": 69, "y": 32}, {"x": 200, "y": 32}, {"x": 200, "y": 180}, {"x": 69, "y": 180}]
        }
      ],
      "subsections": [
        {
          "id": "subsection-uuid-1",
          "name": "Subsección A1",
          "description": "Primera fila",
          "acronym": "SA-SS1",
          "parentSectionId": "section-uuid-1",
          "coordinates": [{"x": 110, "y": 53}, {"x": 180, "y": 53}, {"x": 180, "y": 120}, {"x": 110, "y": 120}],
          "parkingSpots": 2
        }
      ],
      "parkingSpots": [
        {
          "id": "spot-uuid-1",
          "subsectionId": "subsection-uuid-1",
          "coordinates": {
            "topLeft": {"x": 111, "y": 54},
            "topRight": {"x": 136, "y": 54},
            "bottomLeft": {"x": 111, "y": 79},
            "bottomRight": {"x": 136, "y": 79}
          },
          "acronym": "SA-SS1-P1",
          "description": "Puesto 1",
          "type": "normal"
        }
      ],
      "paths": [
        {
          "id": "path-uuid-1",
          "type": "polyline",
          "coordinates": [{"x": 473, "y": 45}, {"x": 473, "y": 250}]
        }
      ]
    }
  ]
}
```

**Response (201 Created):**  
Devuelve la misma estructura pero con IDs de base de datos (Long como String).

### PUT /api/parqueaderos/{id}/configuracion
**Descripción:** Actualizar la configuración de un parqueadero existente

### GET /api/parqueaderos/{id}/configuracion
**Descripción:** Obtener la configuración completa del parqueadero para que el frontend lo dibuje

**Response (200 OK):**
```json
{
  "success": true,
  "data": {
    "parkingLot": {
      "id": 1,
      "name": "Central Universidad Surcolombiana",
      "empresaId": 1,
      "empresaNombre": "USCO Parking",
      "ciudadId": 1,
      "ciudadNombre": "Neiva",
      "tipoParqueaderoId": 1,
      "tipoParqueaderoNombre": "Privado",
      "estadoId": 1,
      "estadoNombre": "ACTIVO",
      "numeroPisos": 1,
      "numeroPuntosParqueo": 50
    },
    "floors": [
      {
        "id": "1",
        "name": "Piso 1",
        "sections": [...],
        "subsections": [...],
        "parkingSpots": [...],
        "paths": [...]
      }
    ]
  }
}
```

### PATCH /api/parqueaderos/{id}/archivar
**Descripción:** Soft-delete — archiva el parqueadero y TODA su configuración en cascada (solo ADMIN/SUPER_ADMIN)
**Headers:** `Authorization: Bearer <token>`

### PATCH /api/parqueaderos/niveles/{nivelId}/archivar
**Descripción:** Archiva un nivel y todas sus secciones/subsecciones/puntos/caminos (solo ADMIN/SUPER_ADMIN)

---

## 🗑️ Archivar (Soft-delete)

Todos los endpoints de eliminación usan `PATCH .../archivar` en vez de `DELETE`.  
Solo roles **ADMIN** y **SUPER_ADMIN** pueden archivar.

| Recurso | Endpoint |
|---------|----------|
| Empresa | `PATCH /api/empresas/{id}/archivar` |
| Parqueadero (cascada) | `PATCH /api/parqueaderos/{id}/archivar` |
| Nivel (cascada) | `PATCH /api/parqueaderos/niveles/{nivelId}/archivar` |
| Sección | `PATCH /api/secciones/{id}/archivar` |
| SubSección | `PATCH /api/sub-secciones/{id}/archivar` |
| Punto Parqueo | `PATCH /api/puntos-parqueo/{id}/archivar` |

---

## 📌 Resumen de Endpoints Implementados

| Módulo | Endpoint Base | Total Endpoints | Descripción |
|--------|--------------|-----------------|-------------|
| Health | `/api/health` | 1 | Verificación de estado |
| Auth | `/api/auth/*` | 11 | Registro, login, PIN, refresh, password |
| Admin | `/api/admin/*` | 5 | Gestión de usuarios/roles (ADMIN) |
| Estados | `/api/estados` | 5 | CRUD de estados |
| Parqueaderos | `/api/parqueaderos` | 4 | CRUD de parqueaderos (sin DELETE) |
| Config Parqueadero | `/api/parqueaderos/*/configuracion` | 3 | Guardar/obtener diseño completo |
| Archivar Parqueadero | `/api/parqueaderos/*/archivar` | 2 | Soft-delete parqueadero/nivel |
| Tarifas | `/api/tarifas` | 5 | CRUD de tarifas |
| Niveles | `/api/niveles` | 6 | CRUD + archivar niveles |
| Empresas | `/api/empresas` | 5 | CRUD + archivar empresas |
| Secciones | `/api/secciones` | 5 | CRUD + archivar secciones |
| Sub-Secciones | `/api/sub-secciones` | 5 | CRUD + archivar subsecciones |
| Puntos Parqueo | `/api/puntos-parqueo` | 5 | CRUD + archivar puntos |
| Personas | `/api/personas` | 4 | CRUD personas (sin DELETE) |
| Usuarios | `/api/usuarios` | 4 | CRUD usuarios (sin DELETE) |
| Vehículos | `/api/vehiculos` | 5 | CRUD de vehículos |
| Tickets | `/api/tickets` | 5 | CRUD entrada/salida |
| Reservas | `/api/reservas` | 5 | CRUD de reservas |
| Facturas | `/api/facturas` | 5 | CRUD de facturas |
| Pagos | `/api/pagos` | 5 | CRUD de pagos |
| Dispositivos | `/api/dispositivos` | 5 | CRUD de dispositivos |
| Catálogo | `/api/tipos-*`, `/api/roles` | 20 | Catálogos (tipos, roles) |
| Ubicación | `/api/paises`, `/api/departamentos`, `/api/ciudades` | 15 | Países, departamentos, ciudades |
| **TOTAL** | | **~138** | **Endpoints activos** |
