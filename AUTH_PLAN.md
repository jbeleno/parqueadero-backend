# AUTH_PLAN.md — Plan de Implementación de Autenticación y Autorización

> **Proyecto:** parqueaderos-api  
> **Stack:** Spring Boot 3.5.10 · Java 21 · PostgreSQL 17 · JWT (jjwt 0.12.6)  
> **Fecha:** 2026-02-18

---

## 1. Estado Actual (diagnóstico)

### Lo que YA existe:
| Componente | Archivo | Estado |
|---|---|---|
| Login básico | `auth/controller/AuthController.java` | Solo `POST /api/auth/login` |
| JWT Service | `auth/service/JwtService.java` | Genera/verifica tokens, 24h expiration |
| Filtro JWT | `auth/filter/JwtAuthenticationFilter.java` | Extrae Bearer token de header |
| Security Config | `auth/config/SecurityConfig.java` | BCrypt, stateless, `/api/auth/**` público |
| UserDetailsService | `auth/service/UserDetailsServiceImpl.java` | Busca por correo |
| Entidades | `Usuario`, `Persona`, `UsuarioRol`, `Rol`, `Estado` | Tablas en DB |
| Repositorios | `UsuarioRepository`, `UsuarioRolRepository`, `PersonaRepository`, `RolRepository` | Queries básicas |

### Lo que FALTA o está MAL:
| Problema | Detalle |
|---|---|
| **Roles hardcodeados** | `UserDetailsServiceImpl` y `AuthService` siempre ponen `.roles("USER")` en lugar de cargar de `usuario_rol` + `rol` |
| **No hay registro** | No existe endpoint de registro. `UsuarioService.save()` pone `passwordHash = ""`. |
| **No hay cambio de contraseña** | No existe endpoint ni lógica |
| **No hay recuperación de contraseña** | No hay PIN, no hay flujo de "olvidé mi contraseña" |
| **No hay confirmación de cuenta** | No hay campo `confirmado` en `Usuario` ni flujo de verificación |
| **No hay envío de correos** | No existe servicio de email |
| **No hay refresh token** | Si el token expira, hay que volver a hacer login |
| **JWT no incluye roles** | El token no lleva claims de roles, el servidor no puede validar permisos por ruta |
| **No hay logout/blacklist** | No se pueden invalidar tokens antes de su expiración |
| **`@PreAuthorize` sin efecto real** | `@EnableMethodSecurity` está habilitado pero nadie usa `@PreAuthorize` |

---

## 2. Cambios en Base de Datos (DDL)

### 2.1 Nuevas columnas en `usuario`
```sql
ALTER TABLE usuario ADD COLUMN confirmado BOOLEAN NOT NULL DEFAULT false;
ALTER TABLE usuario ADD COLUMN pin_codigo VARCHAR(6);
ALTER TABLE usuario ADD COLUMN pin_expiracion TIMESTAMP;
ALTER TABLE usuario ADD COLUMN intentos_fallidos INT NOT NULL DEFAULT 0;
ALTER TABLE usuario ADD COLUMN bloqueado_hasta TIMESTAMP;
```

### 2.2 Nueva tabla `refresh_token`
```sql
CREATE TABLE refresh_token (
    id          BIGSERIAL PRIMARY KEY,
    token       VARCHAR(255) NOT NULL UNIQUE,
    usuario_id  BIGINT NOT NULL REFERENCES usuario(id),
    expiracion  TIMESTAMP NOT NULL,
    revocado    BOOLEAN NOT NULL DEFAULT false,
    creado_en   TIMESTAMP NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_refresh_token_token ON refresh_token(token);
CREATE INDEX idx_refresh_token_usuario ON refresh_token(usuario_id);
```

> **Nota:** Con `ddl-auto: update`, Hibernate creará las columnas/tablas automáticamente al agregar las entidades Java. Pero es buena práctica tener el DDL documentado.

---

## 3. Dependencia nueva: Envío de correos

### Librería: **Resend** (resend.com)

