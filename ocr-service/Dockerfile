# OCR-сервис (v1.11.0): EasyOCR (CPU). torch ставим CPU-колесом, чтобы образ не раздувался CUDA.
FROM python:3.12-slim

# системные библиотеки для opencv/easyocr
RUN apt-get update && apt-get install -y --no-install-recommends \
        libgl1 libglib2.0-0 \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app

# CPU-only torch/torchvision (без CUDA) — раньше easyocr, чтобы он не подтянул CUDA-сборку
RUN pip install --no-cache-dir torch==2.3.1 torchvision==0.18.1 \
        --index-url https://download.pytorch.org/whl/cpu

COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# Модели EasyOCR НЕ предзагружаем в образ (build короче/надёжнее в CI) —
# Reader инициализируется лениво на первой задаче, скачивая модели (см. лог "lazy-loading").

COPY app.py .

EXPOSE 8090
HEALTHCHECK --interval=20s --timeout=4s --start-period=30s --retries=5 \
  CMD python -c "import urllib.request,sys; sys.exit(0 if urllib.request.urlopen('http://127.0.0.1:8090/health').status==200 else 1)" || exit 1

CMD ["python", "app.py"]
