"""
License plate formats and OCR post-processing.

Each format is a template of character classes:
    D - digit, L - letter (restricted per country).
OCR output is cleaned, then every window of the template length is
"coerced" (O->0 where a digit is expected, 0->O where a letter is
expected, etc.) and validated against the template and country rules.

Enabled countries are configured with PLATE_COUNTRIES (default "UZ,RU").
"""
import os
import re
from dataclasses import dataclass
from typing import Callable, Iterable, List, Optional, Tuple

# Uzbekistan region codes (first two digits of the plate).
UZ_REGIONS = {
    "01",  # Tashkent city
    "10",  # Tashkent region
    "20",  # Syrdarya
    "25",  # Jizzakh
    "30",  # Samarkand
    "40",  # Fergana
    "50",  # Namangan
    "60",  # Andijan
    "70",  # Kashkadarya
    "75",  # Surkhandarya
    "80",  # Bukhara
    "85",  # Navoi
    "90",  # Khorezm
    "95",  # Karakalpakstan
}

LATIN = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
# Russian plates only use letters that look the same in Latin and Cyrillic.
RU_LETTERS = "ABEKMHOPCTYX"

TO_DIGIT = {"O": "0", "Q": "0", "D": "0", "I": "1", "L": "1", "Z": "2",
            "S": "5", "B": "8", "G": "6", "T": "7"}
TO_LETTER = {"0": "O", "1": "I", "2": "Z", "5": "S", "8": "B", "6": "G", "7": "T", "4": "A"}

# Cyrillic look-alikes -> Latin (drivers often type plates in Cyrillic).
CYR_TO_LAT = str.maketrans("АВЕКМНОРСТУХ", "ABEKMHOPCTYX")

OCR_ALLOWLIST = LATIN + "0123456789"
_NON_ALNUM = re.compile(r"[^A-Z0-9]")


@dataclass(frozen=True)
class PlateFormat:
    name: str
    country: str
    template: str
    letters: str
    extra_check: Callable[[str], bool] = lambda _: True


FORMATS: List[PlateFormat] = [
    # 01 A 123 BC — private cars
    PlateFormat("uz_private", "UZ", "DDLDDDLL", LATIN, lambda p: p[:2] in UZ_REGIONS),
    # 01 123 ABC — legal entities (fleets, taxi companies)
    PlateFormat("uz_legal", "UZ", "DDDDDLLL", LATIN, lambda p: p[:2] in UZ_REGIONS),
    # A 123 BC 77 / A 123 BC 777
    PlateFormat("ru_2", "RU", "LDDDLLDD", RU_LETTERS),
    PlateFormat("ru_3", "RU", "LDDDLLDDD", RU_LETTERS),
]


def enabled_formats() -> List[PlateFormat]:
    countries = {c.strip().upper() for c in os.getenv("PLATE_COUNTRIES", "UZ,RU").split(",") if c.strip()}
    return [f for f in FORMATS if f.country in countries]


def normalize(text: Optional[str]) -> str:
    if not text:
        return ""
    return _NON_ALNUM.sub("", text.upper().translate(CYR_TO_LAT))


def _coerce(candidate: str, fmt: PlateFormat) -> Optional[str]:
    out = []
    for ch, cls in zip(candidate, fmt.template):
        if cls == "D":
            ch = TO_DIGIT.get(ch, ch)
            if not ch.isdigit():
                return None
        else:
            ch = TO_LETTER.get(ch, ch)
            if ch not in fmt.letters:
                return None
        out.append(ch)
    plate = "".join(out)
    return plate if fmt.extra_check(plate) else None


def match_plate(text: str, formats: Optional[Iterable[PlateFormat]] = None) -> Optional[Tuple[str, PlateFormat]]:
    """Return the first valid plate found in OCR text, preferring exact (uncorrected) matches."""
    cleaned = normalize(text)
    formats = list(formats) if formats is not None else enabled_formats()

    # Rank: fewest OCR corrections first, then the longest plate
    # (so "A123BC777" is not cut down to the 2-digit-region "A123BC77").
    best: Optional[Tuple[Tuple[int, int], str, PlateFormat]] = None
    for fmt in formats:
        size = len(fmt.template)
        for i in range(0, len(cleaned) - size + 1):
            window = cleaned[i:i + size]
            plate = _coerce(window, fmt)
            if plate is None:
                continue
            rank = (sum(a != b for a, b in zip(window, plate)), -size)
            if best is None or rank < best[0]:
                best = (rank, plate, fmt)
    return (best[1], best[2]) if best else None


def format_display(plate: str, fmt: PlateFormat) -> str:
    """Human-readable form, e.g. '01 A 123 BC'."""
    if fmt.name == "uz_private":
        return f"{plate[:2]} {plate[2]} {plate[3:6]} {plate[6:]}"
    if fmt.name == "uz_legal":
        return f"{plate[:2]} {plate[2:5]} {plate[5:]}"
    return f"{plate[0]} {plate[1:4]} {plate[4:6]} {plate[6:]}"