Es el equivalente en Java a lo que usas en Python con una sola API key. No necesita SMTP, no necesita configurar servidor de correos. Solo un API key y listo.

**Agregar en `pom.xml`:**
```xml
<!-- Resend: envío de correos con API key (como Python) -->
<dependency>
    <groupId>com.resend</groupId>
    <artifactId>resend-java</artifactId>
    <version>3.1.0</version>
</dependency>
```

**Agregar en `application.yaml`:**
```yaml
app:
  resend:
    api-key: ${RESEND_API_KEY:re_xxxxxxxxxxxx}
    from-email: ${RESEND_FROM:noreply@tudominio.com}
```

**Alternativas consideradas:**
| Librería | Pros | Contras |
|---|---|---|
| **Resend** (recomendada) | API key simple, SDK para Java, 3000 emails gratis/mes, excelente DX | Requiere dominio verificado para producción |
| SendGrid | Estándar de la industria, 100 emails gratis/día | SDK más pesado, setup más complejo |
| Amazon SES | Ya usan AWS, barato ($0.10/1000 emails) | Requiere verificar identidades, más config |
| spring-boot-starter-mail (SMTP) | Sin dependencias externas | Necesita servidor SMTP, configuración de credenciales, menos confiable |

---

## 4. Endpoints a Implementar

### 4.1 Autenticación (`/api/auth/`)

| # | Método | Ruta | Público | Descripción |
|---|---|---|---|---|
| 1 | `POST` | `/api/auth/registro` | Sí | Crear cuenta nueva (persona + usuario) |
| 2 | `POST` | `/api/auth/login` | Sí | Login → devuelve accessToken + refreshToken |
| 3 | `POST` | `/api/auth/refresh` | Sí | Renovar accessToken con refreshToken |
| 4 | `POST` | `/api/auth/logout` | No | Revocar refreshToken |
| 5 | `POST` | `/api/auth/confirmar-cuenta` | Sí | Confirmar cuenta con PIN de 6 dígitos |
| 6 | `POST` | `/api/auth/reenviar-confirmacion` | Sí | Reenviar PIN de confirmación al correo |
| 7 | `POST` | `/api/auth/olvide-password` | Sí | Solicitar PIN de recuperación de contraseña |
| 8 | `POST` | `/api/auth/verificar-pin` | Sí | Verificar que el PIN es válido (sin cambiar password aún) |
| 9 | `POST` | `/api/auth/resetear-password` | Sí | Cambiar contraseña usando PIN verificado |
| 10 | `PUT` | `/api/auth/cambiar-password` | No (JWT) | Cambiar contraseña (requiere password actual) |
| 11 | `GET` | `/api/auth/me` | No (JWT) | Datos del usuario autenticado + roles |

### 4.2 Gestión de Roles (admin) (`/api/admin/`)

| # | Método | Ruta | Rol requerido | Descripción |
|---|---|---|---|---|
| 12 | `POST` | `/api/admin/usuarios/{id}/roles` | ADMIN | Asignar rol a usuario |
| 13 | `DELETE` | `/api/admin/usuarios/{id}/roles/{rolId}` | ADMIN | Quitar rol a usuario |
| 14 | `GET` | `/api/admin/usuarios/{id}/roles` | ADMIN | Ver roles de un usuario |
| 15 | `PUT` | `/api/admin/usuarios/{id}/estado` | ADMIN | Activar/desactivar usuario |
| 16 | `GET` | `/api/admin/usuarios` | ADMIN | Listar todos los usuarios (con filtros) |

---

## 5. DTOs Nuevos

### 5.1 `RegistroRequest.java`
```java
@Data
public class RegistroRequest {
    @NotBlank String nombre;
    @NotBlank String apellido;
    @NotBlank String telefono;
    @NotBlank String tipoDocumento;   // CC, CE, TI, NIT, PASAPORTE
    @NotBlank String numeroDocumento;
    @NotBlank @Email String correo;
    @NotBlank @Size(min = 8) String password;
    Long empresaId;                    // Opcional: si pertenece a una empresa
}
```

