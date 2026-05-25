# Snippets de código para las figuras del documento académico

Este directorio contiene los 7 fragmentos de código que aparecen como placeholders `[CAPTURA PENDIENTE]` en `DOCUMENTO_ACADEMICO_v2.md`. Cada archivo es un extracto fiel del código real del backend, simplificado para caber en una imagen tipo Carbon sin perder legibilidad.

## Mapeo snippet → figura

| Archivo | Figura | Sección del doc | Origen real |
|---|---|---|---|
| `01_auth_service_refresh.java` | Figura 13 | 3.4.1 | `AuthService.java:200-227` |
| `02_ticket_service_save.java` | Figura 14 | 3.4.2 | `TicketService.java:85-133` |
| `03_tarifa_calculator.java` | Figura 15 | 3.4.3 | `TarifaCalculatorService.java:31-66` |
| `04_ticket_auto_procesar_entrada.java` | Figura 16 | 3.4.4 | `TicketAutoService.java:121-172` |
| `05_ticket_auto_salida_fisica.java` | Figura 17 | 3.4.5 | `TicketAutoService.java:176-234` |
| `06_notification_listener_ticket_creado.java` | Figura 18 | 3.4.6 | `NotificationEventListener.java:37-52` |
| `07_plate_reader_client.java` | Figura 19 | 3.4.7 | `PlateReaderClient.java:54-81` |

## Opción A — Generar imágenes localmente con `silicon` (recomendado)

`silicon` es un CLI en Rust que renderiza imágenes tipo Carbon **localmente** (sin browser, sin internet). Sale en PNG con sintaxis resaltada, sombra y padding configurables.

**Instalación (macOS):**

```bash
brew install silicon
```

**Generar las 7 imágenes de una sola vez:**

```bash
cd /Users/jesus/Desktop/parqueaderos-api

for f in docs/snippets/*.java; do
  name=$(basename "$f" .java)
  silicon "$f" \
    --language java \
    --theme "Dracula" \
    --background "#1e1e2e" \
    --no-window-controls \
    --pad-horiz 40 --pad-vert 30 \
    --font "JetBrains Mono=20" \
    --output "docs/figures/${name}.png"
done
```

Resultado: `docs/figures/01_auth_service_refresh.png`, etc.

**Variantes que puedes probar:**

- Tema claro y limpio para impresión: `--theme "OneHalfLight" --background "#ffffff"`
- Con line numbers visibles (silicon los incluye por defecto, agrega `--no-line-number` para ocultarlos)
- Sombra suave para look Carbon: agrega `--shadow-blur-radius 30 --shadow-color "#00000080"`

## Opción B — Pegar manualmente en carbon.now.sh

1. Abre https://carbon.now.sh/
2. Configura: lenguaje Java, tema "Dracula" o "Material", "Hide window controls" activado.
3. Por cada archivo de `docs/snippets/`: copia el contenido, pégalo en Carbon, y exporta como PNG con el mismo nombre base.

## Opción C — `freeze` de Charm (alternativa con TUI)

```bash
brew install charmbracelet/tap/freeze

for f in docs/snippets/*.java; do
  name=$(basename "$f" .java)
  freeze "$f" --language java --theme "dracula" \
    --output "docs/figures/${name}.png"
done
```

## Después de generar las imágenes

Reemplaza los 7 placeholders `[CAPTURA PENDIENTE: ...]` en `DOCUMENTO_ACADEMICO_v2.md` por:

```markdown
![Figura 13](docs/figures/01_auth_service_refresh.png)
```

(y análogos para las otras 6). El bloque ya tiene el caption y la línea *Fuente: Elaboración propia* alrededor, solo necesitas insertar la línea de imagen.

## Capturas pendientes adicionales (no son código)

Estos placeholders **no** son snippets — requieren capturas reales:

- **Figura 4 (sección 3.2.2):** captura de Swagger UI en `http://deploy.inmero.co:5445/swagger-ui.html`. Toma la pantalla con los 16 controladores colapsados visibles. Nombre sugerido: `docs/figures/figura_04_swagger_ui.png`.
- **Gráfica matplotlib del benchmark (sección 3.3.8):** ejecuta el script de benchmark del directorio `ocr/` que genera el gráfico comparativo de las 7 soluciones de OCR. Nombre sugerido: `docs/figures/figura_benchmark_ocr.png`.
