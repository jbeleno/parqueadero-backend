package com.usco.parqueaderos_api.auth.controller;

import com.usco.parqueaderos_api.auth.dto.*;
import com.usco.parqueaderos_api.auth.service.AuthService;
import com.usco.parqueaderos_api.common.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /** 1. Registro de nueva cuenta */
    @PostMapping("/registro")
    public ResponseEntity<ApiResponse<String>> registro(@Valid @RequestBody RegistroRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(authService.registro(request), "Registro exitoso"));
    }

    /** 2. Login */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody AuthRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(authService.login(request), "Login exitoso"));
    }

    /** 3. Renovar access token con refresh token */
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(@Valid @RequestBody RefreshRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(authService.refresh(request), "Token renovado"));
    }

    /** 4. Logout — revocar refresh token */
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(@Valid @RequestBody RefreshRequest request) {
        authService.logout(request);
        return ResponseEntity.ok(ApiResponse.ok(null, "Sesión cerrada exitosamente"));
    }

    /** 5. Confirmar cuenta con PIN */
    @PostMapping("/confirmar-cuenta")
    public ResponseEntity<ApiResponse<String>> confirmarCuenta(@Valid @RequestBody VerificarPinRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(authService.confirmarCuenta(request), "Cuenta confirmada"));
    }

    /** 6. Reenviar PIN de confirmación */
    @PostMapping("/reenviar-confirmacion")
    public ResponseEntity<ApiResponse<String>> reenviarConfirmacion(@Valid @RequestBody PinRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(authService.reenviarConfirmacion(request), "PIN reenviado"));
    }

    /** 7. Solicitar PIN de recuperación de contraseña */
    @PostMapping("/olvide-password")
    public ResponseEntity<ApiResponse<String>> olvidePassword(@Valid @RequestBody PinRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(authService.olvidePassword(request), "Solicitud procesada"));
    }

    /** 8. Verificar PIN (sin cambiar contraseña aún) */
    @PostMapping("/verificar-pin")
    public ResponseEntity<ApiResponse<String>> verificarPin(@Valid @RequestBody VerificarPinRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(authService.verificarPin(request), "PIN válido"));
    }

    /** 9. Resetear contraseña usando PIN */
    @PostMapping("/resetear-password")
    public ResponseEntity<ApiResponse<String>> resetearPassword(@Valid @RequestBody ResetearPasswordRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(authService.resetearPassword(request), "Contraseña actualizada"));
    }

    /** 10. Cambiar contraseña (requiere JWT) */
    @PutMapping("/cambiar-password")
    public ResponseEntity<ApiResponse<String>> cambiarPassword(
            @Valid @RequestBody CambiarPasswordRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(ApiResponse.ok(
                authService.cambiarPassword(userDetails.getUsername(), request), "Contraseña actualizada"));
    }

    /** 11. Datos del usuario autenticado */
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<MeResponse>> me(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(ApiResponse.ok(authService.me(userDetails.getUsername()), "Usuario autenticado"));
    }
}