### 5.2 `AuthResponse.java` (actualizar)
```java
@Data @Builder
public class AuthResponse {
    String accessToken;
    String refreshToken;
    String correo;
    String nombreCompleto;
    List<String> roles;
    String tipo;         // "Bearer"
    Long expiresIn;      // Segundos hasta expiración
}
```

### 5.3 `RefreshRequest.java`
```java
@Data
public class RefreshRequest {
    @NotBlank String refreshToken;
}
```

### 5.4 `CambiarPasswordRequest.java`
```java
@Data
public class CambiarPasswordRequest {
    @NotBlank String passwordActual;
    @NotBlank @Size(min = 8) String passwordNuevo;
}
```

### 5.5 `PinRequest.java`
```java
@Data
public class PinRequest {
    @NotBlank @Email String correo;
}
```

### 5.6 `VerificarPinRequest.java`
```java
@Data
public class VerificarPinRequest {
    @NotBlank @Email String correo;
    @NotBlank @Size(min = 6, max = 6) String pin;
}
```

### 5.7 `ResetearPasswordRequest.java`
```java
@Data
public class ResetearPasswordRequest {
    @NotBlank @Email String correo;
    @NotBlank @Size(min = 6, max = 6) String pin;
    @NotBlank @Size(min = 8) String passwordNuevo;
}
```

### 5.8 `AsignarRolRequest.java`
```java
@Data
public class AsignarRolRequest {
    @NotNull Long rolId;
}
```

### 5.9 `UsuarioAdminDTO.java`
```java
@Data
public class UsuarioAdminDTO {
    Long id;
    String correo;
    String nombreCompleto;
    String numeroDocumento;
    Boolean confirmado;
    String estadoNombre;
    String empresaNombre;
    List<String> roles;
    LocalDateTime fechaCreacion;
}
```

---

## 6. Entidades Nuevas/Modificadas

### 6.1 Modificar `Usuario.java`
Agregar campos:
```java
@Column(nullable = false)
private Boolean confirmado = false;

@Column(name = "pin_codigo", length = 6)
private String pinCodigo;

@Column(name = "pin_expiracion")
private LocalDateTime pinExpiracion;

@Column(name = "intentos_fallidos", nullable = false)
private Integer intentosFallidos = 0;

@Column(name = "bloqueado_hasta")
private LocalDateTime bloqueadoHasta;
```

### 6.2 Nueva entidad `RefreshToken.java`
```java
@Entity
@Table(name = "refresh_token")
@Data @NoArgsConstructor @AllArgsConstructor
public class RefreshToken {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, unique = true)
    private String token;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;
    
    @Column(nullable = false)
    private LocalDateTime expiracion;
    
    @Column(nullable = false)
    private Boolean revocado = false;
    
    @Column(name = "creado_en", nullable = false)
    private LocalDateTime creadoEn = LocalDateTime.now();
}
```

### 6.3 Nuevo repo `RefreshTokenRepository.java`
```java
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    Optional<RefreshToken> findByToken(String token);
    List<RefreshToken> findByUsuarioIdAndRevocadoFalse(Long usuarioId);
    void deleteByExpiracionBefore(LocalDateTime fecha); // cleanup
}
```

---

## 7. Servicios a Crear/Modificar

### 7.1 `EmailService.java` (NUEVO)
**Ubicación:** `com.usco.parqueaderos_api.common.service.EmailService`

```
Responsabilidades:
- enviarConfirmacionCuenta(String correo, String nombre, String pin)
- enviarRecuperacionPassword(String correo, String nombre, String pin)
- enviarCambioPasswordExitoso(String correo, String nombre)

Usa: Resend Java SDK
Configuración: app.resend.api-key + app.resend.from-email
Templates: HTML inline simple (sin Thymeleaf, por simplicidad)
```

### 7.2 `PinService.java` (NUEVO)
**Ubicación:** `com.usco.parqueaderos_api.auth.service.PinService`

