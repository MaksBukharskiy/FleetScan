"""
Local (free, offline) vehicle damage detector.

Loads any Hugging Face object-detection model saved with `save_pretrained`
(RT-DETR, D-FINE, DETR, ...) from DAMAGE_DETECTOR_PATH. All of these are
Apache-2.0 via `transformers`, unlike Ultralytics YOLO (AGPL-3.0).

Train one with tools/train_damage_detector.py. Without a model directory the
detector is simply disabled and the hybrid pipeline falls back to the LLM.
"""
import json
import logging
import os
from typing import Any, Dict, List, Optional

logger = logging.getLogger(__name__)

DEFAULT_MODEL_DIR = os.path.join(os.path.dirname(__file__), "models", "damage_detector")

# Severity per class. Overridable with damage_severity.json next to the model.
DEFAULT_SEVERITY = {
    "scratch": "minor",
    "dent": "moderate",
    "crack": "moderate",
    "lamp_broken": "severe",       # lights not working = safety issue
    "glass_shatter": "severe",
    "tire_flat": "severe",
}
SEVERITY_ORDER = ["none", "minor", "moderate", "severe"]
MAX_DETECTIONS = 20

_state: Dict[str, Any] = {"loaded": False, "model": None, "processor": None, "severity": DEFAULT_SEVERITY}


def model_dir() -> str:
    return os.getenv("DAMAGE_DETECTOR_PATH", DEFAULT_MODEL_DIR)


def is_enabled() -> bool:
    return os.path.isfile(os.path.join(model_dir(), "config.json"))


def _load() -> bool:
    if _state["loaded"]:
        return _state["model"] is not None
    _state["loaded"] = True
    if not is_enabled():
        logger.info("Local damage detector not found at %s, skipping", model_dir())
        return False
    try:
        import torch  # noqa: F401
        from transformers import AutoImageProcessor, AutoModelForObjectDetection

        path = model_dir()
        _state["processor"] = AutoImageProcessor.from_pretrained(path)
        _state["model"] = AutoModelForObjectDetection.from_pretrained(path).eval()
        sev_file = os.path.join(path, "damage_severity.json")
        if os.path.isfile(sev_file):
            with open(sev_file, encoding="utf-8") as f:
                _state["severity"] = {**DEFAULT_SEVERITY, **json.load(f)}
        logger.info("Local damage detector loaded from %s", path)
        return True
    except Exception:
        logger.exception("Failed to load local damage detector")
        _state["model"] = None
        return False


def max_severity(severities: List[str]) -> str:
    return max(severities, key=SEVERITY_ORDER.index, default="none")


def detect(image_path: str, min_score: float = float(os.getenv("DAMAGE_LOCAL_MIN_SCORE", "0.25"))) -> Optional[Dict[str, Any]]:
    """
    Returns {"detections": [{label, score, severity, box}], "severity": str}
    or None if the local model is unavailable.
    """
    if not _load():
        return None

    import torch
    from PIL import Image

    image = Image.open(image_path).convert("RGB")
    processor, model = _state["processor"], _state["model"]
    inputs = processor(images=image, return_tensors="pt")
    with torch.no_grad():
        outputs = model(**inputs)
    result = processor.post_process_object_detection(
        outputs, threshold=min_score, target_sizes=[(image.height, image.width)]
    )[0]

    detections = []
    for score, label_id, box in zip(result["scores"], result["labels"], result["boxes"]):
        label = model.config.id2label.get(int(label_id), str(int(label_id)))
        detections.append({
            "label": label,
            "score": round(float(score), 3),
            "severity": _state["severity"].get(label, "moderate"),
            "box": [round(float(v)) for v in box.tolist()],
            # Relative box (0..1) so it stays valid after the image is resized for the LLM
            "box_rel": [round(min(max(float(v) / d, 0.0), 1.0), 3)
                        for v, d in zip(box.tolist(), (image.width, image.height) * 2)],
        })
    detections.sort(key=lambda d: d["score"], reverse=True)
    detections = detections[:MAX_DETECTIONS]
    return {"detections": detections, "severity": max_severity([d["severity"] for d in detections])}
