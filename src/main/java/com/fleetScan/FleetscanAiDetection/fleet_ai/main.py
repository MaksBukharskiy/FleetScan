import os
import shutil
import tempfile

from fastapi import FastAPI, File, UploadFile
from fastapi.responses import JSONResponse

from . import damage_llm
from .vehicle_analyzer import analyze_vehicle

app = FastAPI()


@app.get("/")
def root():
    return {
        "status": "LPR service running",
        "damage_method": "llm" if damage_llm.is_enabled() else "heuristic",
    }


@app.post("/analyze")
def analyze(file: UploadFile = File(...)):
    # Never build paths from the client-supplied filename (path traversal).
    suffix = os.path.splitext(os.path.basename(file.filename or ""))[1][:10]
    fd, temp_path = tempfile.mkstemp(suffix=suffix)
    try:
        with os.fdopen(fd, "wb") as buffer:
            shutil.copyfileobj(file.file, buffer)
        result = analyze_vehicle(temp_path)
    finally:
        os.remove(temp_path)

    return JSONResponse(result)