```
Responsabilidades:
- String generarPin()           → genera PIN aleatorio de 6 dígitos
- void guardarPin(Usuario u)    → genera pin, guarda en usuario, setea expiración (15 min)
- boolean verificarPin(Usuario u, String pin) → valida pin + no expirado
- void limpiarPin(Usuario u)    → borra pin después de uso exitoso
```

### 7.3 `RefreshTokenService.java` (NUEVO)
**Ubicación:** `com.usco.parqueaderos_api.auth.service.RefreshTokenService`

```
Responsabilidades:
- RefreshToken crearRefreshToken(Usuario usuario) → genera UUID, guarda en DB, 7 días expiración
- RefreshToken verificarRefreshToken(String token) → busca, valida no expirado ni revocado
- void revocarTokensDeUsuario(Long usuarioId)     → revoca todos los refresh tokens activos
- void limpiarTokensExpirados()                    → @Scheduled: borra tokens expirados cada 24h
```

### 7.4 Modificar `AuthService.java`
```
Cambios:
- login():      cargar roles reales de DB, generar accessToken + refreshToken
                 incluir roles como claims en JWT. Verificar cuenta confirmada.
                 Verificar no bloqueado. Incrementar intentos fallidos si falla.
- registro():   crear Persona + Usuario (passwordHash con BCrypt),
                 generar PIN, enviar email de confirmación, asignar rol USER por defecto.
- refresh():    verificar refreshToken, generar nuevo accessToken
- logout():     revocar refreshToken
- confirmarCuenta(): verificar PIN, marcar confirmado=true
- reenviarConfirmacion(): generar nuevo PIN, enviar email
- olvidePassword(): generar PIN, enviar email
- verificarPin(): solo verificar que el PIN es válido
- resetearPassword(): verificar PIN + cambiar passwordHash
- cambiarPassword(): verificar password actual + cambiar passwordHash
- me():          retornar datos del usuario autenticado + roles
```

### 7.5 Modificar `UserDetailsServiceImpl.java`
```
Cambios:
- Cargar roles reales de UsuarioRol + Rol en lugar de hardcodear "USER"
- Mapear roles a GrantedAuthority: "ROLE_ADMIN", "ROLE_USER", "ROLE_OPERADOR"
```

### 7.6 Modificar `JwtService.java`
```
Cambios:
- generateToken(): incluir claim "roles" con lista de roles del usuario
- extractRoles(): nuevo método para extraer roles del token
```

### 7.7 Nuevo `AdminService.java`
**Ubicación:** `com.usco.parqueaderos_api.auth.service.AdminService`

```
Responsabilidades:
- asignarRol(Long usuarioId, Long rolId)
- quitarRol(Long usuarioId, Long rolId)
- getRoles(Long usuarioId) → List<String>
- cambiarEstadoUsuario(Long usuarioId, Long estadoId)
- listarUsuarios(filtros) → Page<UsuarioAdminDTO>
```

---

## 8. Controllers a Crear/Modificar

### 8.1 Modificar `AuthController.java`
Agregar los 11 endpoints de la sección 4.1.

### 8.2 Nuevo `AdminController.java`
**Ubicación:** `com.usco.parqueaderos_api.auth.controller.AdminController`

```java
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController { ... }
```

---

## 9. Configuración de Seguridad

### 9.1 Actualizar `SecurityConfig.java`

```java
.authorizeHttpRequests(auth -> auth
    // Públicas
    .requestMatchers("/api/auth/registro", "/api/auth/login",
                     "/api/auth/refresh", "/api/auth/confirmar-cuenta",
                     "/api/auth/reenviar-confirmacion", "/api/auth/olvide-password",
                     "/api/auth/verificar-pin", "/api/auth/resetear-password").permitAll()
    .requestMatchers("/api/health", "/swagger-ui/**", "/swagger-ui.html",
                     "/api-docs/**", "/ws/**").permitAll()
    // Admin
    .requestMatchers("/api/admin/**").hasRole("ADMIN")
    // Todo lo demás: autenticado
    .anyRequest().authenticated()
)
```

---

## 10. Actualizar `application.yaml`

