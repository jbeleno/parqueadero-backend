"""
Convierte un anexo Markdown con bloques ```plantuml``` en un .md drive-ready
con los diagramas embebidos como imágenes base64 inline.

Para cada bloque ```plantuml ... ``` del archivo de entrada:
  1. Renderiza el bloque a PNG (200 DPI) con la CLI de PlantUML.
  2. Lo codifica en base64 y reemplaza el bloque por
     ![](data:image/png;base64,...)

Uso:
    python3 docs/build_anexo_drive_md.py <anexo.md> [<anexo.md> ...]

Salida:
    parqueaderos-docs-referencia/SUBIR_A_DRIVE/anexos/<basename>_DRIVE.md
"""

import base64
import pathlib
import re
import subprocess
import sys
import tempfile

OUT_DIR = pathlib.Path(
    "/Users/jesus/Desktop/parqueaderos-api/parqueaderos-docs-referencia/SUBIR_A_DRIVE/anexos"
)
OUT_DIR.mkdir(parents=True, exist_ok=True)


def render_plantuml_to_b64(body: str) -> str | None:
    with tempfile.TemporaryDirectory() as tmpdir:
        puml = pathlib.Path(tmpdir) / "d.puml"
        b = body.strip()
        if not b.startswith("@startuml"):
            b = "@startuml\n" + b
        if not b.rstrip().endswith("@enduml"):
            b = b + "\n@enduml"
        puml.write_text(b, encoding="utf-8")
        try:
            subprocess.run(
                ["plantuml", "-tpng", "-Sdpi=200", str(puml)],
                check=True,
                capture_output=True,
                cwd=tmpdir,
            )
        except subprocess.CalledProcessError as exc:
            sys.stderr.write(
                f"  ⚠ plantuml falló: {exc.stderr.decode(errors='replace')[:300]}\n"
            )
            return None
        png = puml.with_suffix(".png")
        if not png.exists():
            return None
        return base64.b64encode(png.read_bytes()).decode("ascii")


def process_file(src: pathlib.Path) -> pathlib.Path:
    text = src.read_text(encoding="utf-8")
    pat = re.compile(r"```plantuml\n(.*?)```", re.S)

    counter = [0]

    def repl(m: re.Match) -> str:
        counter[0] += 1
        b64 = render_plantuml_to_b64(m.group(1))
        if b64 is None:
            return m.group(0)
        kb = len(b64) // 1024
        print(f"  ✓ figura {counter[0]:02d}  ({kb:5d} KB base64)")
        return f"![](data:image/png;base64,{b64})"

    new_text = pat.sub(repl, text)
    # Compactar líneas vacías
    new_text = re.sub(r"\n{4,}", "\n\n\n", new_text)

    dst = OUT_DIR / (src.stem + "_DRIVE.md")
    dst.write_text(new_text, encoding="utf-8")
    return dst


def main():
    if len(sys.argv) < 2:
        print("Uso: python3 build_anexo_drive_md.py <archivo.md> [...]")
        sys.exit(1)
    for arg in sys.argv[1:]:
        src = pathlib.Path(arg)
        if not src.exists():
            print(f"  ✗ no existe: {arg}")
            continue
        print(f"\n→ procesando {src.name}…")
        dst = process_file(src)
        sz = dst.stat().st_size / 1024 / 1024
        print(f"  ✓ {dst}  ({sz:.1f} MB)")


if __name__ == "__main__":
    main()
