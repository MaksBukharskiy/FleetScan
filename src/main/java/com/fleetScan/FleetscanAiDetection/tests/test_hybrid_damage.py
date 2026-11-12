"""Routing tests for the hybrid damage pipeline (local detector + LLM). No models or network needed."""
import sys
import types
import unittest
from unittest import mock

sys.modules.setdefault("easyocr", types.SimpleNamespace(Reader=lambda *a, **k: None))

from fleet_ai import vehicle_analyzer as va  # noqa: E402

IMG = "fleet_ai/samples/10967img1.jpg"


def local(*dets):
    detections = [{"label": l, "score": s, "severity": sev, "box": [0, 0, 1, 1], "box_rel": [0, 0, 1, 1]}
                  for l, s, sev in dets]
    sev = va.damage_local.max_severity([d["severity"] for d in detections])
    return {"detections": detections, "severity": sev}


LLM_OK = {"is_vehicle_photo": True, "photo_quality": "good", "overall_severity": "minor",
          "damages": [], "confidence": 0.9, "summary": "мелкая царапина"}


class HybridRoutingTest(unittest.TestCase):
    def run_with(self, local_result, llm_result=LLM_OK, audit=0.0):
        llm = mock.Mock(return_value=llm_result)
        with mock.patch.object(va.damage_local, "detect", return_value=local_result), \
             mock.patch.object(va.damage_llm, "assess_damage", llm), \
             mock.patch.object(va, "AUDIT_RATE", audit):
            return va.assess_vehicle_condition(IMG), llm

    def test_clean_photo_decided_locally(self):
        r, llm = self.run_with(local())
        self.assertEqual((r["condition"], r["damage_method"]), ("ok", "local"))
        llm.assert_not_called()

    def test_clean_photo_audited_sometimes(self):
        r, llm = self.run_with(local(), audit=1.0)
        self.assertEqual(r["damage_method"], "hybrid")
        llm.assert_called_once()

    def test_confident_dent_decided_locally(self):
        r, llm = self.run_with(local(("dent", 0.9, "moderate")))
        self.assertEqual((r["condition"], r["damage_method"], r["damage_severity"]), ("defect", "local", "moderate"))
        llm.assert_not_called()

    def test_unsure_detection_goes_to_llm_with_hint(self):
        loc = local(("dent", 0.4, "moderate"))
        r, llm = self.run_with(loc)
        self.assertEqual((r["condition"], r["damage_method"]), ("ok", "hybrid"))  # LLM said minor
        self.assertIs(llm.call_args.kwargs["local_hint"], loc)

    def test_severe_always_confirmed_by_llm(self):
        r, llm = self.run_with(local(("glass_shatter", 0.95, "severe")))
        llm.assert_called_once()

    def test_llm_unavailable_falls_back_to_local(self):
        r, _ = self.run_with(local(("dent", 0.4, "moderate")), llm_result=None)
        self.assertEqual((r["condition"], r["damage_method"]), ("defect", "local"))
        self.assertAlmostEqual(r["condition_confidence"], 0.4)

    def test_no_local_model_uses_llm_only(self):
        r, llm = self.run_with(None)
        self.assertEqual(r["damage_method"], "llm")

    def test_invalid_photo(self):
        bad = dict(LLM_OK, is_vehicle_photo=False)
        r, _ = self.run_with(None, llm_result=bad)
        self.assertEqual(r["condition"], "invalid_photo")


if __name__ == "__main__":
    unittest.main()