```yaml
app:
  jwt:
    secret: ${JWT_SECRET:...}
    access-expiration-ms: ${JWT_ACCESS_EXP:3600000}    # 1 hora (antes era 24h)
    refresh-expiration-ms: ${JWT_REFRESH_EXP:604800000} # 7 días
  resend:
    api-key: ${RESEND_API_KEY:re_xxxxxxxxxxxx}
    from-email: ${RESEND_FROM:noreply@tudominio.com}
  pin:
    expiration-minutes: ${PIN_EXPIRATION:15}
    longitud: 6
  security:
    max-intentos-fallidos: 5
    bloqueo-minutos: 30
```

---

## 11. Flujos de Negocio (diagramas)

### 11.1 Registro
```
Cliente → POST /api/auth/registro {datos}
   → Crear Persona
   → Crear Usuario (passwordHash = BCrypt, confirmado = false)
   → Asignar rol USER por defecto en usuario_rol
   → Generar PIN 6 dígitos → guardar en usuario.pin_codigo + pin_expiracion
   → Enviar email con PIN vía Resend
   → Respuesta: "Cuenta creada. Revisa tu correo para confirmar."
```

### 11.2 Confirmación de Cuenta
```
Cliente → POST /api/auth/confirmar-cuenta {correo, pin}
   → Buscar usuario por correo
   → Verificar pin_codigo == pin Y pin_expiracion > ahora
   → Marcar confirmado = true
   → Limpiar pin_codigo, pin_expiracion
   → Respuesta: "Cuenta confirmada exitosamente"
```

### 11.3 Login
```
Cliente → POST /api/auth/login {correo, password}
   → Verificar usuario existe
   → Verificar no está bloqueado (bloqueado_hasta < ahora)
   → Verificar confirmado == true (si no → 403 "Confirma tu cuenta primero")
   → Autenticar con AuthenticationManager (BCrypt)
   → Si falla: incrementar intentos_fallidos. Si >= 5 → bloquear 30 min
   → Si OK: resetear intentos_fallidos = 0
   → Cargar roles de usuario_rol + rol
   → Generar accessToken (1h) con claims: sub=correo, roles=[...]
   → Generar refreshToken (7d) → guardar en DB
   → Respuesta: {accessToken, refreshToken, correo, roles, expiresIn}
```

### 11.4 Refresh Token
```
Cliente → POST /api/auth/refresh {refreshToken}
   → Buscar refreshToken en DB
   → Verificar no expirado, no revocado
   → Generar nuevo accessToken
   → Respuesta: {accessToken, refreshToken (mismo), expiresIn}
```

### 11.5 Olvide mi Contraseña
```
Cliente → POST /api/auth/olvide-password {correo}
   → Buscar usuario por correo (si no existe, responder OK igual para evitar enumeration)
   → Generar PIN 6 dígitos → guardar en usuario
   → Enviar email con PIN vía Resend
   → Respuesta: "Si el correo está registrado, recibirás un PIN"

Cliente → POST /api/auth/verificar-pin {correo, pin}
   → Verificar que el PIN es válido (sin cambiar nada)
   → Respuesta: "PIN válido"

Cliente → POST /api/auth/resetear-password {correo, pin, passwordNuevo}
   → Verificar PIN válido
   → Cambiar passwordHash = BCrypt(passwordNuevo)
   → Limpiar pin_codigo, pin_expiracion
   → Revocar todos los refresh tokens del usuario
   → Enviar email "Tu contraseña fue cambiada"
   → Respuesta: "Contraseña actualizada"
```

### 11.6 Cambio de Contraseña (autenticado)
```
Cliente → PUT /api/auth/cambiar-password {passwordActual, passwordNuevo}
   (Header: Authorization: Bearer <accessToken>)
   → Obtener usuario del SecurityContext
   → Verificar passwordActual con BCrypt
   → Cambiar passwordHash = BCrypt(passwordNuevo)
   → Revocar todos los refresh tokens
   → Enviar email "Tu contraseña fue cambiada"
   → Respuesta: "Contraseña actualizada"
```

---

