# 🚗 Parqueaderos API

[![Java](https://img.shields.io/badge/Java-21-007396?style=flat-square&logo=openjdk&logoColor=white)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.5.10-6DB33F?style=flat-square&logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-17-336791?style=flat-square&logo=postgresql&logoColor=white)](https://www.postgresql.org/)
[![PostGIS](https://img.shields.io/badge/PostGIS-3.6-336791?style=flat-square)](https://postgis.net/)
[![Docker](https://img.shields.io/badge/Docker-2496ED?style=flat-square&logo=docker&logoColor=white)](https://www.docker.com/)
[![JWT](https://img.shields.io/badge/Auth-JWT-000000?style=flat-square&logo=jsonwebtokens&logoColor=white)](https://jwt.io/)

REST API para sistema de gestión de parqueaderos con disponibilidad de espacios en tiempo real, autenticación JWT y consultas geoespaciales con PostGIS.

> **Contexto:** Proyecto académico desarrollado durante pasantías en la **Universidad Surcolombiana (USCO)**. Diseñado como un backend de producción real, con autenticación, websockets, geolocalización y despliegue dockerizado.

---

## ✨ Overview

Sistema backend completo para administrar parqueaderos comerciales: jerarquía de espacios (parqueadero → niveles → secciones → puntos), gestión de usuarios y vehículos, tickets de entrada/salida, reservas, tarifas, facturación, pagos y dispositivos IoT (sensores, cámaras).

La API expone **121 endpoints REST** documentados con OpenAPI/Swagger y soporta actualizaciones de disponibilidad **en tiempo real** mediante WebSocket + STOMP.

## 🎯 Key Features

- 🔐 **Autenticación con JWT** y autorización basada en roles (Spring Security).
- 📡 **WebSocket + STOMP** para actualizaciones en tiempo real de disponibilidad de espacios.
- 🗺️ **Consultas geoespaciales con PostGIS** (Hibernate Spatial) para búsquedas por proximidad.
- 📚 **OpenAPI / Swagger UI** auto-documentado en `/swagger-ui.html`.
- 📦 **Dockerizado** con `Dockerfile` y `docker-compose` listos para producción.
- ☁️ **Desplegado en Dockploy** con Postgres autoalojado.
- 📧 **Servicio de email SMTP** integrado (Spring Mail) para notificaciones.
- 🧱 Arquitectura por capas (Controller → Service → Repository → Entity) con DTOs y validación.

## 🛠️ Tech Stack

| Categoría | Tecnologías |
|-----------|-------------|
| Lenguaje | Java 21 |
| Framework | Spring Boot 3.5.10 |
| Persistencia | Spring Data JPA · Hibernate · Hibernate Spatial 6.6 |
| Base de datos | PostgreSQL 17 + PostGIS 3.6 |
| Seguridad | Spring Security · JWT (jjwt 0.12.6) |
| Real-time | Spring WebSocket + STOMP |
| Documentación | SpringDoc OpenAPI 2.8 (Swagger UI) |
| Build & Deploy | Maven · Docker · docker-compose · Dockploy |
| Otros | Lombok · Validation · Spring Mail (SMTP) |

---

## 📋 Requisitos Previos

- **Java 21** o superior
- **PostgreSQL 17** con extensión **PostGIS 3.6**
- **Maven 3.8+** (incluido con el wrapper `mvnw`)
- **Docker** (opcional, para correr la base de datos o la app en contenedor)

## 🗄️ Configuración de Base de Datos

### Opción 1: Base de Datos Local (Desarrollo)

#### 1. Crear la base de datos

```sql
CREATE DATABASE parqueaderos;
```

#### 2. Habilitar PostGIS

```sql
\c parqueaderos
CREATE EXTENSION IF NOT EXISTS postgis;
```

#### 3. Verificar instalación de PostGIS

```sql
SELECT PostGIS_version();
```

### Opción 2: Postgres en Dockploy (Producción)

La aplicación está desplegada con **Postgres autoalojado en Dockploy**. La configuración de conexión se inyecta vía variables de entorno (ver `.env.example`).

```env
DB_HOST=<tu-host-dockploy>
DB_PORT=5432
DB_NAME=parqueaderos
DB_USERNAME=<usuario>
DB_PASSWORD=<password>
```

Una vez conectado, crear la base de datos y habilitar PostGIS:

```bash
psql -h $DB_HOST -p $DB_PORT -U $DB_USERNAME -d postgres
CREATE DATABASE parqueaderos;
\c parqueaderos
CREATE EXTENSION IF NOT EXISTS postgis;
```

> **Nota:** La configuración de conexión se controla desde `src/main/resources/application.yaml` con valores tomados de variables de entorno.

## ⚙️ Configuración del Proyecto

### 1. Clonar el repositorio

```bash
git clone https://github.com/jbeleno/parqueadero-backend.git
cd parqueadero-backend
```

### 2. Configurar variables de entorno

Copia `.env.example` a `.env` y rellena los valores:

```bash
cp .env.example .env
```

Para usar una base de datos local, también puedes editar directamente:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/parqueaderos
    username: postgres
    password: 'tu_password_local'
```

## 🚀 Comandos para Ejecutar

### Compilar el proyecto

```bash
./mvnw clean compile
```

### Ejecutar tests

```bash
./mvnw test
```

### Generar el JAR

```bash
./mvnw clean package
```

### Generar el JAR sin ejecutar tests

```bash
./mvnw clean package -DskipTests
```

### Ejecutar la aplicación

**Opción 1: Maven Wrapper**
```bash
./mvnw spring-boot:run
```

**Opción 2: JAR generado**
```bash
java -jar target/parqueaderos-api-0.0.1-SNAPSHOT.jar
```

### Ejecutar con Docker Compose

```bash
docker-compose up --build
```

> En Windows usa `mvnw.cmd` en lugar de `./mvnw`.

## 🌐 Acceso a la API

Una vez iniciada la aplicación:

```
http://localhost:8080
```

### Documentación interactiva (Swagger UI)

```
http://localhost:8080/swagger-ui.html
```

### Verificar que el servicio está corriendo

```bash
curl http://localhost:8080/api/health
```

**Respuesta esperada:**
```json
{
  "status": "UP",
  "timestamp": "2024-01-15T10:30:00"
}
```

## 📚 Documentación de Endpoints

Consulta [ENDPOINTS.md](./ENDPOINTS.md) para la documentación completa.

### Resumen de módulos

| Módulo | Endpoint | Descripción |
|--------|----------|-------------|
| Health | `/api/health` | Estado del servicio |
| Estados | `/api/estados` | Catálogo de estados |
| Países | `/api/paises` | Gestión de países |
| Departamentos | `/api/departamentos` | Gestión de departamentos |
| Ciudades | `/api/ciudades` | Gestión de ciudades |
| Empresas | `/api/empresas` | Gestión de empresas |
| Parqueaderos | `/api/parqueaderos` | Gestión de parqueaderos |
| Niveles | `/api/niveles` | Niveles de parqueadero |
| Secciones | `/api/secciones` | Secciones por nivel |
| Sub-Secciones | `/api/sub-secciones` | Sub-secciones |
| Puntos Parqueo | `/api/puntos-parqueo` | Espacios individuales |
| Tarifas | `/api/tarifas` | Gestión de tarifas |
| Tipos | `/api/tipos-*` | Catálogos de tipos |
| Roles | `/api/roles` | Roles de usuario |
| Personas | `/api/personas` | Gestión de personas |
| Usuarios | `/api/usuarios` | Gestión de usuarios |
| Vehículos | `/api/vehiculos` | Gestión de vehículos |
| Tickets | `/api/tickets` | Entrada/salida |
| Reservas | `/api/reservas` | Reservas de espacios |
| Facturas | `/api/facturas` | Facturación |
| Pagos | `/api/pagos` | Registro de pagos |
| Dispositivos | `/api/dispositivos` | Sensores/cámaras |

**Total: 121 endpoints REST**

## 🧪 Probar Endpoints

### Usando cURL

**Obtener todos los parqueaderos:**
```bash
curl http://localhost:8080/api/parqueaderos
```

**Crear un nuevo estado:**
```bash
curl -X POST http://localhost:8080/api/estados \
  -H "Content-Type: application/json" \
  -d '{"nombre":"Activo","descripcion":"Estado activo"}'
```

**Obtener un parqueadero por ID:**
```bash
curl http://localhost:8080/api/parqueaderos/1
```

### Usando Postman / Thunder Client

1. Importa la colección desde [ENDPOINTS.md](./ENDPOINTS.md)
2. Configura la variable de entorno `BASE_URL=http://localhost:8080`
3. Ejecuta las peticiones

## 📁 Estructura del Proyecto

```
parqueadero-backend/
├── src/
│   ├── main/
│   │   ├── java/com/usco/parqueaderos_api/
│   │   │   ├── controller/      # Controladores REST
│   │   │   ├── dto/             # Data Transfer Objects
│   │   │   ├── entity/          # Entidades JPA
│   │   │   ├── repository/      # Repositorios
│   │   │   ├── service/         # Lógica de negocio
│   │   │   └── ParqueaderosApiApplication.java
│   │   └── resources/
│   │       ├── application.yaml # Configuración
│   │       ├── static/
│   │       └── templates/
│   └── test/
├── target/                      # Archivos compilados
├── pom.xml                      # Dependencias Maven
├── mvnw / mvnw.cmd              # Maven Wrapper
├── Dockerfile                   # Imagen Docker
├── docker-compose.yml           # Compose para desarrollo
├── .env.example                 # Variables de entorno
├── ENDPOINTS.md                 # Documentación API
├── modelo_db.md                 # Modelo de base de datos
└── README.md                    # Este archivo
```

## 🔧 Configuración Adicional

### Cambiar el puerto del servidor

En `application.yaml`:
```yaml
server:
  port: 9090
```

### Habilitar logs SQL detallados

```yaml
logging:
  level:
    org.hibernate.SQL: DEBUG
    org.hibernate.type.descriptor.sql.BasicBinder: TRACE
```

### Configurar CORS para producción

Edita `@CrossOrigin` en los controladores o crea una configuración global.

## 🐛 Solución de Problemas

### Error: "PSQLException: FATAL: password authentication failed"

Verifica las credenciales en `application.yaml` o en tu `.env` y que PostgreSQL esté corriendo.

### Error: "Extension postgis is not available"

Instala PostGIS:
```bash
# Linux
sudo apt-get install postgis postgresql-17-postgis-3

# Windows
# Descarga desde: https://postgis.net/windows_downloads/
```

### Error: "Port 8080 already in use"

Cambia el puerto en `application.yaml` o detén el proceso que ocupa el 8080.

### Las tablas no se crean automáticamente

Verifica que `spring.jpa.hibernate.ddl-auto` esté en `update` o `create` en `application.yaml`.

---

## 📄 Licencia

Proyecto académico — uso educativo. [Definir licencia formal si se publica].

---

**Desarrollado durante pasantías universitarias — Universidad Surcolombiana (USCO), 2024.**
