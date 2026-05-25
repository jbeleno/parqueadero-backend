"""
Genera la gráfica comparativa del benchmark de OCR para la Figura del
documento académico (sección 3.3.8).

Datos: Tabla 16 del DOCUMENTO_ACADEMICO_v2.md — benchmark sobre 60 imágenes
(12 placas × 5 variaciones) ejecutado durante la pasantía.

Ejecución:
    python3 docs/snippets/generate_benchmark_chart.py

Salida:
    docs/figures/figura_benchmark_ocr.png
"""

from pathlib import Path
import matplotlib.pyplot as plt
import numpy as np

# ── Datos del benchmark (Tabla 16) ────────────────────────────────────────
soluciones = [
    "PaddleOCR v2\n(voting + regex)",
    "EasyOCR",
    "PaddleOCR\nvanilla",
    "Tesseract",
    "TrOCR\nsmall-printed",
    "docTR\n(Mindee)",
    "ResNet50 ep20\n(propio)",
]

# Consistencia (%): 5 variaciones de la misma placa producen el mismo string
consistencia = [100, 67, 58, 0, 8, 0, 0]

# Mayoría correcta (%): ≥3 de 5 lecturas coinciden con el ground truth
mayoria_correcta = [83, 50, 83, 17, 0, 0, 0]

# Latencia (s/img) — None significa "no aplicable / descartado"
latencia = [2.4, 0.2, 0.8, 0.1, 0.07, 0.9, None]

# ── Configuración del gráfico ─────────────────────────────────────────────
plt.rcParams.update({
    "font.family": "DejaVu Sans",
    "font.size": 11,
    "axes.titlesize": 13,
    "axes.labelsize": 11,
    "figure.dpi": 150,
})

fig, ax = plt.subplots(figsize=(12, 6))

x = np.arange(len(soluciones))
width = 0.36

# Barras de consistencia
bars1 = ax.bar(
    x - width / 2, consistencia, width,
    label="Consistencia (5/5 variaciones idénticas)",
    color="#2563eb", edgecolor="#1e3a8a", linewidth=0.6,
)
# Barras de mayoría correcta
bars2 = ax.bar(
    x + width / 2, mayoria_correcta, width,
    label="Precisión por mayoría (≥3/5 vs. ground truth)",
    color="#f59e0b", edgecolor="#92400e", linewidth=0.6,
)

# Etiquetas numéricas encima de cada barra
def label_bars(bars, valores):
    for b, v in zip(bars, valores):
        ax.text(
            b.get_x() + b.get_width() / 2, b.get_height() + 1.5,
            f"{v}%", ha="center", va="bottom", fontsize=9,
        )

label_bars(bars1, consistencia)
label_bars(bars2, mayoria_correcta)

# Líneas guía horizontales
ax.set_axisbelow(True)
ax.yaxis.grid(True, color="#e5e7eb", linewidth=0.8)
ax.set_ylim(0, 115)
ax.set_yticks(range(0, 101, 20))

# Resaltar al ganador (PaddleOCR v2)
ax.axvspan(-0.5, 0.5, color="#dcfce7", alpha=0.4, zorder=0)
ax.text(0, 108, "Ganador", ha="center", fontsize=10,
        fontweight="bold", color="#15803d")

# Ejes y título
ax.set_xticks(x)
ax.set_xticklabels(soluciones, fontsize=9)
ax.set_ylabel("Porcentaje sobre 12 placas reales")
ax.set_title(
    "Benchmark de soluciones de OCR sobre 60 imágenes (12 placas × 5 variaciones)",
    pad=14, fontweight="bold",
)

# Anotaciones de latencia debajo de cada par de barras
for xi, lat in zip(x, latencia):
    texto = f"{lat:.2f} s/img" if lat is not None else "descartado"
    ax.text(xi, -8, texto, ha="center", fontsize=8, color="#374151",
            style="italic")

# Margen inferior para las anotaciones
plt.subplots_adjust(bottom=0.20)

# Leyenda
ax.legend(loc="upper right", framealpha=0.95, fontsize=10)

# Borde minimalista
for spine in ("top", "right"):
    ax.spines[spine].set_visible(False)

# ── Guardar ───────────────────────────────────────────────────────────────
out = Path("docs/figures/figura_benchmark_ocr.png")
out.parent.mkdir(parents=True, exist_ok=True)
fig.savefig(out, bbox_inches="tight", dpi=200)
print(f"OK → {out}  ({out.stat().st_size / 1024:.0f} KB)")
