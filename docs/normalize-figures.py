"""
Normaliza el formato de Figuras y Tablas en los .md académicos:
1. Cada figura: **Figura N.** + título cursiva + cuerpo + *Fuente: ...*
   (elimina la línea "Figura N. Título" duplicada debajo de la imagen)
2. Cada tabla: **Tabla N** + título cursiva + cuerpo + *Fuente: ...*
3. Renumera secuencialmente desde 1 dentro de cada archivo.
4. Quita prefijos tipo "C.1", "D.1" en favor de "1".

Uso:
    python3 docs/normalize-figures.py archivo1.md [archivo2.md ...]
"""

import re
import sys
import pathlib


def normalize(text: str) -> str:
    # 1) Detectar bloques **Figura X.** y renumerar.
    fig_counter = [0]
    tab_counter = [0]

    def fig_repl(m: re.Match) -> str:
        fig_counter[0] += 1
        return f"**Figura {fig_counter[0]}.**"

    def tab_repl(m: re.Match) -> str:
        tab_counter[0] += 1
        return f"**Tabla {tab_counter[0]}**"

    # **Figura D.0** / **Figura D.1** / **Figura C.20** / **Figura 1.**
    text = re.sub(
        r"\*\*Figura\s+[A-Z]?\.?\s*\d+\.?\*\*",
        fig_repl,
        text,
    )
    text = re.sub(
        r"\*\*Tabla\s+[A-Z]?\.?\s*\d+\*\*",
        tab_repl,
        text,
    )

    # 2) Eliminar la línea de caption duplicada bajo cada figura:
    #    *Figura 1. Texto del título.*
    text = re.sub(
        r"^\*Figura\s+[A-Z]?\.?\s*\d+\.?\s+.*?\.\*\s*$",
        "",
        text,
        flags=re.M,
    )

    # 3) Compactar bloques de líneas vacías (>2 → 1).
    text = re.sub(r"\n\n\n+", "\n\n", text)

    return text


def main():
    if len(sys.argv) < 2:
        print("Uso: python3 normalize-figures.py archivo.md [...]")
        sys.exit(1)

    for arg in sys.argv[1:]:
        p = pathlib.Path(arg)
        if not p.exists():
            print(f"  ✗ no existe: {arg}")
            continue
        before = p.read_text(encoding="utf-8")
        after = normalize(before)
        if before == after:
            print(f"  = sin cambios: {arg}")
        else:
            p.write_text(after, encoding="utf-8")
            # contar figuras y tablas finales
            n_fig = len(re.findall(r"^\*\*Figura \d+\.\*\*", after, re.M))
            n_tab = len(re.findall(r"^\*\*Tabla \d+\*\*", after, re.M))
            print(f"  ✓ normalizado: {arg}  ({n_fig} figuras, {n_tab} tablas)")


if __name__ == "__main__":
    main()
