package com.usco.parqueaderos_api.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.List;

@Configuration
public class CorsConfig {

    @Bean
    public CorsFilter corsFilter() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        CorsConfiguration config = new CorsConfiguration();
        
        // Permite credenciales (cookies, headers de autorización)
        config.setAllowCredentials(true);
        
        // orígenes permitidos
        config.setAllowedOrigins(List.of(
                "http://localhost:3000",
                "http://localhost:3001",
                "http://localhost:5173", // Vite default
                "*" // TODO: En producción, reemplazar con el dominio real si no se usa wildcard
        ));
        
        // Headers permitidos
        config.setAllowedHeaders(List.of(
                "Origin", 
                "Content-Type", 
                "Accept", 
                "Authorization",
                "X-Requested-With",
                "Access-Control-Request-Method",
                "Access-Control-Request-Headers"
        ));
        
        // Métodos permitidos
        config.setAllowedMethods(List.of(
                "GET", 
                "POST", 
                "PUT", 
                "PATCH", 
                "DELETE", 
                "OPTIONS"
        ));
        
        // Configura el CORS para todas las rutas
        source.registerCorsConfiguration("/**", config);
        return new CorsFilter(source);
    }
}
