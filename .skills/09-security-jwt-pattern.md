# Skill 09: Seguridad — JWT + Spring Security

## Dependencias requeridas en pom.xml

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-api</artifactId>
    <version>0.12.6</version>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-impl</artifactId>
    <version>0.12.6</version>
    <scope>runtime</scope>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-jackson</artifactId>
    <version>0.12.6</version>
    <scope>runtime</scope>
</dependency>
```

---

## Configuración en application.yaml

```yaml
app:
  jwt:
    secret: ${JWT_SECRET:clave-secreta-desarrollo-cambiar-en-produccion-min-256-bits-xxxxx}
    expiration-ms: ${JWT_EXPIRATION:86400000}
    refresh-expiration-ms: ${JWT_REFRESH_EXPIRATION:604800000}
```

---

## 1. JwtService.java (paquete: auth.service)

```java
package com.usco.parqueaderos_api.auth.service;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;

@Service
public class JwtService {
    
    @Value("${app.jwt.secret}")
    private String secret;
    
    @Value("${app.jwt.expiration-ms}")
    private long expirationMs;
    
    @Value("${app.jwt.refresh-expiration-ms}")
    private long refreshExpirationMs;
    
    public String generateToken(Long userId, String email, String role) {
        return Jwts.builder()
                .subject(userId.toString())
                .claims(Map.of(
                        "email", email,
                        "role", role
                ))
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expirationMs))
                .signWith(getSigningKey())
                .compact();
    }
    
    public String generateRefreshToken(Long userId) {
        return Jwts.builder()
                .subject(userId.toString())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + refreshExpirationMs))
                .signWith(getSigningKey())
                .compact();
    }
    
    public Long getUserIdFromToken(String token) {
        return Long.parseLong(getClaims(token).getSubject());
    }
    
    public String getEmailFromToken(String token) {
        return getClaims(token).get("email", String.class);
    }
    
    public boolean isTokenValid(String token) {
        try {
            getClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }
    
    private Claims getClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
    
    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }
}
```

---

## 2. JwtAuthenticationFilter.java (paquete: auth.filter)

```java
package com.usco.parqueaderos_api.auth.filter;

import com.usco.parqueaderos_api.auth.service.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    
    private final JwtService jwtService;
    
    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        
        String authHeader = request.getHeader("Authorization");
        
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }
        
        String token = authHeader.substring(7);
        
        if (jwtService.isTokenValid(token)) {
            Long userId = jwtService.getUserIdFromToken(token);
            
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            userId,
                            null,
                            List.of(new SimpleGrantedAuthority("ROLE_USER"))
                    );
            
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }
        
        filterChain.doFilter(request, response);
    }
}
```

---

## 3. SecurityConfig.java (paquete: auth.config)

```java
package com.usco.parqueaderos_api.auth.config;

