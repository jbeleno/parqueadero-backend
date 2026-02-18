package com.usco.parqueaderos_api.auth.service;

import com.usco.parqueaderos_api.auth.dto.*;
import com.usco.parqueaderos_api.auth.entity.RefreshToken;
import com.usco.parqueaderos_api.catalog.entity.Estado;
import com.usco.parqueaderos_api.catalog.entity.Rol;
import com.usco.parqueaderos_api.catalog.repository.EstadoRepository;
import com.usco.parqueaderos_api.catalog.repository.RolRepository;
import com.usco.parqueaderos_api.common.exception.BusinessException;
import com.usco.parqueaderos_api.common.exception.ResourceNotFoundException;
import com.usco.parqueaderos_api.common.service.EmailService;
import com.usco.parqueaderos_api.user.entity.Persona;
import com.usco.parqueaderos_api.user.entity.Usuario;
import com.usco.parqueaderos_api.user.entity.UsuarioRol;
import com.usco.parqueaderos_api.user.repository.PersonaRepository;
import com.usco.parqueaderos_api.user.repository.UsuarioRepository;
import com.usco.parqueaderos_api.user.repository.UsuarioRolRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final PersonaRepository personaRepository;
    private final UsuarioRolRepository usuarioRolRepository;
    private final RolRepository rolRepository;
    private final EstadoRepository estadoRepository;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final PinService pinService;
    private final UserDetailsServiceImpl userDetailsService;
    private final AuthenticationManager authenticationManager;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    @Value("${app.security.max-intentos-fallidos:5}")
    private int maxIntentosFallidos;

    @Value("${app.security.bloqueo-minutos:30}")
    private int bloqueoMinutos;

    // ── 1. REGISTRO ────────────────────────────────────────────────────────────

    @Transactional
    public String registro(RegistroRequest request) {
        if (usuarioRepository.existsByCorreo(request.getCorreo())) {
            throw new BusinessException("El correo ya está registrado");
        }
        if (personaRepository.findByNumeroDocumento(request.getNumeroDocumento()).isPresent()) {
            throw new BusinessException("El número de documento ya está registrado");
        }

        Estado estadoActivo = estadoRepository.findByNombreIgnoreCase("ACTIVO")
                .orElseGet(() -> {
                    Estado e = new Estado();
                    e.setNombre("ACTIVO");
                    e.setDescripcion("Estado activo");
                    return estadoRepository.save(e);
                });

        Persona persona = new Persona();
        persona.setNombre(request.getNombre());
        persona.setApellido(request.getApellido());
        persona.setTelefono(request.getTelefono());
        persona.setTipoDocumento(request.getTipoDocumento());
        persona.setNumeroDocumento(request.getNumeroDocumento());
        personaRepository.save(persona);

        Usuario usuario = new Usuario();
        usuario.setCorreo(request.getCorreo());
        usuario.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        usuario.setEstado(estadoActivo);
        usuario.setPersona(persona);
        usuario.setConfirmado(false);
        usuario.setIntentosFallidos(0);
        usuario.setFechaCreacion(LocalDateTime.now());
        usuarioRepository.save(usuario);

        // Asignar rol USER por defecto
        Rol rolUser = rolRepository.findByNombreIgnoreCase("USER")
                .orElseGet(() -> {
                    Rol r = new Rol();
                    r.setNombre("USER");
                    r.setDescripcion("Usuario estándar");
                    r.setEstado(estadoActivo);
                    return rolRepository.save(r);
                });
        UsuarioRol ur = new UsuarioRol();
        ur.setUsuario(usuario);
        ur.setRol(rolUser);
        usuarioRolRepository.save(ur);

        // Generar y enviar PIN de confirmación
        String pin = pinService.guardarPin(usuario);
        emailService.enviarConfirmacionCuenta(usuario.getCorreo(),
                persona.getNombre() + " " + persona.getApellido(), pin);

        return "Cuenta creada. Revisa tu correo para confirmar tu cuenta.";
    }

    // ── 2. LOGIN ───────────────────────────────────────────────────────────────

    @Transactional
    public AuthResponse login(AuthRequest request) {
        Usuario usuario = usuarioRepository.findByCorreo(request.getCorreo())
                .orElseThrow(() -> new BadCredentialsException("Credenciales inválidas"));

        if (usuario.getBloqueadoHasta() != null && LocalDateTime.now().isBefore(usuario.getBloqueadoHasta())) {
            throw new BusinessException("Cuenta bloqueada temporalmente. Intenta de nuevo más tarde.");
        }

        if (!usuario.getConfirmado()) {
            throw new BusinessException("Debes confirmar tu cuenta antes de iniciar sesión.");
        }

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getCorreo(), request.getPassword())
            );
        } catch (AuthenticationException e) {
            int intentos = usuario.getIntentosFallidos() + 1;
            usuario.setIntentosFallidos(intentos);
            if (intentos >= maxIntentosFallidos) {
                usuario.setBloqueadoHasta(LocalDateTime.now().plusMinutes(bloqueoMinutos));
                usuarioRepository.save(usuario);
                throw new BusinessException("Demasiados intentos fallidos. Cuenta bloqueada por " + bloqueoMinutos + " minutos.");
            }
            usuarioRepository.save(usuario);
            throw new BadCredentialsException("Credenciales inválidas");
        }

        // Reset intentos fallidos
        usuario.setIntentosFallidos(0);
        usuario.setBloqueadoHasta(null);
        usuarioRepository.save(usuario);

        UserDetails userDetails = userDetailsService.loadUserByUsername(usuario.getCorreo());
        String accessToken = jwtService.generateToken(userDetails);
        RefreshToken refreshToken = refreshTokenService.crearRefreshToken(usuario);

        List<String> roles = userDetails.getAuthorities().stream()
                .map(a -> a.getAuthority())
                .toList();

        String nombreCompleto = usuario.getPersona().getNombre() + " " + usuario.getPersona().getApellido();

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken.getToken())
                .correo(usuario.getCorreo())
                .nombreCompleto(nombreCompleto)
                .roles(roles)
                .tipo("Bearer")
                .expiresIn(jwtService.getAccessExpirationMs() / 1000)
                .build();
    }

    // ── 3. REFRESH TOKEN ───────────────────────────────────────────────────────

    public AuthResponse refresh(RefreshRequest request) {
        RefreshToken rt = refreshTokenService.verificarRefreshToken(request.getRefreshToken());
        UserDetails userDetails = userDetailsService.loadUserByUsername(rt.getUsuario().getCorreo());
        String newAccessToken = jwtService.generateToken(userDetails);

        List<String> roles = userDetails.getAuthorities().stream()
                .map(a -> a.getAuthority())
                .toList();

        return AuthResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(request.getRefreshToken())
                .correo(rt.getUsuario().getCorreo())
                .roles(roles)
                .tipo("Bearer")
                .expiresIn(jwtService.getAccessExpirationMs() / 1000)
                .build();
    }

    // ── 4. LOGOUT ──────────────────────────────────────────────────────────────

    @Transactional
    public void logout(RefreshRequest request) {
        RefreshToken rt = refreshTokenService.verificarRefreshToken(request.getRefreshToken());
        refreshTokenService.revocarTokensDeUsuario(rt.getUsuario().getId());
    }

    // ── 5. CONFIRMAR CUENTA ────────────────────────────────────────────────────

    @Transactional
    public String confirmarCuenta(VerificarPinRequest request) {
        Usuario usuario = usuarioRepository.findByCorreo(request.getCorreo())
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", 0L));
        if (usuario.getConfirmado()) {
            return "La cuenta ya estaba confirmada.";
        }
        if (!pinService.verificarPin(usuario, request.getPin())) {
            throw new BusinessException("PIN inválido o expirado");
        }
        usuario.setConfirmado(true);
        pinService.limpiarPin(usuario);
        return "Cuenta confirmada exitosamente.";
    }

    // ── 6. REENVIAR CONFIRMACIÓN ───────────────────────────────────────────────

    @Transactional
    public String reenviarConfirmacion(PinRequest request) {
        Usuario usuario = usuarioRepository.findByCorreo(request.getCorreo())
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", 0L));
        if (usuario.getConfirmado()) {
            throw new BusinessException("La cuenta ya está confirmada.");
        }
        String pin = pinService.guardarPin(usuario);
        String nombreCompleto = usuario.getPersona().getNombre() + " " + usuario.getPersona().getApellido();
        emailService.enviarConfirmacionCuenta(usuario.getCorreo(), nombreCompleto, pin);
        return "Se reenvió el PIN de confirmación al correo registrado.";
    }

    // ── 7. OLVIDÉ CONTRASEÑA ───────────────────────────────────────────────────

    @Transactional
    public String olvidePassword(PinRequest request) {
        // Anti-enumeración: siempre misma respuesta
        usuarioRepository.findByCorreo(request.getCorreo()).ifPresent(usuario -> {
            String pin = pinService.guardarPin(usuario);
            String nombre = usuario.getPersona().getNombre() + " " + usuario.getPersona().getApellido();
            emailService.enviarRecuperacionPassword(usuario.getCorreo(), nombre, pin);
        });
        return "Si el correo está registrado, recibirás un PIN de recuperación.";
    }

    // ── 8. VERIFICAR PIN ───────────────────────────────────────────────────────

    public String verificarPin(VerificarPinRequest request) {
        Usuario usuario = usuarioRepository.findByCorreo(request.getCorreo())
                .orElseThrow(() -> new BusinessException("PIN inválido o expirado"));
        if (!pinService.verificarPin(usuario, request.getPin())) {
            throw new BusinessException("PIN inválido o expirado");
        }
        return "PIN válido.";
    }

    // ── 9. RESETEAR CONTRASEÑA ─────────────────────────────────────────────────

    @Transactional
    public String resetearPassword(ResetearPasswordRequest request) {
        Usuario usuario = usuarioRepository.findByCorreo(request.getCorreo())
                .orElseThrow(() -> new BusinessException("PIN inválido o expirado"));
        if (!pinService.verificarPin(usuario, request.getPin())) {
            throw new BusinessException("PIN inválido o expirado");
        }
        usuario.setPasswordHash(passwordEncoder.encode(request.getPasswordNuevo()));
        pinService.limpiarPin(usuario);
        refreshTokenService.revocarTokensDeUsuario(usuario.getId());
        String nombre = usuario.getPersona().getNombre() + " " + usuario.getPersona().getApellido();
        emailService.enviarCambioPasswordExitoso(usuario.getCorreo(), nombre);
        return "Contraseña actualizada exitosamente.";
    }

    // ── 10. CAMBIAR CONTRASEÑA (autenticado) ───────────────────────────────────

    @Transactional
    public String cambiarPassword(String correo, CambiarPasswordRequest request) {
        Usuario usuario = usuarioRepository.findByCorreo(correo)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", 0L));
        if (!passwordEncoder.matches(request.getPasswordActual(), usuario.getPasswordHash())) {
            throw new BusinessException("La contraseña actual es incorrecta");
        }
        usuario.setPasswordHash(passwordEncoder.encode(request.getPasswordNuevo()));
        usuarioRepository.save(usuario);
        refreshTokenService.revocarTokensDeUsuario(usuario.getId());
        String nombre = usuario.getPersona().getNombre() + " " + usuario.getPersona().getApellido();
        emailService.enviarCambioPasswordExitoso(usuario.getCorreo(), nombre);
        return "Contraseña actualizada exitosamente.";
    }

    // ── 11. ME ─────────────────────────────────────────────────────────────────

    public MeResponse me(String correo) {
        Usuario usuario = usuarioRepository.findByCorreo(correo)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", 0L));
        List<UsuarioRol> roles = usuarioRolRepository.findByUsuarioId(usuario.getId());
        List<String> roleNames = roles.stream()
                .map(ur -> ur.getRol().getNombre())
                .toList();
        String nombreCompleto = usuario.getPersona().getNombre() + " " + usuario.getPersona().getApellido();
        return MeResponse.builder()
                .id(usuario.getId())
                .correo(usuario.getCorreo())
                .nombreCompleto(nombreCompleto)
                .roles(roleNames)
                .confirmado(usuario.getConfirmado())
                .fechaCreacion(usuario.getFechaCreacion())
                .build();
    }
}

