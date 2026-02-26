import os
from typing import List

try:
    from ultralytics import YOLO
except Exception:  # pragma: no cover
    YOLO = None

try:
    import cv2
except Exception:  # pragma: no cover
    cv2 = None


LOCAL_MODEL = os.path.join(os.path.dirname(__file__), "models", "plate_yolo", "best.pt")
REMOTE_MODEL = "https://huggingface.co/Koushim/yolov8-license-plate-detection/resolve/main/best.pt"
_MODEL = None


def _get_model():
    global _MODEL
    if _MODEL is not None:
        return _MODEL
    if YOLO is None:
        return None
    model_path = os.getenv("PLATE_YOLO_MODEL")
    if not model_path:
        model_path = LOCAL_MODEL if os.path.exists(LOCAL_MODEL) else REMOTE_MODEL
    try:
        _MODEL = YOLO(model_path)
        return _MODEL
    except Exception:
        return None


def detect_plate_rois_yolo(image_path: str, max_rois: int = 2, conf: float = 0.25) -> List:
    if YOLO is None or cv2 is None:
        return []

    try:
        model = _get_model()
    except Exception:
        model = None
    if model is None:
        return []

    img = cv2.imread(image_path)
    if img is None:
        return []

    results = model.predict(source=img, conf=conf, imgsz=640, verbose=False)
    if not results:
        return []

    r = results[0]
    if r.boxes is None:
        return []

    boxes = r.boxes
    if boxes.conf is None or boxes.xyxy is None:
        return []

    confs = boxes.conf.cpu().numpy().tolist()
    coords = boxes.xyxy.cpu().numpy().tolist()
    candidates = list(zip(coords, confs))
    candidates.sort(key=lambda x: x[1], reverse=True)

    rois = []
    for (x1, y1, x2, y2), _ in candidates[:max_rois]:
        x1 = max(0, int(x1))
        y1 = max(0, int(y1))
        x2 = min(img.shape[1], int(x2))
        y2 = min(img.shape[0], int(y2))
        if x2 <= x1 or y2 <= y1:
            continue
        roi = img[y1:y2, x1:x2]
        rois.append(roi)

    return rois
