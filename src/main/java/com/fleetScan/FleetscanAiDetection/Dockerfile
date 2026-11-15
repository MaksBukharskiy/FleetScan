FROM python:3.11-slim

ENV PYTHONDONTWRITEBYTECODE=1 \
    PYTHONUNBUFFERED=1 \
    DEBIAN_FRONTEND=noninteractive

# Установка системных зависимостей для OpenCV и других библиотек
# HTTPS mirror + retries: plain-HTTP Debian downloads get 403 on some networks
RUN sed -i 's|http://deb.debian.org|https://deb.debian.org|g' /etc/apt/sources.list.d/debian.sources && \
    apt-get -o Acquire::Retries=3 update && apt-get -o Acquire::Retries=3 install -y --no-install-recommends \
    libgl1 \
    libglib2.0-0t64 \
    libsm6 \
    libxext6 \
    libxrender-dev \
    libgomp1 \
    libxcb1 \
    libxkbcommon0 \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /usr/src/app

COPY src/main/java/com/fleetScan/FleetscanAiDetection/fleet_ai/requirements.txt ./requirements.txt
# Clear any global pip config that might enforce hash-checking.
RUN rm -f /etc/pip.conf /etc/pip/pip.conf /root/.config/pip/pip.conf && \
    pip install --no-cache-dir -r requirements.txt

COPY src/main/java/com/fleetScan/FleetscanAiDetection/fleet_ai ./fleet_ai
COPY src/main/java/com/fleetScan/FleetscanAiDetection/fleet_ai/models ./fleet_ai/models

# Предварительная загрузка моделей EasyOCR чтобы избежать проблем при запуске
RUN python3 -c "import easyocr; easyocr.Reader(['en'], gpu=False, download_enabled=True)" || true

EXPOSE 8000

CMD ["uvicorn", "fleet_ai.main:app", "--host", "0.0.0.0", "--port", "8000"]
