"""Check subtitle patterns with ICU, the regex engine used by Android."""
import ctypes
import ctypes.util
import os
from pathlib import Path
import re
import unittest


class SubtitleRegexTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        library = "icu.dll" if os.name == "nt" else ctypes.util.find_library("icui18n")
        if not library:
            raise RuntimeError("ICU is required to validate Android regex compatibility")
        cls.icu = ctypes.CDLL(library)

        def symbol(name):
            for suffix in [""] + ["_" + str(n) for n in range(100, 59, -1)]:
                try:
                    return getattr(cls.icu, name + suffix)
                except AttributeError:
                    pass
            raise RuntimeError("Missing ICU symbol: " + name)

        cls.open = staticmethod(symbol("uregex_open"))
        cls.open.argtypes = [ctypes.c_void_p, ctypes.c_int32, ctypes.c_uint32,
                            ctypes.c_void_p, ctypes.POINTER(ctypes.c_int32)]
        cls.open.restype = ctypes.c_void_p
        cls.close = staticmethod(symbol("uregex_close"))
        cls.close.argtypes = [ctypes.c_void_p]
        cls.set_text = staticmethod(symbol("uregex_setText"))
        cls.set_text.argtypes = [ctypes.c_void_p, ctypes.c_void_p, ctypes.c_int32,
                                ctypes.POINTER(ctypes.c_int32)]
        cls.find_next = staticmethod(symbol("uregex_findNext"))
        cls.find_next.argtypes = [ctypes.c_void_p, ctypes.POINTER(ctypes.c_int32)]
        cls.find_next.restype = ctypes.c_int8

    def matches(self, pattern, text):
        encoded_pattern = ctypes.create_string_buffer(pattern.encode("utf-16-le"))
        error = ctypes.c_int32(0)
        handle = self.open(encoded_pattern, len(pattern), 32, None, ctypes.byref(error))
        self.assertEqual(error.value, 0, "Android ICU rejected: " + pattern)
        encoded_text = ctypes.create_string_buffer(text.encode("utf-16-le"))
        try:
            self.set_text(handle, encoded_text, len(text), ctypes.byref(error))
            count = 0
            while self.find_next(handle, ctypes.byref(error)):
                count += 1
            self.assertEqual(error.value, 0)
            return count
        finally:
            self.close(handle)

    def test_rapidvid_subtitle_pattern(self):
        source = (Path(__file__).resolve().parents[1] / "FullHDFilmizlesene/src/main/kotlin/com/keyiflerolsun/RapidVidExtractor.kt").read_text(encoding="utf-8")
        section = source.split("private fun parseTracks")[1]
        pattern = re.search(r'Regex\("""(.*?)"""', section, re.S).group(1)
        sample = '{"file":"https://example.org/tr.vtt","label":"Turkish"}'
        self.assertEqual(self.matches(pattern, sample), 1)

    def test_subtitle_patterns_from_provider(self):
        source = (Path(__file__).resolve().parents[1] / "HDFilmCehennemi/src/main/kotlin/com/keyiflerolsun/HDFilmCehennemi.kt").read_text(encoding="utf-8")
        section = source.split("val subtitleUrls =")[1].split("catch (e:")[0]
        patterns = re.findall(r'Regex\("""(.*?)"""', section, re.S)
        self.assertEqual(len(patterns), 4)
        sample = 'tracks: [{"file":"https:\\/\\/example.org\\/full.vtt","kind":"captions","label":"Turkish"},{"file":"https:\\/\\/example.org\\/forced.vtt","kind":"captions","label":"Forced"}]'
        for pattern, expected in zip(patterns, [1, 2, 2, 2]):
            self.assertEqual(self.matches(pattern, sample), expected)


if __name__ == "__main__":
    unittest.main()