## 12. Roles del Sistema

| Rol | Descripción | Permisos clave |
|---|---|---|
| `USER` | Usuario registrado | Ver/reservar puntos de parqueo, ver tickets propios, ver facturas propias |
| `OPERADOR` | Operario del parqueadero | Crear/cerrar tickets, gestionar puntos de parqueo |
| `ADMIN` | Administrador | Todo lo anterior + gestionar usuarios/roles, ver reportes, CRUD completo |
| `SUPER_ADMIN` | Super administrador | Todo + gestionar empresas, parqueaderos, catálogos |

### Endpoints protegidos por rol (ejemplos):
```java
@PreAuthorize("hasRole('ADMIN')")           // Solo ADMIN
@PreAuthorize("hasAnyRole('ADMIN','OPERADOR')") // ADMIN o OPERADOR
@PreAuthorize("hasRole('USER')")            // Cualquier autenticado (todos tienen USER)
```

### Matriz de permisos por endpoint:
| Endpoint base | USER | OPERADOR | ADMIN |
|---|---|---|---|
| `/api/auth/**` (público) | -- | -- | -- |
| `/api/tickets` GET (propios) | ✓ | ✓ | ✓ |
| `/api/tickets` POST/PUT | ✗ | ✓ | ✓ |
| `/api/reservas` (propias) | ✓ | ✓ | ✓ |
| `/api/parqueaderos` POST/PUT/DELETE | ✗ | ✗ | ✓ |
| `/api/admin/**` | ✗ | ✗ | ✓ |
| `/api/usuarios` GET (self) | ✓ | ✓ | ✓ |
| `/api/usuarios` GET (all) | ✗ | ✗ | ✓ |

---

## 13. Archivos a Crear

| # | Archivo | Acción |
|---|---|---|
| 1 | `auth/dto/RegistroRequest.java` | CREAR |
| 2 | `auth/dto/RefreshRequest.java` | CREAR |
| 3 | `auth/dto/CambiarPasswordRequest.java` | CREAR |
| 4 | `auth/dto/PinRequest.java` | CREAR |
| 5 | `auth/dto/VerificarPinRequest.java` | CREAR |
| 6 | `auth/dto/ResetearPasswordRequest.java` | CREAR |
| 7 | `auth/dto/AsignarRolRequest.java` | CREAR |
| 8 | `auth/dto/UsuarioAdminDTO.java` | CREAR |
| 9 | `auth/dto/AuthResponse.java` | MODIFICAR (agregar refreshToken, roles, expiresIn) |
| 10 | `auth/entity/RefreshToken.java` | CREAR |
| 11 | `auth/repository/RefreshTokenRepository.java` | CREAR |
| 12 | `auth/service/PinService.java` | CREAR |
| 13 | `auth/service/RefreshTokenService.java` | CREAR |
| 14 | `auth/service/AdminService.java` | CREAR |
| 15 | `auth/service/AuthService.java` | REESCRIBIR (11 métodos) |
| 16 | `auth/service/JwtService.java` | MODIFICAR (agregar roles en claims) |
| 17 | `auth/service/UserDetailsServiceImpl.java` | MODIFICAR (cargar roles reales) |
| 18 | `auth/controller/AuthController.java` | REESCRIBIR (11 endpoints) |
| 19 | `auth/controller/AdminController.java` | CREAR |
| 20 | `auth/config/SecurityConfig.java` | MODIFICAR (rutas por rol) |
| 21 | `common/service/EmailService.java` | CREAR |
| 22 | `user/entity/Usuario.java` | MODIFICAR (5 campos nuevos) |
| 23 | `pom.xml` | MODIFICAR (agregar resend-java) |
| 24 | `application.yaml` | MODIFICAR (agregar config de resend, pin, seguridad) |

**Total: 16 archivos nuevos, 8 archivos modificados = 24 cambios**

---

## 14. Orden de Implementación (paso a paso)

### Fase A — Base de datos y entidades
```
A1. Modificar Usuario.java (5 campos nuevos)
A2. Crear RefreshToken.java + RefreshTokenRepository.java
A3. Verificar que ddl-auto: update crea las columnas al arrancar
```

