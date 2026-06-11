"""Unit-тесты маппинга статусов OCR-движков (без torch/easyocr — Reader замокан).
Запуск: pip install pillow numpy && python -m unittest test_app -v"""
import io
import os
import unittest

os.environ.setdefault("OCR_ENGINE", "easyocr")  # конструктор движка ленивый, torch не импортируется
import app  # noqa: E402

from PIL import Image  # noqa: E402


def _png_bytes():
    buf = io.BytesIO()
    Image.new("RGB", (40, 20), "white").save(buf, format="PNG")
    return buf.getvalue()


class FakeReader:
    def __init__(self, detections):
        self._d = detections
    def readtext(self, arr, **kwargs):  # detail/paragraph/text_threshold/low_text/mag_ratio
        return self._d


class StatusMappingTest(unittest.TestCase):
    def test_stub_engine_needs_review(self):
        self.assertEqual(app.StubOcrEngine().recognize(_png_bytes())["status"], 2)

    def test_text_found_needs_review(self):
        eng = app.EasyOcrEngine()
        eng._reader = FakeReader([([[0, 0]], "Состав: вода", 0.88)])
        r = eng.recognize(_png_bytes())
        self.assertEqual(r["status"], 2)
        self.assertEqual(r["rawText"], "Состав: вода")
        self.assertAlmostEqual(r["confidence"], 0.88, places=2)
        self.assertIsNone(r["errorCode"])

    def test_empty_text_photo_unreadable(self):
        eng = app.EasyOcrEngine()
        eng._reader = FakeReader([])
        r = eng.recognize(_png_bytes())
        self.assertEqual(r["status"], 3)
        self.assertEqual(r["rawText"], "")
        self.assertEqual(r["errorCode"], "PHOTO_UNREADABLE")

    def test_low_confidence_photo_unreadable(self):
        eng = app.EasyOcrEngine()
        eng._reader = FakeReader([([[0, 0]], "x", 0.01)])
        self.assertEqual(eng.recognize(_png_bytes())["status"], 3)

    def test_bad_image_error(self):
        eng = app.EasyOcrEngine()
        eng._reader = FakeReader([])
        r = eng.recognize(b"not-an-image")
        self.assertEqual(r["status"], 5)
        self.assertEqual(r["errorCode"], "IMAGE_DECODE_ERROR")


if __name__ == "__main__":
    unittest.main()
