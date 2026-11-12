"""
Train the local damage detector (RT-DETRv2, Apache-2.0) on a COCO-format dataset.

Dataset layout (Roboflow "COCO" export, CarDD converted to COCO, or your own):
    DATA/
      train/  *.jpg + _annotations.coco.json
      valid/  *.jpg + _annotations.coco.json

Category names are mapped to the labels the service understands:
    scratch, dent, crack, lamp_broken, glass_shatter, tire_flat
Unknown names are kept as-is (severity defaults to "moderate"); pass
--label-map map.json to rename them, e.g. {"glass shatter": "glass_shatter"}.

Usage (from FleetscanAiDetection/):
    pip install -r tools/requirements-train.txt
    python tools/train_damage_detector.py --data /path/to/DATA --epochs 40

On Apple Silicon this uses the GPU (MPS) automatically; on CPU it is slow but works.
The result is saved to fleet_ai/models/damage_detector (DAMAGE_DETECTOR_PATH).
"""
import argparse
import json
import os
import random
from typing import Any, Dict, List

import torch
from PIL import Image
from transformers import (
    AutoImageProcessor,
    AutoModelForObjectDetection,
    Trainer,
    TrainingArguments,
)

DEFAULT_BASE = "PekingU/rtdetr_v2_r18vd"  # Apache-2.0, ~20M params, fast on CPU
DEFAULT_OUT = os.path.join(os.path.dirname(__file__), "..", "fleet_ai", "models", "damage_detector")

CANONICAL = {
    "scratch": "scratch", "scratches": "scratch",
    "dent": "dent", "dents": "dent",
    "crack": "crack", "cracks": "crack",
    "lamp broken": "lamp_broken", "lamp_broken": "lamp_broken", "broken lamp": "lamp_broken",
    "headlight": "lamp_broken", "broken headlight": "lamp_broken",
    "glass shatter": "glass_shatter", "glass_shatter": "glass_shatter", "broken glass": "glass_shatter",
    "tire flat": "tire_flat", "tire_flat": "tire_flat", "flat tire": "tire_flat",
}


def load_split(split_dir: str, label_map: Dict[str, str]):
    with open(os.path.join(split_dir, "_annotations.coco.json"), encoding="utf-8") as f:
        coco = json.load(f)
    cat_names = {}
    for c in coco["categories"]:
        raw = c["name"].strip().lower()
        cat_names[c["id"]] = label_map.get(raw, CANONICAL.get(raw, raw.replace(" ", "_")))
    by_image: Dict[int, List[Dict[str, Any]]] = {}
    for a in coco["annotations"]:
        by_image.setdefault(a["image_id"], []).append(a)
    items = [
        {"path": os.path.join(split_dir, img["file_name"]), "id": img["id"], "anns": by_image.get(img["id"], [])}
        for img in coco["images"]
    ]
    return items, cat_names


class CocoDetection(torch.utils.data.Dataset):
    def __init__(self, items, cat_to_label_id, processor, train: bool):
        self.items, self.cat_to_label_id, self.processor, self.train = items, cat_to_label_id, processor, train

    def __len__(self):
        return len(self.items)

    def __getitem__(self, idx):
        item = self.items[idx]
        image = Image.open(item["path"]).convert("RGB")
        anns = [
            {"bbox": a["bbox"], "category_id": self.cat_to_label_id[a["category_id"]],
             "area": a.get("area", a["bbox"][2] * a["bbox"][3]), "iscrowd": a.get("iscrowd", 0)}
            for a in item["anns"] if a["category_id"] in self.cat_to_label_id
        ]
        if self.train and random.random() < 0.5:  # horizontal flip: cheap and safe for cars
            image = image.transpose(Image.FLIP_LEFT_RIGHT)
            for a in anns:
                x, y, w, h = a["bbox"]
                a["bbox"] = [image.width - x - w, y, w, h]
        enc = self.processor(images=image, annotations={"image_id": item["id"], "annotations": anns},
                             return_tensors="pt")
        return {"pixel_values": enc["pixel_values"][0], "labels": enc["labels"][0]}


def collate(batch):
    return {
        "pixel_values": torch.stack([b["pixel_values"] for b in batch]),
        "labels": [b["labels"] for b in batch],
    }


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--data", required=True)
    ap.add_argument("--base", default=DEFAULT_BASE, help="HF model id or local dir")
    ap.add_argument("--out", default=DEFAULT_OUT)
    ap.add_argument("--epochs", type=float, default=40)
    ap.add_argument("--batch", type=int, default=8)
    ap.add_argument("--lr", type=float, default=1e-4)
    ap.add_argument("--image-size", type=int, default=640)
    ap.add_argument("--label-map", help="JSON file: dataset category name -> label")
    ap.add_argument("--max-steps", type=int, default=-1, help="for smoke tests")
    args = ap.parse_args()

    label_map = {}
    if args.label_map:
        with open(args.label_map, encoding="utf-8") as f:
            label_map = {k.lower(): v for k, v in json.load(f).items()}

    train_items, train_cats = load_split(os.path.join(args.data, "train"), label_map)
    valid_items, valid_cats = load_split(os.path.join(args.data, "valid"), label_map)

    labels = sorted({n for n in train_cats.values()})
    label2id = {n: i for i, n in enumerate(labels)}
    id2label = {i: n for n, i in label2id.items()}
    print(f"Classes: {labels}  train={len(train_items)}  valid={len(valid_items)}")

    processor = AutoImageProcessor.from_pretrained(
        args.base, size={"height": args.image_size, "width": args.image_size}
    )
    model = AutoModelForObjectDetection.from_pretrained(
        args.base, id2label=id2label, label2id=label2id, ignore_mismatched_sizes=True
    )

    train_ds = CocoDetection(train_items, {c: label2id[n] for c, n in train_cats.items()}, processor, True)
    valid_ds = CocoDetection(valid_items, {c: label2id[n] for c, n in valid_cats.items() if n in label2id},
                             processor, False)

    training_args = TrainingArguments(
        output_dir=os.path.join(args.out, "_checkpoints"),
        num_train_epochs=args.epochs,
        max_steps=args.max_steps,
        per_device_train_batch_size=args.batch,
        per_device_eval_batch_size=args.batch,
        learning_rate=args.lr,
        weight_decay=1e-4,
        warmup_steps=0.05,  # <1 = fraction of total steps (transformers 5)
        lr_scheduler_type="cosine",
        eval_strategy="epoch" if args.max_steps < 0 else "no",
        save_strategy="epoch" if args.max_steps < 0 else "no",
        save_total_limit=2,
        load_best_model_at_end=args.max_steps < 0,
        metric_for_best_model="eval_loss",
        logging_steps=20,
        remove_unused_columns=False,
        dataloader_num_workers=0,
        report_to=[],
    )
    Trainer(model=model, args=training_args, train_dataset=train_ds, eval_dataset=valid_ds,
            data_collator=collate).train()

    os.makedirs(args.out, exist_ok=True)
    model.save_pretrained(args.out)
    processor.save_pretrained(args.out)
    print(f"Saved to {os.path.abspath(args.out)}")


if __name__ == "__main__":
    main()
