"""
Evaluate the local damage detector the way the product uses it: per photo.

For every image in the test split it answers:
  * per class: did the model find this damage type on a photo that has it? (image-level precision/recall)
  * any damage at the "defect" level (moderate+severe): precision/recall
  * hybrid routing: how many photos would be decided locally (free) vs sent to
    Claude, and how often the locally decided ones are wrong.

Note: CarDD only contains damaged cars, so it cannot measure false alarms on
clean cars. Add your own clean photos to the test set for that.

Usage (from FleetscanAiDetection/):
    python tools/evaluate_damage_detector.py --data /path/to/CarDD_COCO --model fleet_ai/models/damage_detector
"""
import argparse
import json
import os
import sys
from collections import Counter

sys.path.insert(0, os.path.join(os.path.dirname(__file__), ".."))
sys.path.insert(0, os.path.dirname(__file__))

from train_damage_detector import load_split  # noqa: E402

DEFECT = {"moderate", "severe"}


def ratio(a, b):
    return a / b if b else float("nan")


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--data", required=True)
    ap.add_argument("--model", required=True)
    ap.add_argument("--split", default="test", choices=["test", "valid"])
    ap.add_argument("--min-score", type=float, default=0.25)
    ap.add_argument("--sure", type=float, default=0.75, help="DAMAGE_LOCAL_SURE")
    ap.add_argument("--limit", type=int, default=0)
    ap.add_argument("--json-out", help="also write the metrics as JSON")
    args = ap.parse_args()

    os.environ["DAMAGE_DETECTOR_PATH"] = args.model
    from fleet_ai import damage_local

    items, cats = load_split(args.data, args.split, {})
    if args.limit:
        items = items[: args.limit]
    severity_of = damage_local._state["severity"]  # defaults; refreshed after load below

    tp, fp, fn = Counter(), Counter(), Counter()
    defect = Counter()
    routing = Counter()
    for n, item in enumerate(items, 1):
        res = damage_local.detect(item["path"], min_score=args.min_score)
        if res is None:
            sys.exit(f"Could not load model from {args.model}")
        severity_of = damage_local._state["severity"]

        truth = {cats[a["category_id"]] for a in item["anns"]}
        pred = {d["label"] for d in res["detections"]}
        for c in truth | pred:
            if c in truth and c in pred:
                tp[c] += 1
            elif c in pred:
                fp[c] += 1
            else:
                fn[c] += 1

        truth_defect = any(severity_of.get(c, "moderate") in DEFECT for c in truth)
        pred_defect = res["severity"] in DEFECT
        defect[(truth_defect, pred_defect)] += 1

        dets = res["detections"]
        top = dets[0]["score"] if dets else 0.0
        if not dets:
            route = "local_clean"
        elif top >= args.sure and res["severity"] != "severe":
            route = "local_damage"
        else:
            route = "llm"
        routing[route] += 1
        if route != "llm" and truth_defect != pred_defect:
            routing["local_wrong"] += 1

        if n % 50 == 0:
            print(f"  {n}/{len(items)}", flush=True)

    classes = sorted(set(tp) | set(fp) | set(fn))
    per_class = {
        c: {"precision": ratio(tp[c], tp[c] + fp[c]), "recall": ratio(tp[c], tp[c] + fn[c]),
            "photos_with_class": tp[c] + fn[c]}
        for c in classes
    }
    d_tp, d_fp, d_fn = defect[(True, True)], defect[(False, True)], defect[(True, False)]
    local = routing["local_clean"] + routing["local_damage"]
    metrics = {
        "photos": len(items),
        "per_class": per_class,
        "defect": {"precision": ratio(d_tp, d_tp + d_fp), "recall": ratio(d_tp, d_tp + d_fn)},
        "routing": {
            "decided_locally": ratio(local, len(items)),
            "sent_to_llm": ratio(routing["llm"], len(items)),
            "local_error_rate": ratio(routing["local_wrong"], local),
        },
    }

    print(f"\nPhotos: {len(items)}  (split: {args.split})\n")
    print(f"{'class':<15}{'precision':>10}{'recall':>9}{'photos':>8}")
    for c, m in per_class.items():
        print(f"{c:<15}{m['precision']:>10.2f}{m['recall']:>9.2f}{m['photos_with_class']:>8}")
    print(f"\nDefect (moderate+severe) per photo: precision {metrics['defect']['precision']:.2f}, "
          f"recall {metrics['defect']['recall']:.2f}")
    r = metrics["routing"]
    print(f"Hybrid routing: {r['decided_locally']:.0%} decided locally (free), "
          f"{r['sent_to_llm']:.0%} sent to Claude; local decisions wrong {r['local_error_rate']:.1%}")

    if args.json_out:
        with open(args.json_out, "w", encoding="utf-8") as f:
            json.dump(metrics, f, ensure_ascii=False, indent=2)


if __name__ == "__main__":
    main()
