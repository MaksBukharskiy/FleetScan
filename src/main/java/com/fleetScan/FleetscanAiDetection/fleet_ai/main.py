from fastapi import FastAPI, UploadFile, File
from fastapi.responses import JSONResponse
import shutil
import os

from .vehicle_analyzer import analyze_vehicle

app = FastAPI()


@app.get("/")
def root():
    return {"status": "LPR service running"}


@app.post("/analyze")
async def analyze(file: UploadFile = File(...)):

    temp_path = f"/tmp/{file.filename}"

    # Сохраняем файл временно
    with open(temp_path, "wb") as buffer:
        shutil.copyfileobj(file.file, buffer)

    # Анализируем номер + состояние
    result = analyze_vehicle(temp_path)

    # Удаляем временный файл
    os.remove(temp_path)

    return JSONResponse(result)
