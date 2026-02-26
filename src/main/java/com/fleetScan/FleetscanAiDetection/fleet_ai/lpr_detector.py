import easyocr
import logging
import re
from typing import Optional, Tuple, List

try:
    import cv2
except Exception:  # pragma: no cover
    cv2 = None

from .plate_yolo import detect_plate_rois_yolo
logger = logging.getLogger(__name__)

# Инициализация один раз при запуске сервера
reader = easyocr.Reader(['en'], gpu=False)

RU_PLATE_RE = re.compile(r'([ABEKMHOPCTYX]\\d{3}[ABEKMHOPCTYX]{2}\\d{2,3})')
RU_PLATE_FULL_RE = re.compile(r'^[ABEKMHOPCTYX]\\d{3}[ABEKMHOPCTYX]{2}\\d{2,3}$')
ALNUM_RE = re.compile(r'[^A-Z0-9]')
ALLOWLIST = "ABEKMHOPCTYX0123456789"
DIGIT_FIX = {
    "O": "0",
    "I": "1",
    "Z": "2",
    "S": "5",
    "B": "8",
    "G": "6",
    "T": "7",
}


def _normalize_text(text: str) -> str:
    cleaned = ALNUM_RE.sub('', text.upper())
    return cleaned


def _extract_ru_plate(text: str) -> Optional[str]:
    cleaned = _normalize_text(text)
    match = RU_PLATE_RE.search(cleaned)
    if match:
        return match.group(1)
    return None


def _coerce_ru_plate(text: str) -> Optional[str]:
    cleaned = _normalize_text(text)
    if len(cleaned) < 8:
        return None

    # Сканируем окнами 8 и 9 символов
    for size in (9, 8):
        for i in range(0, len(cleaned) - size + 1):
            candidate = cleaned[i:i + size]
            # Чиним возможные ошибки в регионе (последние 2-3 символа)
            region_len = 3 if size == 9 else 2
            region = candidate[-region_len:]
            region = "".join(DIGIT_FIX.get(ch, ch) for ch in region)
            candidate_fixed = candidate[:-region_len] + region
            if RU_PLATE_FULL_RE.match(candidate_fixed):
                return candidate_fixed

    # Если на конце 4 цифры, пробуем отбросить одну (регион из 3 цифр)
    if len(cleaned) >= 9:
        tail = cleaned[-4:]
        if tail.isdigit():
            candidate = cleaned[:-1]
            if RU_PLATE_FULL_RE.match(candidate):
                return candidate

    return None


def _pick_best_candidate(results: List[Tuple[List, str, float]]) -> Tuple[Optional[str], float]:
    if not results:
        return None, 0.0

    best_plate = None
    best_conf = 0.0

    for _, text, conf in results:
        plate = _extract_ru_plate(text)
        if plate and conf > best_conf:
            best_plate = plate
            best_conf = float(conf)

    if best_plate:
        return best_plate, best_conf

    # Если совпадений с шаблоном нет — берём самый уверенный текст
    best_result = max(results, key=lambda x: x[2])
    coerced = _coerce_ru_plate(best_result[1])
    if coerced:
        return coerced, float(best_result[2])

    return _normalize_text(best_result[1]) or None, float(best_result[2])


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

    except Exception as e:
        logger.exception("Ошибка распознавания")
        return None, 0.0
