import os
from typing import Dict, Any, Tuple

import numpy as np
from PIL import Image

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


def assess_vehicle_condition(image_path: str) -> Dict[str, Any]:
    threshold = float(os.getenv("DAMAGE_THRESHOLD", "0.70"))
    score, details = _damage_metrics(image_path)

    condition = "defect" if score >= threshold else "ok"
    # Уверенность — расстояние до порога
    if score >= threshold:
        confidence = min((score - threshold) / max(1e-6, (1.0 - threshold)), 1.0)
    else:
        confidence = min((threshold - score) / max(1e-6, threshold), 1.0)

    return {
        "condition": condition,
        "condition_confidence": float(confidence),
        "damage_score": float(score),
        "damage_details": details,
    }


def analyze_vehicle(image_path: str) -> Dict[str, Any]:
    plate, plate_conf = recognize_license(image_path)
    condition_info = assess_vehicle_condition(image_path)

    return {
        "license_plate": plate,
        "confidence": float(plate_conf),
        **condition_info,
    }