import com.usco.parqueaderos_api.auth.filter.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {
    
    private final JwtAuthenticationFilter jwtAuthFilter;
    
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> 
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // Endpoints públicos
                .requestMatchers("/api/health").permitAll()
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/ws/**").permitAll()
                .requestMatchers("/swagger-ui/**", "/api-docs/**", "/v3/api-docs/**").permitAll()
                
                // GETs de catálogos públicos
                .requestMatchers(HttpMethod.GET, "/api/paises/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/departamentos/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/ciudades/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/estados/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/tipos-vehiculo/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/tipos-parqueadero/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/tipos-punto-parqueo/**").permitAll()
                
                // Todo lo demás requiere autenticación
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
        
        return http.build();
    }
    
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
```

---

## 4. DTOs de autenticación (paquete: auth.dto)

### LoginRequest.java
```java
package com.usco.parqueaderos_api.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginRequest {
    
    @NotBlank(message = "El correo es obligatorio")
    @Email(message = "Formato de correo inválido")
    private String correo;
    
    @NotBlank(message = "La contraseña es obligatoria")
    private String password;
}
```

### RegisterRequest.java
```java
package com.usco.parqueaderos_api.auth.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class RegisterRequest {
    
    @NotBlank(message = "El correo es obligatorio")
    @Email(message = "Formato de correo inválido")
    private String correo;
    
    @NotBlank(message = "La contraseña es obligatoria")
    @Size(min = 8, message = "La contraseña debe tener al menos 8 caracteres")
    private String password;
    
    @NotBlank(message = "El nombre es obligatorio")
    private String nombre;
    
    @NotBlank(message = "El apellido es obligatorio")
    private String apellido;
    
    private String telefono;
    private String tipoDocumento;
    private String numeroDocumento;
}
```

### LoginResponse.java
```java
package com.usco.parqueaderos_api.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class LoginResponse {
    private String accessToken;
    private String refreshToken;
    private Long userId;
    private String correo;
    private String nombre;
}
```

---

## 5. AuthService.java (paquete: auth.service)

```java
package com.usco.parqueaderos_api.auth.service;

import com.usco.parqueaderos_api.auth.dto.LoginRequest;
import com.usco.parqueaderos_api.auth.dto.LoginResponse;
import com.usco.parqueaderos_api.auth.dto.RegisterRequest;
import com.usco.parqueaderos_api.common.exception.BusinessException;
import com.usco.parqueaderos_api.common.exception.DuplicateResourceException;
import com.usco.parqueaderos_api.catalog.repository.EstadoRepository;
import com.usco.parqueaderos_api.user.entity.Persona;
import com.usco.parqueaderos_api.user.entity.Usuario;
import com.usco.parqueaderos_api.user.repository.PersonaRepository;
import com.usco.parqueaderos_api.user.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuthService {
    
    private final UsuarioRepository usuarioRepository;
    private final PersonaRepository personaRepository;
    private final EstadoRepository estadoRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    
    @Transactional
    public LoginResponse login(LoginRequest request) {
        Usuario usuario = usuarioRepository.findByCorreo(request.getCorreo())
                .orElseThrow(() -> new BusinessException("Credenciales inválidas"));
        
        if (!passwordEncoder.matches(request.getPassword(), usuario.getPasswordHash())) {
            throw new BusinessException("Credenciales inválidas");
        }
        
        String accessToken = jwtService.generateToken(
                usuario.getId(),
                usuario.getCorreo(),
                "USER"  // Extraer del UsuarioRol si aplica
        );
        String refreshToken = jwtService.generateRefreshToken(usuario.getId());
        
        return new LoginResponse(
                accessToken,
                refreshToken,
                usuario.getId(),
                usuario.getCorreo(),
                usuario.getPersona().getNombre()
        );
    }
    
    @Transactional
    public LoginResponse register(RegisterRequest request) {
        if (usuarioRepository.existsByCorreo(request.getCorreo())) {
            throw new DuplicateResourceException("Usuario", "correo", request.getCorreo());
        }
        
        // Crear Persona
        Persona persona = new Persona();
        persona.setNombre(request.getNombre());
        persona.setApellido(request.getApellido());
        persona.setTelefono(request.getTelefono());
        persona.setTipoDocumento(request.getTipoDocumento());
        persona.setNumeroDocumento(request.getNumeroDocumento());
        persona = personaRepository.save(persona);
        
        // Crear Usuario
        Usuario usuario = new Usuario();
        usuario.setCorreo(request.getCorreo());
        usuario.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        usuario.setPersona(persona);
        usuario.setFechaCreacion(LocalDateTime.now());
        
        // Estado ACTIVO (ID 1 por defecto)
        usuario.setEstado(estadoRepository.findById(1L)
                .orElseThrow(() -> new BusinessException("Estado ACTIVO no encontrado en la base de datos")));
        
        usuario = usuarioRepository.save(usuario);
        
        String accessToken = jwtService.generateToken(usuario.getId(), usuario.getCorreo(), "USER");
        String refreshToken = jwtService.generateRefreshToken(usuario.getId());
        
        return new LoginResponse(
                accessToken,
                refreshToken,
                usuario.getId(),
                usuario.getCorreo(),
                persona.getNombre()
        );
    }
}
```

---

## 6. AuthController.java (paquete: auth.controller)

```java
package com.usco.parqueaderos_api.auth.controller;

import com.usco.parqueaderos_api.auth.dto.LoginRequest;
import com.usco.parqueaderos_api.auth.dto.LoginResponse;
import com.usco.parqueaderos_api.auth.dto.RegisterRequest;
import com.usco.parqueaderos_api.auth.service.AuthService;
import com.usco.parqueaderos_api.common.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    
    private final AuthService authService;
    
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<LoginResponse>> register(@Valid @RequestBody RegisterRequest request) {
        LoginResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response));
    }
}
```

---

## CorsConfig.java (paquete: common.config)

Reemplaza el `@CrossOrigin(origins = "*")` de cada controller.

```java
package com.usco.parqueaderos_api.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
public class CorsConfig {
    
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(List.of("*"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
```

**IMPORTANTE**: Al crear la `CorsConfig`, agregar `.cors(cors -> cors.configurationSource(corsConfigurationSource()))` al `SecurityFilterChain` en `SecurityConfig.java`:

```java
http
    .cors(cors -> cors.configurationSource(corsConfigurationSource()))
    .csrf(csrf -> csrf.disable())
    // ... resto
```

Y quitar `@CrossOrigin(origins = "*")` de TODOS los controllers existentes.
