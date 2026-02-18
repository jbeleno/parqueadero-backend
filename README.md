# 🚗 Parqueaderos API

API REST para sistema de gestión de parqueaderos con PostgreSQL + PostGIS.

## 📋 Requisitos Previos

- **Java 21** o superior
- **PostgreSQL 17** con extensión **PostGIS 3.6**
- **Maven 3.8+** (incluido con el wrapper `mvnw`)
- **Git** (opcional)

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

### Opción 2: AWS RDS (Producción) ✅ CONFIGURACIÓN ACTUAL

La aplicación está configurada para conectarse a **AWS RDS PostgreSQL**.

**Ver instrucciones completas:** [AWS_RDS_SETUP.md](./AWS_RDS_SETUP.md)

**Resumen de conexión:**
- **Host:** `database-1.c5qc4qo884xb.us-east-2.rds.amazonaws.com`
- **Puerto:** `5432`
- **Base de datos:** `parqueaderos`
- **Usuario:** `postgres`

**Conectarse y crear la base de datos:**
```bash
# Conectar a RDS
psql -h database-1.c5qc4qo884xb.us-east-2.rds.amazonaws.com -p 5432 -U postgres -d postgres

# Crear base de datos
CREATE DATABASE parqueaderos;
\c parqueaderos
CREATE EXTENSION IF NOT EXISTS postgis;
```

> **Nota:** Para cambiar a base de datos local, edita `src/main/resources/application.yaml`

## ⚙️ Configuración del Proyecto

### 1. Clonar el repositorio (si aplica)

```bash
git clone <repository-url>
cd parqueaderos-api
```

### 2. Configurar credenciales de base de datos

**La aplicación está configurada para AWS RDS** (ver `src/main/resources/application.yaml`).

Para usar una base de datos local, edita:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/parqueaderos
    username: postgres
    password: 'tu_password_local'  # Cambiar por tu contraseña
```

## 🚀 Comandos para Ejecutar

### Compilar el proyecto

```bash
.\mvnw.cmd clean compile
```

### Ejecutar tests

```bash
.\mvnw.cmd test
```

### Generar el archivo JAR

```bash
.\mvnw.cmd clean package
```

### Generar el JAR sin ejecutar tests

```bash
.\mvnw.cmd clean package -DskipTests
```

### Ejecutar la aplicación

**Opción 1: Usando Maven Wrapper**
```bash
.\mvnw.cmd spring-boot:run
```

**Opción 2: Usando el JAR generado**
```bash
java -jar target\parqueaderos-api-0.0.1-SNAPSHOT.jar
```

### Instalar en repositorio local Maven

```bash
.\mvnw.cmd clean install
```

## 🌐 Acceso a la API

Una vez iniciada la aplicación, la API estará disponible en:

```
http://localhost:8080
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

Consulta el archivo [ENDPOINTS.md](./ENDPOINTS.md) para ver la documentación completa de todos los endpoints disponibles.

### Resumen de módulos disponibles

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
  -d "{\"nombre\":\"Activo\",\"descripcion\":\"Estado activo\"}"
```

**Obtener un parqueadero por ID:**
```bash
curl http://localhost:8080/api/parqueaderos/1
```

### Usando Postman o Thunder Client

1. Importa la colección desde [ENDPOINTS.md](./ENDPOINTS.md)
2. Configura la variable de entorno `BASE_URL=http://localhost:8080`
3. Ejecuta las peticiones

## 📁 Estructura del Proyecto

```
parqueaderos-api/
├── src/
│   ├── main/
│   │   ├── java/com/usco/parqueaderos_api/
│   │   │   ├── controller/      # Controladores REST
│   │   │   ├── dto/              # Data Transfer Objects
│   │   │   ├── entity/           # Entidades JPA
│   │   │   ├── repository/       # Repositorios
│   │   │   ├── service/          # Lógica de negocio
│   │   │   └── ParqueaderosApiApplication.java
│   │   └── resources/
│   │       ├── application.yaml  # Configuración
│   │       ├── static/
│   │       └── templates/
│   └── test/
├── target/                       # Archivos compilados
├── pom.xml                       # Dependencias Maven
├── mvnw / mvnw.cmd              # Maven Wrapper
├── ENDPOINTS.md                  # Documentación API
├── modelo_db.md                  # Modelo de base de datos
└── README.md                     # Este archivo
```

## 🛠️ Tecnologías

- **Spring Boot 3.5.10** - Framework principal
- **Java 21** - Lenguaje de programación
- **PostgreSQL 17.7** - Base de datos relacional
- **PostGIS 3.6** - Extensión geoespacial
- **Hibernate Spatial 6.6.41** - ORM con soporte espacial
- **Lombok** - Reducción de boilerplate
- **Maven** - Gestión de dependencias

## 🔧 Configuración Adicional

### Cambiar el puerto del servidor

En `application.yaml`:
```yaml
server:
  port: 9090  # Cambiar a tu puerto preferido
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

Verifica las credenciales en `application.yaml` y que PostgreSQL esté corriendo.

### Error: "Extension postgis is not available"

Instala PostGIS:
```bash
# Windows (usando Stack Builder)
# O descarga desde: https://postgis.net/windows_downloads/

# Linux
sudo apt-get install postgis postgresql-17-postgis-3
```

### Error: "Port 8080 already in use"

Cambia el puerto en `application.yaml` o detén el proceso que usa el puerto 8080.

### Las tablas no se crean automáticamente

Verifica que `spring.jpa.hibernate.ddl-auto` esté en `update` o `create` en `application.yaml`.

## 📞 Contacto

Para reportar problemas o sugerencias, contacta al equipo de desarrollo.

## 📄 Licencia

[Definir licencia del proyecto]

---

**Desarrollado para pasantías universitarias - USCO 2024**
