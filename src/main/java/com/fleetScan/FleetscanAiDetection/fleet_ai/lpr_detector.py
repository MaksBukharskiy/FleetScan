import easyocr
import logging
from typing import Optional, Tuple, List

try:
    import cv2
except Exception:  # pragma: no cover
    cv2 = None

from .plate_yolo import detect_plate_rois_yolo
from .plate_formats import OCR_ALLOWLIST, match_plate, normalize
logger = logging.getLogger(__name__)

# Инициализация один раз при запуске сервера
reader = easyocr.Reader(['en'], gpu=False)

ALLOWLIST = OCR_ALLOWLIST


def _joined_text(results: List[Tuple[List, str, float]]) -> Tuple[str, float]:
    """Plates are often split into several OCR boxes ("01" + "A123BC"): join them left to right."""
    if not results:
        return "", 0.0
    ordered = sorted(results, key=lambda r: min(pt[0] for pt in r[0]) if r[0] is not None else 0)
    text = "".join(r[1] for r in ordered)
    conf = min(float(r[2]) for r in ordered)
    return text, conf


def _pick_best_candidate(results: List[Tuple[List, str, float]]) -> Tuple[Optional[str], float]:
    """
    Returns (plate, confidence). A plate that matches a known format (UZ/RU)
    always wins over raw text; if nothing matches, the raw text is returned
    with its confidence halved so the backend sends it to manual review.
    """
    if not results:
        return None, 0.0

    best_plate, best_conf = None, 0.0
    candidates = [(text, float(conf)) for _, text, conf in results]
    candidates.append(_joined_text(results))

    for text, conf in candidates:
        found = match_plate(text)
        if found and conf > best_conf:
            best_plate, best_conf = found[0], conf

    if best_plate:
        return best_plate, best_conf

    _, text, conf = max(results, key=lambda x: x[2])
    return normalize(text) or None, float(conf) * 0.5


def _readtext_variants_from_bgr(img_bgr):
    if cv2 is None or img_bgr is None:
        return []

    results = []

    rgb = cv2.cvtColor(img_bgr, cv2.COLOR_BGR2RGB)
    results.extend(reader.readtext(rgb, allowlist=ALLOWLIST))

    # Масштабируем для мелкого номера
    rgb_2x = cv2.resize(rgb, None, fx=2.0, fy=2.0, interpolation=cv2.INTER_CUBIC)
    results.extend(reader.readtext(rgb_2x, allowlist=ALLOWLIST))

    # Пороговая версия для контрастных номеров
    gray = cv2.cvtColor(img_bgr, cv2.COLOR_BGR2GRAY)
    th = cv2.adaptiveThreshold(gray, 255, cv2.ADAPTIVE_THRESH_GAUSSIAN_C,
                               cv2.THRESH_BINARY, 31, 5)
    th_rgb = cv2.cvtColor(th, cv2.COLOR_GRAY2RGB)
    results.extend(reader.readtext(th_rgb, allowlist=ALLOWLIST))

    return results


def _detect_plate_rois(image_path: str, max_rois: int = 2):
    if cv2 is None:
        return []

    img = cv2.imread(image_path)
    if img is None:
        return []

    gray = cv2.cvtColor(img, cv2.COLOR_BGR2GRAY)
    blur = cv2.bilateralFilter(gray, 11, 17, 17)
    edges = cv2.Canny(blur, 30, 200)

    contours, _ = cv2.findContours(edges, cv2.RETR_TREE, cv2.CHAIN_APPROX_SIMPLE)
    contours = sorted(contours, key=cv2.contourArea, reverse=True)

    h, w = gray.shape[:2]
    img_area = float(h * w)

    rois = []
    for cnt in contours[:50]:
        peri = cv2.arcLength(cnt, True)
        approx = cv2.approxPolyDP(cnt, 0.02 * peri, True)
        if len(approx) != 4:
            continue
        x, y, rw, rh = cv2.boundingRect(approx)
        if rh == 0:
            continue
        ar = rw / float(rh)
        area = rw * rh
        if 2.0 <= ar <= 6.0 and 0.001 * img_area <= area <= 0.3 * img_area:
            roi = img[y:y + rh, x:x + rw]
            rois.append(roi)
            if len(rois) >= max_rois:
                break

    return rois


def recognize_license(image_path: str) -> Tuple[Optional[str], float]:
    """
    Распознаёт номер авто.
    Возвращает:
        (номер, confidence)
    """

    try:
        # 1) Пытаемся распознать по ROI номерного знака (YOLO -> эвристика)
        rois = detect_plate_rois_yolo(image_path)
        if not rois:
            rois = _detect_plate_rois(image_path)
        best_plate = None
        best_conf = 0.0

        for roi in rois:
            results = _readtext_variants_from_bgr(roi)
            plate, conf = _pick_best_candidate(results)
            if plate and conf > best_conf:
                best_plate = plate
                best_conf = conf

        if best_plate:
            return best_plate, best_conf

        # 2) Фоллбэк — OCR по всему изображению
        if cv2 is not None:
            img = cv2.imread(image_path)
            results = _readtext_variants_from_bgr(img)
        else:
            results = reader.readtext(image_path, allowlist=ALLOWLIST)
        return _pick_best_candidate(results)

    except Exception:
        logger.exception("Ошибка распознавания")
        return None, 0.0
