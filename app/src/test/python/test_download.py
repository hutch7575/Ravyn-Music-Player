"""Contract tests for Android's Python bridge, with no network or yt-dlp installation."""
import importlib.util
import json
from pathlib import Path
import sys
import types
import unittest
from unittest.mock import MagicMock, patch

sys.modules.setdefault("yt_dlp", types.SimpleNamespace(YoutubeDL=None))
spec = importlib.util.spec_from_file_location("download", Path(__file__).parents[2] / "main/python/download.py")
download = importlib.util.module_from_spec(spec)
spec.loader.exec_module(download)


class ExtractionContractTest(unittest.TestCase):
    def extract(self, info, video_id="dQw4w9WgXcQ"):
        client = MagicMock()
        client.__enter__.return_value.extract_info.return_value = info
        with patch.object(download.yt_dlp, "YoutubeDL", return_value=client) as constructor:
            result = download.download("/native/libqjs.so", video_id)
        return json.loads(result), client, constructor

    def test_canonical_watch_url_and_progressive_audio(self):
        result, client, constructor = self.extract({"id": "dQw4w9WgXcQ", "url": "https://audio.example/song", "ext": "m4a"})
        client.__enter__.return_value.extract_info.assert_called_once_with(
            "https://www.youtube.com/watch?v=dQw4w9WgXcQ", download=False)
        self.assertIn("protocol=https", constructor.call_args.args[0]["format"])
        self.assertEqual(result["mime_type"], "audio/mp4")

    def test_invalid_ids_fail_before_network(self):
        for value in ("", "short", "https://youtube.com/watch?v=dQw4w9WgXcQ", "dQw4w9WgXc!"):
            with self.subTest(value=value), patch.object(download.yt_dlp, "YoutubeDL") as constructor:
                with self.assertRaises(ValueError):
                    download.download("qjs", value)
                constructor.assert_not_called()

    def test_missing_or_unsupported_url_fails(self):
        for info in (None, {}, {"url": "file:///etc/passwd"}, {"url": "ftp://audio.example/song"}):
            with self.subTest(info=info), self.assertRaises(ValueError):
                self.extract(info)

    def test_exact_and_approximate_sizes_remain_distinct(self):
        result, _, _ = self.extract({"url": "https://audio.example/a", "filesize_approx": 500})
        self.assertIsNone(result["filesize"])
        self.assertEqual(result["filesize_approx"], 500)

    def test_only_selected_format_crosses_bridge(self):
        result, _, _ = self.extract({"url": "https://audio.example/a", "format_id": "251", "duration": 123.5,
            "formats": [{"format_id": "140", "abr": 128}, {"format_id": "251", "abr": 160, "filesize": 400}],
            "thumbnail": "unused", "description": "x" * 10000})
        self.assertEqual(result["formats"], [{"format_id": "251", "abr": 160}])
        self.assertEqual(result["filesize"], 400)
        self.assertEqual(result["duration"], 123.5)
        self.assertNotIn("description", result)
        self.assertLess(len(json.dumps(result)), 500)

    def test_extraction_errors_propagate(self):
        client = MagicMock()
        client.__enter__.return_value.extract_info.side_effect = RuntimeError("network failed")
        with patch.object(download.yt_dlp, "YoutubeDL", return_value=client), self.assertRaises(RuntimeError):
            download.download("qjs", "dQw4w9WgXcQ")


if __name__ == "__main__":
    unittest.main()
