import os
import random
from typing import Any, Dict, Optional, Tuple

import numpy as np
from PIL import Image

from . import damage_llm, damage_local
from .lpr_detector import recognize_license


def _center_crop(arr: np.ndarray, ratio: float = 0.7) -> np.ndarray:
    if ratio >= 1.0:
        return arr
    h, w = arr.shape[:2]
    new_h = max(1, int(h * ratio))
    new_w = max(1, int(w * ratio))
    y0 = (h - new_h) // 2
    x0 = (w - new_w) // 2
    return arr[y0:y0 + new_h, x0:x0 + new_w]


def _laplacian(arr: np.ndarray) -> np.ndarray:
    return (
        -4.0 * arr
        + np.roll(arr, 1, 0)
        + np.roll(arr, -1, 0)
        + np.roll(arr, 1, 1)
        + np.roll(arr, -1, 1)
    )


def _box_blur(arr: np.ndarray, k: int = 5) -> np.ndarray:
    if k <= 1:
        return arr
    pad = k // 2
    padded = np.pad(arr, pad, mode="reflect")
    cumsum = padded.cumsum(0).cumsum(1)
    # Добавляем нулевую строку/колонку для корректных размеров
    cumsum = np.pad(cumsum, ((1, 0), (1, 0)), mode="constant")
    total = (
        cumsum[k:, k:]
        - cumsum[:-k, k:]
        - cumsum[k:, :-k]
        + cumsum[:-k, :-k]
    )
    return total / float(k * k)


def _damage_metrics(image_path: str) -> Tuple[float, Dict[str, float]]:
    img = Image.open(image_path).convert("RGB")
    arr = np.asarray(img, dtype=np.float32) / 255.0
    arr = _center_crop(arr, ratio=0.7)
    gray = (
        0.299 * arr[:, :, 0]
        + 0.587 * arr[:, :, 1]
        + 0.114 * arr[:, :, 2]
    )

    lap = _laplacian(gray)
    edge_mean = float(np.mean(np.abs(lap)))
    edge_var = float(np.var(lap))

    blur = _box_blur(gray, k=7)
    hf = gray - blur
    hf_energy = float(np.mean(np.abs(hf)))

    # Нормализация под базовые диапазоны (эмпирически)
    edge_mean_n = min(edge_mean / 0.12, 1.0)
    edge_var_n = min(edge_var / 0.02, 1.0)
    hf_energy_n = min(hf_energy / 0.08, 1.0)

    score = 0.5 * edge_mean_n + 0.3 * edge_var_n + 0.2 * hf_energy_n

    details = {
        "edge_mean": edge_mean,
        "edge_var": edge_var,
        "hf_energy": hf_energy,
        "edge_mean_n": edge_mean_n,
        "edge_var_n": edge_var_n,
        "hf_energy_n": hf_energy_n,
    }

    return float(score), details


def _assess_heuristic(image_path: str) -> Dict[str, Any]:
    """
    Fallback when the LLM check is unavailable. This is NOT a damage detector:
    it measures edge/texture energy, so busy backgrounds raise the score.
    Results are marked damage_method="heuristic" and should be reviewed by a human.
    """
    threshold = float(os.getenv("DAMAGE_THRESHOLD", "0.70"))
    score, details = _damage_metrics(image_path)

    condition = "defect" if score >= threshold else "ok"
    # Уверенность — расстояние до порога, но не выше 0.5: эвристике нельзя доверять
    if score >= threshold:
        confidence = min((score - threshold) / max(1e-6, (1.0 - threshold)), 1.0)
    else:
        confidence = min((threshold - score) / max(1e-6, threshold), 1.0)

    return {
        "condition": condition,
        "condition_confidence": float(min(confidence, 0.5)),
        "damage_method": "heuristic",
        "damage_score": float(score),
        "damage_severity": None,
        "damages": [],
        "damage_summary": None,
        "damage_details": details,
    }


