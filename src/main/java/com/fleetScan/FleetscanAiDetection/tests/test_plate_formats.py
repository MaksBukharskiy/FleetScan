"""Run from FleetscanAiDetection/:  python -m pytest tests  (or python -m unittest discover tests)"""
import unittest

from fleet_ai.plate_formats import FORMATS, match_plate, normalize, format_display


def plate(text):
    found = match_plate(text, FORMATS)
    return found[0] if found else None


class PlateFormatsTest(unittest.TestCase):
    def test_uzbek_private(self):
        self.assertEqual(plate("01 A 123 BC"), "01A123BC")
        self.assertEqual(plate("UZ 30 K 777 AA"), "30K777AA")

    def test_uzbek_legal_entity(self):
        self.assertEqual(plate("01 123 ABC"), "01123ABC")

    def test_ocr_confusions_are_fixed_by_position(self):
        # O read instead of 0 in region, 8 instead of B in letters
        self.assertEqual(plate("O1A123 8C"), "01A123BC")

    def test_unknown_uzbek_region_rejected(self):
        self.assertIsNone(match_plate("02A123BC", [f for f in FORMATS if f.country == "UZ"]))

    def test_russian_still_supported(self):
        self.assertEqual(plate("A123BC77"), "A123BC77")
        self.assertEqual(plate("А123ВС777"), "A123BC777")  # Cyrillic input

    def test_garbage(self):
        self.assertIsNone(plate("HELLO"))
        self.assertIsNone(plate(""))

    def test_normalize_and_display(self):
        self.assertEqual(normalize(" 01-a 123 bc "), "01A123BC")
        fmt = next(f for f in FORMATS if f.name == "uz_private")
        self.assertEqual(format_display("01A123BC", fmt), "01 A 123 BC")


if __name__ == "__main__":
    unittest.main()
