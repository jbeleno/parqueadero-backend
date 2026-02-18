package com.usco.parqueaderos_api.auth.controller;

import com.usco.parqueaderos_api.auth.dto.AuthRequest;
import com.usco.parqueaderos_api.auth.dto.AuthResponse;
import com.usco.parqueaderos_api.auth.service.AuthService;
import com.usco.parqueaderos_api.common.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@RequestBody AuthRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(authService.login(request), "Login exitoso"));
    }
}
