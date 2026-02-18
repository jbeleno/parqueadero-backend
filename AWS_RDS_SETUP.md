# 🗄️ Configuración de Base de Datos AWS RDS

## Información de Conexión

- **Host:** `database-1.c5qc4qo884xb.us-east-2.rds.amazonaws.com`
- **Puerto:** `5432`
- **Usuario:** `postgres`
- **Password:** `jesusADOLFO2355`
- **Región:** `us-east-2`

## 📋 Pasos para Configurar la Base de Datos

### 1. Conectarse a RDS desde Windows

#### Opción 1: Usar pgAdmin (Recomendado - Interfaz Gráfica) ✅

**Descargar pgAdmin:**
1. Ir a https://www.pgadmin.org/download/pgadmin-4-windows/
2. Descargar e instalar pgAdmin 4

**Conectar a AWS RDS:**
1. Abrir pgAdmin
2. Click derecho en "Servers" → "Register" → "Server"
3. **Tab General:**
   - Name: `AWS RDS - Parqueaderos`
4. **Tab Connection:**
   - Host: `database-1.c5qc4qo884xb.us-east-2.rds.amazonaws.com`
   - Port: `5432`
   - Maintenance database: `postgres`
   - Username: `postgres`
   - Password: `jesusADOLFO2355`
   - ✅ Save password
5. **Tab SSL:**
   - SSL mode: `Require`
6. Click "Save"

#### Opción 2: Usar DBeaver (Alternativa multiplataforma)

**Descargar DBeaver:**
1. Ir a https://dbeaver.io/download/
2. Descargar e instalar DBeaver Community

**Conectar a AWS RDS:**
1. Abrir DBeaver
2. Click en "New Database Connection" (icono de enchufe)
3. Seleccionar "PostgreSQL" → Next
4. **Main:**
   - Host: `database-1.c5qc4qo884xb.us-east-2.rds.amazonaws.com`
   - Port: `5432`
   - Database: `postgres`
   - Username: `postgres`
   - Password: `jesusADOLFO2355`
5. **SSL Tab:**
   - Use SSL: ✅ Enabled
   - SSL mode: `require`
6. Test Connection → Finish

#### Opción 3: Instalar PostgreSQL Client (psql) en Windows

**Descargar PostgreSQL:**
1. Ir a https://www.postgresql.org/download/windows/
2. Descargar el instalador de PostgreSQL 17
3. Durante la instalación, seleccionar solo "Command Line Tools"
4. Agregar al PATH: `C:\Program Files\PostgreSQL\17\bin`

**Conectarse con psql:**
```powershell
# Cerrar y reabrir PowerShell después de instalar
psql -h database-1.c5qc4qo884xb.us-east-2.rds.amazonaws.com -p 5432 -U postgres -d postgres
# Password: jesusADOLFO2355
```

#### Opción 4: Dejar que Spring Boot cree todo automáticamente 🚀 (MÁS FÁCIL)

**No necesitas conectarte manualmente.** Spring Boot puede crear la base de datos automáticamente.

**Paso 1:** Actualizar `application.yaml` para crear la BD automáticamente:
```yaml
spring:
  datasource:
    url: jdbc:postgresql://database-1.c5qc4qo884xb.us-east-2.rds.amazonaws.com:5432/postgres?ssl=true&sslmode=require
    username: postgres
    password: 'jesusADOLFO2355'
  jpa:
    hibernate:
      ddl-auto: update
    properties:
      hibernate:
        default_schema: public
        jdbc:
          lob:
            non_contextual_creation: true
```

**Paso 2:** Crear un script SQL de inicialización en `src/main/resources/schema.sql`:
```sql
CREATE DATABASE IF NOT EXISTS parqueaderos;
```

**Paso 3:** Ejecutar la aplicación:
```powershell
.\mvnw.cmd spring-boot:run
```

Spring Boot creará automáticamente todas las tablas en el schema `public`.

### 2. Crear la base de datos del proyecto

Una vez conectado a PostgreSQL, ejecuta:

```sql
-- Crear la base de datos
CREATE DATABASE parqueaderos;

-- Conectarse a la nueva base de datos
\c parqueaderos

-- Habilitar la extensión PostGIS
CREATE EXTENSION IF NOT EXISTS postgis;

-- Verificar que PostGIS está instalado
SELECT PostGIS_version();
```

**Resultado esperado de PostGIS_version():**
```
 postgis_version 
-----------------
 3.x USE_GEOS=1 USE_PROJ=1 USE_STATS=1
```

### 3. Verificar las tablas creadas

Después de ejecutar la aplicación Spring Boot por primera vez, verifica que se crearon las tablas:

```sql
-- Listar todas las tablas
\dt

-- Ver estructura de una tabla específica
\d ticket

-- Contar registros
SELECT COUNT(*) FROM estado;
```

## ⚙️ Configuración de la Aplicación

El archivo `application.yaml` ya ha sido actualizado con la configuración de AWS RDS:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://database-1.c5qc4qo884xb.us-east-2.rds.amazonaws.com:5432/parqueaderos?ssl=true&sslmode=require
    username: postgres
    password: 'jesusADOLFO2355'
