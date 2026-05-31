# 🎥 Webcam Tester — Tester de OCR + WebSocket

Archivo: [`webcam-tester.html`](./webcam-tester.html)

Una página HTML standalone (1 solo archivo, sin build, sin dependencias locales) que simula el frontend del sistema para **probar el pipeline OCR + WebSocket en vivo** durante la presentación o el desarrollo.

---

## ¿Qué hace?

| # | Sección | Función |
|---|---|---|
| 1 | **Login** | Autentica con `correo`/`password` y guarda el JWT |
| 2 | **Parqueadero y cámaras** | Lista las cámaras del parqueadero (incluye `tipo: ENTRADA/SALIDA`) |
| 3 | **Webcam** | Usa tu webcam laptop como si fuera la cámara seleccionada — captura un frame cada N segundos y lo sube al backend |
| 3.b | **Subir foto manual** | Selector de archivo para subir fotos reales de placas (no necesita webcam) |
| 4 | **WebSocket** | Conecta a `/ws` con STOMP y se suscribe a `/topic/parqueadero/{id}` — muestra TODOS los eventos en vivo |
| 5 | **Placas detectadas** | Render visual de cada placa que el sidecar OCR reconoce |
| 6 | **Log** | Trace cronológico de cada acción (color-coded: ok/err/info) |

Sirve para **demostrar el flujo end-to-end** sin necesidad del front "real":
**Webcam laptop → backend → sidecar OCR (YOLO+PaddleOCR) → evento WebSocket → render en vivo**.

---

## 🚀 Cómo levantarlo (3 opciones)

### Opción 1: Doble click (más simple)

```bash
open docs/tester/webcam-tester.html
```

Se abre directo en tu navegador. **No requiere servidor local.**

> Limitación: algunos navegadores (Chrome estricto) bloquean `navigator.mediaDevices.getUserMedia()` cuando la página se sirve via `file://`. Si la webcam no arranca, usa la Opción 2.

### Opción 2: Servidor HTTP estático local

```bash
cd docs/tester
python3 -m http.server 8000
# Abrir: http://localhost:8000/webcam-tester.html
```

Esto hace que el navegador trate la página como un origen HTTP válido y permita acceso a la webcam.

### Opción 3: Subida temporal (para mostrar en otra máquina)

Subí el archivo a cualquier hosting estático (Vercel, Netlify, GitHub Pages, ngrok). No tiene dependencias del backend más allá de la URL del API.

---

## 🔧 Configuración

Por defecto apunta a **producción**:

```javascript
const API = "https://parqueadero-api-useune-c1a095-158-69-200-27.traefik.me";
const WS  = "https://parqueadero-api-useune-c1a095-158-69-200-27.traefik.me/ws";
```

Para apuntar a **localhost** (desarrollo), editar líneas 172-173 del HTML:

```javascript
const API = "http://localhost:8080";
const WS  = "http://localhost:8080/ws";
```

### Defaults

| Campo | Valor | Por qué |
|---|---|---|
| Email | `admin@test.com` | Usuario semilla con rol ADMIN |
| Password | `admin123` | Idem |
| Parqueadero ID | `8` | **PARQUEADERO CENTRAL ECONOMIA** — tiene 2 cámaras configuradas (ENTRADA + SALIDA), perfecto para demo |
| Intervalo captura | 3 segundos | Sube un frame cada 3s mientras la webcam esté activa |

### Parqueaderos con cámaras configuradas (prod actual)

| ID | Nombre | Cámaras |
|---|---|---|
| 2 | USCO Sede Central | 1 |
| 3 | USCO Postgrados | 1 |
| 7 | PARQUEADERO USCO PRUEBA | 1 |
| **8** | **PARQUEADERO CENTRAL ECONOMIA** | **2** (ENTRADA + SALIDA) ⭐ recomendado |
| 9 | Parqueadero zz | 2 |

---

## 🎬 Guion para la presentación

### Setup (antes de empezar la demo)

1. Abrir `docs/tester/webcam-tester.html` en Chrome/Firefox.
2. Login con `admin@test.com` / `admin123` → ver "OK · roles: ADMIN" en verde.
3. Cargar cámaras del parqueadero **8** → aparecen 2 cámaras (ENTRADA + SALIDA).
4. Seleccionar la cámara `cam 18 — Cámara 1 (ENTRADA)`.
5. Click "Conectar WS" → ver en el log "WS conectado al parq 8" en verde.

### Demo en vivo

**Opción A: con webcam de la laptop**
1. Click "Iniciar webcam" → muestra el preview.
2. Click "Auto-enviar cada 3s" → cada 3 segundos sube un frame.
3. Apuntá la webcam a una placa real (impresa o en una pantalla).
4. En el panel "Placas detectadas" aparece la lectura del OCR.
5. En el log de WebSocket aparecen los eventos `placa-detectada`, `ticket-creado`, `ticket-cerrado`.

**Opción B: subir foto manual**
1. Click "Seleccionar archivo" → eligir una foto JPG de una placa colombiana.
2. La placa aparece en el panel de detectadas + evento WebSocket.

### Lo que el tutor ve

- **Pipeline end-to-end funcionando**: cámara → OCR → BD → WebSocket → UI.
- **Latencia visible**: típicamente 2-4 segundos del click al evento WS (depende del modelo y la BD).
- **Trazabilidad**: cada evento queda registrado en `audit_log` (mostrable luego en psql).
- **Multi-cámara**: cambiar entre ENTRADA y SALIDA y ver cómo el backend distingue el flujo (entrada crea ticket, salida lo cierra).

---

## 🐛 Troubleshooting

| Síntoma | Causa | Fix |
|---|---|---|
| "Cannot access webcam" en consola | Navegador bloquea `file://` | Usar Opción 2 (servidor local) |
| 401 al login | Token expiró o credenciales mal | Re-login |
| No aparecen cámaras tras "Cargar" | Parqueadero sin cámaras configuradas | Cambiar a parq 8 (ENTRADA+SALIDA) o parq 9 |
| WS no conecta | URL del WS mal o backend caído | Verificar `const WS` en línea 173, probar `curl <BASE>/api/health` |
| Foto sube pero no llega evento WS | Sidecar OCR caído | Revisar logs del contenedor `ocr` en Dokploy |
| Foto sube pero el OCR no detecta nada | Calidad/iluminación de la imagen | Usar imágenes con placa nítida, contraste alto, formato colombiano `ABC123` o `ABC12D` |

---

## 📦 Dependencias del HTML (cargadas desde CDN, no requieren install)

```html
<script src="https://cdn.jsdelivr.net/npm/sockjs-client@1.6.1/dist/sockjs.min.js"></script>
<script src="https://cdn.jsdelivr.net/npm/@stomp/stompjs@7.0.0/bundles/stomp.umd.min.js"></script>
```

> Necesitas conexión a internet la primera vez que abrís la página (después podrías cachearlas con Service Worker si querés offline).

---

## 📂 Endpoints del backend que usa el tester

| Método | Path | Para qué |
|---|---|---|
| `POST` | `/api/auth/login` | Obtener JWT |
| `GET` | `/api/parqueaderos/{id}/configuracion` | Listar cámaras del parqueadero |
| `POST` | `/api/camaras/{id}/imagen` | Subir frame de webcam o foto manual (multipart) |
| `WS` | `/ws` (SockJS+STOMP) | Conectarse al broker |
| `SUB` | `/topic/parqueadero/{id}` | Recibir eventos en vivo (placa-detectada, ticket-creado, etc.) |

Todos usan `Authorization: Bearer <jwt>` excepto el WS handshake (token va en query).
