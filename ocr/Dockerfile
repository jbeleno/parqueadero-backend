# OCR sidecar: YOLO + PaddleOCR + FastAPI
FROM python:3.11-slim

# Dependencias del sistema para opencv-python-headless y PaddleOCR
RUN apt-get update && apt-get install -y --no-install-recommends \
        libgl1 \
        libglib2.0-0 \
        libgomp1 \
        ccache \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app

# Cache de pip aparte para que builds reiteradas no reinstalen todo
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# Copiar app + modelo (el .pt pesa 5.2 MB, se incluye en la imagen)
COPY ocr_parqueadero.py .
COPY app.py .
COPY yolo11n_placas.pt .

# Pre-warm: ejecutar import de paddleocr en build para descargar modelos
# y que el primer request en runtime sea rapido. Si falla por red en build,
# se descargan en primer uso.
RUN python -c "from paddleocr import PaddleOCR; PaddleOCR(use_textline_orientation=True, lang='en')" || \
    echo "WARN: no se pudo prewarm paddleocr en build, se descargara al iniciar"

EXPOSE 8001

CMD ["uvicorn", "app:app", "--host", "0.0.0.0", "--port", "8001"]
