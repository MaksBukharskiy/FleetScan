import argparse
import json
import os
import tempfile
from typing import List, Dict, Any

import cv2

from .vehicle_analyzer import analyze_vehicle


VIDEO_EXTS = (".avi", ".mp4", ".mov", ".mkv")


def _collect_inputs(path: str) -> List[str]:
    if os.path.isdir(path):
        return [
            os.path.join(path, f)
            for f in sorted(os.listdir(path))
            if f.lower().endswith((".jpg", ".jpeg", ".png", ".bmp", ".webp", ".avif"))
        ]
    return [path]


def _analyze_video(path: str) -> Dict[str, Any]:
    cap = cv2.VideoCapture(path)
    if not cap.isOpened():
        return {"path": path, "error": "Cannot open video"}

    total = int(cap.get(cv2.CAP_PROP_FRAME_COUNT) or 0)
    target = total // 2 if total > 0 else 0
    cap.set(cv2.CAP_PROP_POS_FRAMES, target)
    ok, frame = cap.read()
    if not ok or frame is None:
        cap.release()
        return {"path": path, "error": "Cannot read frame"}

    with tempfile.NamedTemporaryFile(suffix=".jpg", delete=False) as tmp:
        tmp_path = tmp.name
    cv2.imwrite(tmp_path, frame)
    cap.release()

    try:
        result = analyze_vehicle(tmp_path)
        return {"path": path, "frame_path": tmp_path, **result}
    finally:
        try:
            os.remove(tmp_path)
        except OSError:
            pass


def main() -> None:
    parser = argparse.ArgumentParser(description="Analyze vehicle images")
    parser.add_argument("--input", required=True, help="Path to image file or directory")
    args = parser.parse_args()

    results = []
    if os.path.isfile(args.input) and args.input.lower().endswith(VIDEO_EXTS):
        results.append(_analyze_video(args.input))
    else:
        inputs = _collect_inputs(args.input)
        for path in inputs:
            results.append({"path": path, **analyze_vehicle(path)})

    print(json.dumps(results, ensure_ascii=False, indent=2))


if __name__ == "__main__":
    main()
