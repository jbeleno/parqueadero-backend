package com.usco.parqueaderos_api.common.controller;

import com.usco.parqueaderos_api.common.dto.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class HealthController {

    @GetMapping("/health")
    public ResponseEntity<ApiResponse<Map<String, Object>>> health() {
        Map<String, Object> info = Map.of(
                "status", "UP",
                "timestamp", LocalDateTime.now().toString(),
                "app", "parqueaderos-api"
        );
        return ResponseEntity.ok(ApiResponse.ok(info, "Servicio activo"));
    }
}
