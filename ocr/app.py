"""
OCR sidecar para parqueaderos-api.

Endpoint unico: POST /read-plate
- Recibe el frame completo de la camara
- YOLO detecta las placas
- PaddleOCR lee cada placa con majority voting + filtro regex
- Aplica cooldown 30s por (camara_id, placa) para no procesar duplicados

Variables de entorno:
  COOLDOWN_SECONDS (default 30)
  YOLO_MODEL_PATH  (default yolo11n_placas.pt)
  DETECTOR_CONF    (default 0.25)
  MIN_VOTING_CONF  (default 0.66)
"""
import io
import logging
import os
import time
from typing import List, Optional, Tuple

import cv2
import numpy as np
from fastapi import FastAPI, Form, HTTPException, UploadFile, File
from pydantic import BaseModel
from ultralytics import YOLO

from ocr_parqueadero import ParqueaderoOCR

logging.basicConfig(level=logging.INFO, format="%(asctime)s %(levelname)s %(message)s")
log = logging.getLogger("ocr-sidecar")

COOLDOWN_SECONDS = int(os.getenv("COOLDOWN_SECONDS", "30"))
YOLO_MODEL_PATH = os.getenv("YOLO_MODEL_PATH", "yolo11n_placas.pt")
DETECTOR_CONF = float(os.getenv("DETECTOR_CONF", "0.25"))
MIN_VOTING_CONF = float(os.getenv("MIN_VOTING_CONF", "0.66"))

app = FastAPI(title="OCR Sidecar")

log.info("Cargando YOLO desde %s ...", YOLO_MODEL_PATH)
detector = YOLO(YOLO_MODEL_PATH)
log.info("Cargando PaddleOCR ...")
ocr = ParqueaderoOCR()
log.info("Listo. Cooldown=%ds, det_conf=%s, voting_conf=%s",
         COOLDOWN_SECONDS, DETECTOR_CONF, MIN_VOTING_CONF)

# Cache en memoria para cooldown: (camara_id, placa) -> timestamp_segundos
_cooldown: dict[tuple[int, str], float] = {}


def _is_in_cooldown(camara_id: int, placa: str) -> bool:
    now = time.time()
    last = _cooldown.get((camara_id, placa))
    if last is None:
        return False
    if now - last < COOLDOWN_SECONDS:
        return True
    return False


def _mark_seen(camara_id: int, placa: str) -> None:
    _cooldown[(camara_id, placa)] = time.time()
    # cleanup ocasional: si crece a >5000 entradas, purga las expiradas
    if len(_cooldown) > 5000:
        now = time.time()
        expired = [k for k, v in _cooldown.items() if now - v > COOLDOWN_SECONDS]
        for k in expired:
            _cooldown.pop(k, None)


class PlacaDetectada(BaseModel):
    placa: str
    confianza: float
    bbox: List[int]   # [x1, y1, x2, y2]
    skipped_cooldown: bool = False


class ReadPlateResponse(BaseModel):
    placas: List[PlacaDetectada]
    skipped_cooldown: List[str]   # placas detectadas pero en cooldown


@app.get("/health")
def health():
    return {"status": "ok", "cooldown_seconds": COOLDOWN_SECONDS}


@app.post("/read-plate", response_model=ReadPlateResponse)
async def read_plate(
    camara_id: int = Form(...),
    file: UploadFile = File(...),
):
    """Detecta y lee placas en el frame. Aplica cooldown por (camara_id, placa)."""
    try:
        raw = await file.read()
        if not raw:
            raise HTTPException(status_code=400, detail="archivo vacio")
        arr = np.frombuffer(raw, dtype=np.uint8)
        img = cv2.imdecode(arr, cv2.IMREAD_COLOR)
        if img is None:
            raise HTTPException(status_code=400, detail="imagen invalida")

        results = detector.predict(img, conf=DETECTOR_CONF, verbose=False)
        if not results:
            return ReadPlateResponse(placas=[], skipped_cooldown=[])

        detected: List[PlacaDetectada] = []
        skipped: List[str] = []
        boxes = results[0].boxes.xyxy.cpu().numpy() if results[0].boxes is not None else []

        for box in boxes:
            x1, y1, x2, y2 = map(int, box)
            crop = img[y1:y2, x1:x2]
            if crop.size == 0:
                continue
            placa, conf = ocr.read_with_confidence(crop)
            if not placa or conf < MIN_VOTING_CONF:
                continue
            if _is_in_cooldown(camara_id, placa):
                skipped.append(placa)
                continue
            _mark_seen(camara_id, placa)
            detected.append(PlacaDetectada(
                placa=placa,
                confianza=float(conf),
                bbox=[x1, y1, x2, y2],
            ))

        log.info("camara=%d placas=%d skipped=%d", camara_id, len(detected), len(skipped))
        return ReadPlateResponse(placas=detected, skipped_cooldown=skipped)

    except HTTPException:
        raise
    except Exception as e:
        log.exception("Error procesando frame")
        raise HTTPException(status_code=500, detail=str(e))
