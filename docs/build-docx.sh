#!/usr/bin/env bash
# Convierte DOCUMENTO_ACADEMICO_v2.md a .docx (Word / Google Docs compatible).
#
# Pasos:
#   1. Renderiza cada bloque ```plantuml``` a PNG con la CLI de PlantUML.
#   2. Reemplaza los placeholders [CAPTURA PENDIENTE: ...] de las figuras 13-19
#      por las imágenes ya generadas con Carbon en docs/figures/0X_*.png.
#   3. Llama a pandoc para generar el .docx.
#
# Uso:  ./docs/build-docx.sh
# Salida: DOCUMENTO_ACADEMICO_v2.docx en la raíz del repo.

set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

SRC="DOCUMENTO_ACADEMICO_v2.md"
OUT="DOCUMENTO_ACADEMICO_v2.docx"
TMP_MD="$(mktemp -t docv2.XXXXXX.md)"
FIG_PUML_DIR="docs/figures/plantuml"
mkdir -p "$FIG_PUML_DIR"

echo "[1/4] Extrayendo y renderizando bloques PlantUML…"

# Extrae cada bloque plantuml a un archivo .puml individual
python3 - <<'PY'
import re, pathlib
src = pathlib.Path("DOCUMENTO_ACADEMICO_v2.md").read_text(encoding="utf-8")
pat = re.compile(r"```plantuml\n(.*?)```", re.S)
outdir = pathlib.Path("docs/figures/plantuml")
outdir.mkdir(parents=True, exist_ok=True)
for i, m in enumerate(pat.finditer(src), start=1):
    body = m.group(1).strip()
    if not body.startswith("@startuml"):
        body = "@startuml\n" + body
    if not body.rstrip().endswith("@enduml"):
        body = body + "\n@enduml"
    (outdir / f"fig_uml_{i:02d}.puml").write_text(body, encoding="utf-8")
print(f"  → {i} bloques PlantUML extraídos a {outdir}")
PY

# Renderiza todos los .puml a .png
plantuml -tpng "$FIG_PUML_DIR"/*.puml
echo "  → PNGs generados en $FIG_PUML_DIR/"

echo "[2/4] Construyendo .md intermedio con imágenes embebidas…"

python3 - <<'PY'
import re, pathlib
src = pathlib.Path("DOCUMENTO_ACADEMICO_v2.md").read_text(encoding="utf-8")

# 1) Reemplaza ```plantuml ...``` por ![](docs/figures/plantuml/fig_uml_XX.png)
counter = [0]
def repl_uml(_m):
    counter[0] += 1
    return f"![](docs/figures/plantuml/fig_uml_{counter[0]:02d}.png)"
src = re.sub(r"```plantuml\n.*?```", repl_uml, src, flags=re.S)

# 2) Reemplaza los placeholders de las figuras 13-19 por las capturas de Carbon
mapping = {
    "Captura del método AuthService.refresh con el manejo de revocación e inserción del nuevo refresh token":
        "docs/figures/01_auth_service_refresh.png",
    "Captura del método TicketService.save con la llamada findByIdForUpdate y la validación del estado del punto":
        "docs/figures/02_ticket_service_save.png",
    "Captura del método TarifaCalculatorService.calcular ilustrando el redondeo a la fracción mínima y el cálculo del monto final":
        "docs/figures/03_tarifa_calculator.png",
    "Captura del método TicketAutoService.procesarEntrada mostrando la validación de duplicado y la creación del ticket EN_CURSO":
        "docs/figures/04_ticket_auto_procesar_entrada.png",
    "Captura del método TicketAutoService.procesarSalida ilustrando la rama que registra fechaHoraSalidaFisica cuando el ticket fue cerrado en los últimos 5 minutos":
        "docs/figures/05_ticket_auto_salida_fisica.png",
    "Captura del método NotificationListener.onTicketCreado con la fase AFTER_COMMIT y las tres invocaciones a SimpMessagingTemplate.convertAndSend":
        "docs/figures/06_notification_listener_ticket_creado.png",
    "Captura del método PlateReaderClient.detect mostrando la captura de excepciones y el return de una lista vacía":
        "docs/figures/07_plate_reader_client.png",
}
for desc, path in mapping.items():
    placeholder = f"`[CAPTURA PENDIENTE: {desc}]`"
    if placeholder in src and pathlib.Path(path).exists():
        src = src.replace(placeholder, f"![]({path})")

import sys, os
tmp = os.environ["TMP_MD"]
pathlib.Path(tmp).write_text(src, encoding="utf-8")
PY

echo "[3/4] Convirtiendo .md → .docx con pandoc…"

pandoc "$TMP_MD" \
  --from markdown+pipe_tables+table_captions+grid_tables+raw_html \
  --to docx \
  --toc \
  --toc-depth=3 \
  --output "$OUT"

rm -f "$TMP_MD"

echo "[4/4] Listo."
echo ""
ls -lh "$OUT"
echo ""
echo "Para abrir:"
echo "  open '$OUT'                    # Microsoft Word / LibreOffice"
echo "  drive.google.com → New → Upload  # luego clic derecho → Open with Google Docs"
