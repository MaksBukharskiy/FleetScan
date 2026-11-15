"""
Vehicle damage assessment with Claude vision.

Enabled when ANTHROPIC_API_KEY is set. Returns None on any failure so the
caller can fall back to the local heuristic.
"""
import base64
import json
import logging
import os
from typing import Any, Dict, Optional

logger = logging.getLogger(__name__)

try:
    import anthropic
except Exception:  # pragma: no cover
    anthropic = None

# Haiku: ~$0.10 / $0.50 per 1M tokens -> roughly $0.0003-0.0005 per photo.
MODEL = os.getenv("DAMAGE_MODEL", "claude-haiku-5-5")
EFFORT = os.getenv("DAMAGE_EFFORT", "low")
# Server-side refusal fallback exists for Opus/Sonnet/Fable only, not Haiku.
_FALLBACK_MODELS = ("claude-opus-5-5", "claude-sonnet-5-5", "claude-fable-5-2", "claude-fable-5-1")

SEVERITIES = ["none", "minor", "moderate", "severe"]

SCHEMA = {
    "type": "object",
    "properties": {
        "is_vehicle_photo": {"type": "boolean"},
        "photo_quality": {"type": "string", "enum": ["good", "poor", "unusable"]},
        "overall_severity": {"type": "string", "enum": SEVERITIES},
        "damages": {
            "type": "array",
            "items": {
                "type": "object",
                "properties": {
                    "part": {"type": "string"},
                    "type": {"type": "string", "enum": [
                        "scratch", "dent", "crack", "broken_part", "missing_part",
                        "paint_damage", "rust", "glass_damage", "tire_damage", "other",
                    ]},
                    "severity": {"type": "string", "enum": SEVERITIES[1:]},
                    "description": {"type": "string"},
                },
                "required": ["part", "type", "severity", "description"],
                "additionalProperties": False,
            },
        },
        "confidence": {"type": "number"},
        "summary": {"type": "string"},
    },
    "required": ["is_vehicle_photo", "photo_quality", "overall_severity", "damages", "confidence", "summary"],
    "additionalProperties": False,
}

PROMPT = """You are inspecting a photo a taxi/fleet driver sent for a routine vehicle check.

Report only damage that is actually visible on the vehicle body, glass, lights, wheels or interior.
Do not report dirt, dust, water drops, reflections or shadows as damage.
If the photo is not of a vehicle, set is_vehicle_photo=false and overall_severity="none".
If the photo is too dark/blurry to judge, set photo_quality accordingly and keep confidence low.

Severity:
- minor: cosmetic (small scratches, chips), car can keep working
- moderate: visible dents, cracked bumper/lights, needs repair soon
- severe: safety-relevant (broken glass, lights not working, tire damage, structural damage)

confidence is 0..1 for the overall assessment. Write description and summary in Russian."""

_client = None


def is_enabled() -> bool:
    return anthropic is not None and bool(os.getenv("ANTHROPIC_API_KEY"))


def _get_client():
    global _client
    if _client is None:
        _client = anthropic.Anthropic(timeout=90.0)
    return _client


def _media_type(image_bytes: bytes) -> str:
    if image_bytes[:3] == b"\xff\xd8\xff":
        return "image/jpeg"
    if image_bytes[:8] == b"\x89PNG\r\n\x1a\n":
        return "image/png"
    if image_bytes[:4] == b"RIFF" and image_bytes[8:12] == b"WEBP":
        return "image/webp"
    if image_bytes[:6] in (b"GIF87a", b"GIF89a"):
        return "image/gif"
    return ""


MAX_IMAGE_BYTES = 3_500_000  # base64 adds ~33%; API limit is 5 MB per image
# Image tokens grow with pixel count (~w*h/750), so cap the long side:
# 1280px is enough to see dents and scratches and keeps a photo around 1.5k tokens.
MAX_SIDE = int(os.getenv("DAMAGE_MAX_SIDE", "1280"))


def _prepare(data: bytes) -> bytes:
    import io
    from PIL import Image

    img = Image.open(io.BytesIO(data))
    if max(img.size) <= MAX_SIDE and len(data) <= MAX_IMAGE_BYTES:
        return data
    img = img.convert("RGB")
    img.thumbnail((MAX_SIDE, MAX_SIDE))
    out = io.BytesIO()
    img.save(out, format="JPEG", quality=85)
    return out.getvalue()


def _hint_text(local_hint: Optional[Dict[str, Any]]) -> str:
    if not local_hint or not local_hint.get("detections"):
        return ""
    lines = [
        f"- {d['label']} (score {d['score']}) at box x1,y1,x2,y2 = {d.get('box_rel', d['box'])} (fractions of width/height)"
        for d in local_hint["detections"][:8]
    ]
    return ("\n\nA fast local detector flagged these regions (it is often wrong; "
            "confirm or reject each by looking at the photo yourself):\n" + "\n".join(lines))


def assess_damage(image_path: str, local_hint: Optional[Dict[str, Any]] = None) -> Optional[Dict[str, Any]]:
    if not is_enabled():
        return None

    with open(image_path, "rb") as f:
        data = f.read()
    try:
        data = _prepare(data)
    except Exception:
        logger.warning("Could not read image for LLM damage check: %s", image_path)
        return None
    media_type = _media_type(data)
    if not media_type:
        logger.warning("Unsupported image format for LLM damage check: %s", image_path)
        return None

    try:
        extra: Dict[str, Any] = {}
        if MODEL in _FALLBACK_MODELS:
            extra = {"betas": ["server-side-fallback-2026-07-01"], "fallbacks": "default"}
        response = _get_client().beta.messages.create(
            model=MODEL,
            max_tokens=4000,
            **extra,
            output_config={
                "effort": EFFORT,
                "format": {"type": "json_schema", "schema": SCHEMA},
            },
            messages=[{
                "role": "user",
                "content": [
                    {"type": "image", "source": {
                        "type": "base64",
                        "media_type": media_type,
                        "data": base64.standard_b64encode(data).decode("utf-8"),
                    }},
                    {"type": "text", "text": PROMPT + _hint_text(local_hint)},
                ],
            }],
        )
    except anthropic.RateLimitError:
        logger.warning("Claude rate limit hit, falling back")
        return None
    except anthropic.APIStatusError as e:
        logger.error("Claude API error %s: %s", e.status_code, e.message)
        return None
    except anthropic.APIConnectionError:
        logger.error("Claude API unreachable, falling back")
        return None

    if response.stop_reason != "end_turn":
        logger.warning("LLM damage check stopped with %s", response.stop_reason)
        return None

    text = next((b.text for b in response.content if b.type == "text"), None)
    if not text:
        return None
    try:
        return json.loads(text)
    except json.JSONDecodeError:
        logger.error("LLM damage check returned invalid JSON")
        return None
