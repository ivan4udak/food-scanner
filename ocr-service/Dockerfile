# OCR-сервис (скелет): лёгкий Python, без тяжёлого движка (EasyOCR — v1.10.2).
FROM python:3.12-slim

WORKDIR /app
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

COPY app.py .

EXPOSE 8090
HEALTHCHECK --interval=20s --timeout=4s --start-period=15s --retries=5 \
  CMD python -c "import urllib.request,sys; sys.exit(0 if urllib.request.urlopen('http://127.0.0.1:8090/health').status==200 else 1)" || exit 1

CMD ["python", "app.py"]
