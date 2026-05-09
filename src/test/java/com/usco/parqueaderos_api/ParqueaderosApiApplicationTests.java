package com.usco.parqueaderos_api;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@Disabled("Requiere Postgres real. Para correrlo: levantar docker compose up -d db y descomentar.")
class ParqueaderosApiApplicationTests {

    @Test
    void contextLoads() {
    }
}