```

## 🚀 Ejecutar la Aplicación

```bash
# Compilar y ejecutar
.\mvnw.cmd spring-boot:run

# O generar JAR y ejecutar
.\mvnw.cmd clean package -DskipTests
java -jar target\parqueaderos-api-0.0.1-SNAPSHOT.jar
```

La aplicación se conectará automáticamente a AWS RDS y creará todas las tablas necesarias.

## 🔒 Seguridad

### Variables de Entorno (Recomendado para Producción)

En lugar de tener la contraseña en el archivo `application.yaml`, usa variables de entorno:

**application.yaml:**
```yaml
spring:
  datasource:
    url: jdbc:postgresql://${DB_HOST}:${DB_PORT}/${DB_NAME}?ssl=true&sslmode=require
    username: ${DB_USER}
    password: ${DB_PASSWORD}
```

**Configurar variables (Windows PowerShell):**
```powershell
$env:DB_HOST="database-1.c5qc4qo884xb.us-east-2.rds.amazonaws.com"
$env:DB_PORT="5432"
$env:DB_NAME="parqueaderos"
$env:DB_USER="postgres"
$env:DB_PASSWORD="jesusADOLFO2355"

# Ejecutar la aplicación
.\mvnw.cmd spring-boot:run
```

**Configurar variables (Linux/Mac/WSL):**
```bash
export DB_HOST="database-1.c5qc4qo884xb.us-east-2.rds.amazonaws.com"
export DB_PORT="5432"
export DB_NAME="parqueaderos"
export DB_USER="postgres"
export DB_PASSWORD="jesusADOLFO2355"

# Ejecutar la aplicación
./mvnw spring-boot:run
```

## 🛠️ Conexión SSL

AWS RDS requiere conexión SSL. La configuración actual usa `sslmode=require` que valida la conexión SSL pero no verifica el certificado del servidor.

### Opciones de SSL Mode:

- `disable` - Sin SSL (no recomendado para producción)
- `require` - Requiere SSL pero no verifica certificado ✅ **(Configuración actual)**
- `verify-ca` - Verifica el certificado con CA
- `verify-full` - Verificación completa del certificado

Si necesitas usar `verify-full`, descarga el certificado de AWS:

```bash
# Descargar certificado global de AWS RDS
curl -o rds-combined-ca-bundle.pem https://truststore.pki.rds.amazonaws.com/global/global-bundle.pem

# Actualizar application.yaml
url: jdbc:postgresql://...?ssl=true&sslmode=verify-full&sslrootcert=rds-combined-ca-bundle.pem
```

## 📊 Gestión de la Base de Datos

### Backup de la base de datos

```bash
pg_dump -h database-1.c5qc4qo884xb.us-east-2.rds.amazonaws.com \
        -p 5432 -U postgres -d parqueaderos \
        -F c -b -v -f parqueaderos_backup.dump
```

### Restaurar backup

```bash
pg_restore -h database-1.c5qc4qo884xb.us-east-2.rds.amazonaws.com \
           -p 5432 -U postgres -d parqueaderos \
           -v parqueaderos_backup.dump
```

### Conectar con herramientas gráficas

**DBeaver / pgAdmin:**
- Host: `database-1.c5qc4qo884xb.us-east-2.rds.amazonaws.com`
- Port: `5432`
- Database: `parqueaderos`
- Username: `postgres`
- Password: `jesusADOLFO2355`
- SSL Mode: `require`

## 🐛 Solución de Problemas

### Error: "Connection refused"

1. Verifica que el Security Group de RDS permita conexiones desde tu IP
2. En la consola de AWS RDS, ve a tu instancia → Connectivity & Security → Security Groups
3. Agrega una regla Inbound para el puerto 5432 desde tu IP

### Error: "SSL connection required"

Agrega `?ssl=true&sslmode=require` a la URL de conexión.

### Error: "Database does not exist"

Conéctate primero a la base `postgres` y crea la base `parqueaderos`:
```bash
psql -h database-1.c5qc4qo884xb.us-east-2.rds.amazonaws.com -U postgres -d postgres
CREATE DATABASE parqueaderos;
```

### Error: "Extension postgis does not exist"

```sql
\c parqueaderos
CREATE EXTENSION postgis;
```

Si PostGIS no está disponible, contacta al administrador de AWS RDS para habilitar la extensión en el Parameter Group.

## 📈 Monitoreo

Puedes monitorear la instancia RDS desde:
- **AWS CloudWatch** - Métricas de rendimiento
- **AWS RDS Console** - Estado de la instancia
- **Logs de PostgreSQL** - Consultas y errores

## 🔗 Enlaces Útiles

- [AWS RDS PostgreSQL Documentation](https://docs.aws.amazon.com/AmazonRDS/latest/UserGuide/CHAP_PostgreSQL.html)
- [PostGIS on AWS RDS](https://docs.aws.amazon.com/AmazonRDS/latest/UserGuide/Appendix.PostgreSQL.CommonDBATasks.PostGIS.html)
- [PostgreSQL JDBC Driver SSL](https://jdbc.postgresql.org/documentation/ssl/)

---

**✅ Configuración completada** - La aplicación está lista para conectarse a AWS RDS.
