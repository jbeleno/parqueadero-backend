package com.usco.parqueaderos_api.tariff.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Smoke test contra la queja del front: "la respuesta del POST no trae los 5 campos
 * del Modelo B". Verifica que TarifaDTO serializa esos campos cuando se le setean.
 */
class TarifaDTOSerializationTest {

    private final ObjectMapper json = new ObjectMapper();

    @Test
    @DisplayName("TarifaDTO con Modelo B + IVA + suscripciones serializa los 7 campos nuevos")
    void serializa_modelo_b() throws Exception {
        TarifaDTO dto = new TarifaDTO();
        dto.setId(16L);
        dto.setNombre("test modelo b");
        dto.setValor(3000.0);
        dto.setUnidad("Hora");
        dto.setMinutosFraccion(5);
        dto.setParqueaderoId(9L);
        dto.setParqueaderoNombre("Parqueadero zz");
        dto.setTipoVehiculoId(1L);
        dto.setTipoVehiculoNombre("Carro");

        // Los 7 campos que el front reclama:
        dto.setMinutosGracia(10);
        dto.setValorMinimo(2000.0);
        dto.setMinutosCubiertosPorMinimo(30);
        dto.setAplicaIva(true);
        dto.setIvaPorcentaje(19.0);
        dto.setPrecioMensualidad(150000.0);
        dto.setPrecioPaseDia(10000.0);

        String out = json.writeValueAsString(dto);

        assertTrue(out.contains("\"minutosGracia\":10"),                    "minutosGracia ausente: " + out);
        assertTrue(out.contains("\"valorMinimo\":2000"),                    "valorMinimo ausente: " + out);
        assertTrue(out.contains("\"minutosCubiertosPorMinimo\":30"),         "minutosCubiertosPorMinimo ausente: " + out);
        assertTrue(out.contains("\"aplicaIva\":true"),                      "aplicaIva ausente: " + out);
        assertTrue(out.contains("\"ivaPorcentaje\":19"),                    "ivaPorcentaje ausente: " + out);
        assertTrue(out.contains("\"precioMensualidad\":150000"),            "precioMensualidad ausente: " + out);
        assertTrue(out.contains("\"precioPaseDia\":10000"),                 "precioPaseDia ausente: " + out);
    }
}