### Fase B — Dependencia y servicio de email
```
B1. Agregar resend-java en pom.xml
B2. Agregar config en application.yaml
B3. Crear EmailService.java
```

### Fase C — Servicios de auth
```
C1. Crear PinService.java
C2. Crear RefreshTokenService.java
C3. Modificar JwtService.java (roles en claims, access vs refresh expiration)
C4. Modificar UserDetailsServiceImpl.java (cargar roles reales)
C5. Reescribir AuthService.java (11 métodos)
C6. Crear AdminService.java
```

### Fase D — DTOs
```
D1. Crear los 7 DTOs nuevos
D2. Modificar AuthResponse.java
```

### Fase E — Controllers
```
E1. Reescribir AuthController.java (11 endpoints)
E2. Crear AdminController.java (5 endpoints)
```

### Fase F — Security config
```
F1. Modificar SecurityConfig.java (rutas públicas, rutas admin, rutas autenticadas)
```

### Fase G — Verificación
```
G1. Compilar con mvn clean compile
G2. Arrancar y verificar Swagger UI
G3. Probar flujo completo: registro → confirmar → login → refresh → cambiar password
G4. Probar flujo: olvide password → verificar PIN → resetear
G5. Probar admin: asignar rol, listar usuarios
G6. Git commit y push
```

---

## 15. Templates de Email (HTML inline)

### 15.1 Confirmación de Cuenta
```
Asunto: "Confirma tu cuenta en Parqueaderos"
Cuerpo: "Hola {nombre}, tu código de confirmación es: {PIN}. Expira en 15 minutos."
```

### 15.2 Recuperación de Contraseña
```
Asunto: "Recupera tu contraseña en Parqueaderos"
Cuerpo: "Hola {nombre}, tu PIN de recuperación es: {PIN}. Expira en 15 minutos."
```

### 15.3 Contraseña Cambiada
```
Asunto: "Tu contraseña fue cambiada"
Cuerpo: "Hola {nombre}, tu contraseña fue cambiada exitosamente. Si no fuiste tú, contacta soporte."
```

---

## 16. Consideraciones de Seguridad

| Medida | Implementación |
|---|---|
| **Contraseñas hasheadas** | BCrypt (ya implementado) |
| **PIN con expiración** | 15 minutos, un solo uso |
| **Bloqueo por intentos** | 5 intentos fallidos → bloqueo 30 min |
| **Enumeración de usuarios** | `/olvide-password` siempre responde "si existe, te enviamos un email" |
| **Refresh token en DB** | Revocable, con expiración, limpieza automática |
| **Tokens de corta vida** | accessToken: 1h, refreshToken: 7d |
| **Roles en JWT** | Validación server-side con `@PreAuthorize` |
| **CSRF deshabilitado** | OK para API stateless con JWT |
| **CORS** | Configurar orígenes permitidos cuando haya frontend |

---

## 17. Resumen de Resend (la librería de email)

```java
// Así de simple es usar Resend en Java:
@Service
public class EmailService {
    
    private final Resend resend;
    private final String fromEmail;
    
    public EmailService(@Value("${app.resend.api-key}") String apiKey,
                        @Value("${app.resend.from-email}") String from) {
        this.resend = new Resend(apiKey);  // Solo pasas el API key
        this.fromEmail = from;
    }
    
    public void enviar(String para, String asunto, String htmlBody) {
        CreateEmailOptions params = CreateEmailOptions.builder()
            .from(fromEmail)
            .to(para)
            .subject(asunto)
            .html(htmlBody)
            .build();
        resend.emails().send(params);
    }
}
```

Para obtener tu API key:
1. Ir a https://resend.com → crear cuenta gratis
2. Dashboard → API Keys → Create API Key
3. Copiar la key (empieza con `re_...`)
4. Setear como variable de entorno: `RESEND_API_KEY=re_xxxxx`

**Gratis:** 3,000 emails/mes, 100 emails/día.