SEVERITY_SCORE = {"none": 0.0, "minor": 0.33, "moderate": 0.66, "severe": 1.0}
DEFECT_SEVERITIES = {
    s.strip() for s in os.getenv("DAMAGE_DEFECT_SEVERITIES", "moderate,severe").split(",") if s.strip()
}
# Local detections at or above this score are trusted without asking the LLM.
LOCAL_SURE = float(os.getenv("DAMAGE_LOCAL_SURE", "0.75"))
# Share of "clean" photos still double-checked by the LLM: catches what the
# local model misses and gives labelled examples for retraining.
AUDIT_RATE = float(os.getenv("DAMAGE_AUDIT_RATE", "0.05"))


def _condition_for(severity: str) -> str:
    return "defect" if severity in DEFECT_SEVERITIES else "ok"


def _from_llm(llm: Dict[str, Any], method: str, local: Optional[Dict[str, Any]] = None) -> Dict[str, Any]:
    severity = llm.get("overall_severity", "none")
    if not llm.get("is_vehicle_photo", True) or llm.get("photo_quality") == "unusable":
        condition = "invalid_photo"
    else:
        condition = _condition_for(severity)
    result = {
        "condition": condition,
        "condition_confidence": float(llm.get("confidence", 0.0)),
        "damage_method": method,
        "damage_score": SEVERITY_SCORE.get(severity, 0.0),
        "damage_severity": severity,
        "damages": llm.get("damages", []),
        "damage_summary": llm.get("summary"),
        "photo_quality": llm.get("photo_quality"),
    }
    if local is not None:
        result["local_detections"] = local["detections"]
    return result


_LABEL_RU = {
    "scratch": "царапина", "dent": "вмятина", "crack": "трещина",
    "lamp_broken": "разбита фара/фонарь", "glass_shatter": "разбито стекло", "tire_flat": "спущено колесо",
}


def _from_local(local: Dict[str, Any], confidence: float) -> Dict[str, Any]:
    severity = local["severity"]
    damages = [
        {"part": "unknown", "type": d["label"], "severity": d["severity"],
         "description": _LABEL_RU.get(d["label"], d["label"]) + f" ({d['score']:.0%})"}
        for d in local["detections"]
    ]
    summary = "; ".join(x["description"] for x in damages) if damages else None
    return {
        "condition": _condition_for(severity),
        "condition_confidence": float(confidence),
        "damage_method": "local",
        "damage_score": SEVERITY_SCORE.get(severity, 0.0),
        "damage_severity": severity,
        "damages": damages,
        "damage_summary": summary,
        "local_detections": local["detections"],
    }


def assess_vehicle_condition(image_path: str) -> Dict[str, Any]:
    """
    Hybrid pipeline:
      1. Local detector (free, offline) looks at every photo.
      2. Clear cases are decided locally: nothing found, or confident non-severe damage.
      3. Unclear cases (low-score detections, anything severe, plus a random audit
         sample of clean photos) go to Claude Haiku with the local findings as hints.
      4. Without a local model -> Claude only; without an API key -> local only;
         with neither -> image heuristic.
    """
    local = damage_local.detect(image_path)

    if local is None:
        llm = damage_llm.assess_damage(image_path)
        return _from_llm(llm, "llm") if llm else _assess_heuristic(image_path)

    detections = local["detections"]
    top = detections[0]["score"] if detections else 0.0

    if not detections:
        needs_llm = random.random() < AUDIT_RATE
    else:
        needs_llm = top < LOCAL_SURE or local["severity"] == "severe"

    if needs_llm:
        llm = damage_llm.assess_damage(image_path, local_hint=local)
        if llm:
            return _from_llm(llm, "hybrid", local)

    # Decided locally (or LLM unavailable): confidence reflects how sure the detector was.
    confidence = 0.8 if not detections else top
    return _from_local(local, confidence)


def analyze_vehicle(image_path: str) -> Dict[str, Any]:
    plate, plate_conf = recognize_license(image_path)
    condition_info = assess_vehicle_condition(image_path)

    return {
        "license_plate": plate,
        "confidence": float(plate_conf),
        **condition_info,
    }
