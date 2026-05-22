#!/usr/bin/env python3
"""
ocr_parqueadero — Módulo de lectura de placas para sistema de parqueadero.

GANADOR del benchmark: PaddleOCR + majority voting + filtro estricto.
- Consistencia: 100% (12/12 placas dan el MISMO string en entrada y salida)
- Accuracy literal: 83% (10/12 correctas; los 2 errores son Q→O consistentes)

Uso desde otro módulo:
    from src.ocr_parqueadero import ParqueaderoOCR
    ocr = ParqueaderoOCR()
    plate = ocr.read("/path/to/plate_crop.jpg")
    # → "KJV807" o None si no detectó nada

Integración con YOLO:
    from ultralytics import YOLO
    from src.ocr_parqueadero import ParqueaderoOCR
    detector = YOLO("models/detector/yolo11n_placas.pt")
    ocr = ParqueaderoOCR()
    # ... en el loop de captura:
    results = detector.predict(frame, conf=0.25)
    for box in results[0].boxes.xyxy:
        x1, y1, x2, y2 = map(int, box)
        crop = frame[y1:y2, x1:x2]
        plate = ocr.read_array(crop)
"""
import re
import tempfile
from collections import Counter
from pathlib import Path
from typing import List, Optional, Tuple

import cv2
import numpy as np


class ParqueaderoOCR:
    """Lector de placas optimizado para parqueadero (PaddleOCR + voting)."""

    PLATE_REGEX = re.compile(r"([A-Z]{3})\s*[\.\-·]?\s*(\d{3})")
    PLATE_LOOSE_REGEX = re.compile(r"([A-Z0]{3})\s*[\.\-·]?\s*(\d{3})")

    def __init__(self, lang: str = "en"):
        from paddleocr import PaddleOCR
        self.ocr = PaddleOCR(use_textline_orientation=True, lang=lang)

    def read(self, image_path: str) -> Optional[str]:
        """Lee placa desde archivo. Devuelve string ABC123 o None."""
        img = cv2.imread(str(image_path))
        if img is None:
            return None
        return self.read_array(img)

    def read_array(self, img: np.ndarray) -> Optional[str]:
        """Lee placa desde array numpy BGR. Devuelve string ABC123 o None."""
        readings = self._read_with_voting(img)
        if not readings:
            return None
        most_common, _count = Counter(readings).most_common(1)[0]
        return most_common

    def read_with_confidence(self, img: np.ndarray) -> Tuple[Optional[str], float]:
        """Lee placa y devuelve (placa, confianza 0-1) según voting.
        confianza = (votos del ganador) / (lecturas válidas totales).
        """
        readings = self._read_with_voting(img)
        if not readings:
            return None, 0.0
        most_common, count = Counter(readings).most_common(1)[0]
        return most_common, count / len(readings)

    def _read_with_voting(self, img: np.ndarray) -> List[str]:
        """3 lecturas con preprocessing distinto + filtro estricto."""
        variants = self._preprocess_variants(img)
        readings = []
        for variant in variants:
            with tempfile.NamedTemporaryFile(suffix=".jpg", delete=False) as f:
                cv2.imwrite(f.name, variant)
                try:
                    result = self.ocr.predict(f.name)
                finally:
                    Path(f.name).unlink(missing_ok=True)
            text = self._parse_result(result)
            plate = self._extract_plate(text)
            if plate:
                readings.append(plate)
        return readings

    @staticmethod
    def _preprocess_variants(img: np.ndarray) -> List[np.ndarray]:
        """3 versiones distintas para robustez ante variaciones de luz/ruido."""
        return [
            img,  # original
            cv2.convertScaleAbs(img, alpha=1.15, beta=10),  # algo más brillante
            cv2.bilateralFilter(img, 9, 75, 75),  # suavizado
        ]

    @staticmethod
    def _parse_result(result) -> str:
        if not result:
            return ""
        r = result[0]
        if isinstance(r, dict):
            return " ".join(r.get("rec_texts", []))
        return ""

    @classmethod
    def _extract_plate(cls, text: str) -> str:
        """Extrae formato colombiano ABC123, descarta ciudades y basuras."""
        if not text:
            return ""
        upper = text.upper().replace("-", "").replace("·", "").replace(".", "")
        # Estricto: 3 letras + 3 dígitos
        m = cls.PLATE_REGEX.search(upper)
        if m:
            return m.group(1) + m.group(2)
        # Loose: aceptar 0 como letra (OCR a veces confunde Q→0)
        m = cls.PLATE_LOOSE_REGEX.search(upper)
        if m:
            letters = m.group(1).replace("0", "O")
            if re.fullmatch(r"[A-Z]{3}", letters):
                return letters + m.group(2)
        return ""


# ============= CLI demo =============

def _demo():
    import sys
    if len(sys.argv) < 2:
        print("Uso: ocr_parqueadero.py <ruta_imagen>")
        sys.exit(1)
    ocr = ParqueaderoOCR()
    img_path = sys.argv[1]
    plate, conf = ocr.read_with_confidence(cv2.imread(img_path))
    if plate:
        print(f"Placa: {plate}  (confianza voting: {conf:.2f})")
    else:
        print("No se detectó placa válida")


if __name__ == "__main__":
    _demo()
