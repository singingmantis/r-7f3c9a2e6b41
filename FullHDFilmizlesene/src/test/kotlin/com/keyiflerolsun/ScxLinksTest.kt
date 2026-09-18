package com.keyiflerolsun
import org.junit.Assert.*
import org.junit.Test

class ScxLinksTest {
    private fun encoded(url: String): String = java.util.Base64.getEncoder().encodeToString(url.toByteArray()).map { c -> when(c) {
        in 'a'..'z' -> ('a'.code + (c - 'a' + 13) % 26).toChar()
        in 'A'..'Z' -> ('A'.code + (c - 'A' + 13) % 26).toChar()
        else -> c
    }}.joinToString("")
    @Test fun findsLaterScriptAndKeepsEveryMirrorSeparate() {
        val first = "https://video.example/one"
        val second = "https://video.example/two"
        val html = """<script>var analytics = 1;</script><script>var scx={"fast":{"sx":{"t":["${encoded(first)}","${encoded(second)}"]}}}; init();</script>"""
        assertEquals(listOf("fast" to first, "fast" to second), extractScxLinks(html))
    }
    @Test fun acceptsLanguageMapAndIgnoresMalformedEntry() {
        val html = """<script>scx = {"atom":{"sx":{"t":{"en":"${encoded("https://video.example/original")}","tr":"invalid!"}}}};</script>"""
        assertEquals(listOf("en" to "https://video.example/original"), extractScxLinks(html))
    }
    @Test fun missingConfigReturnsNoLinks() { assertTrue(extractScxLinks("<script>var x = 1;</script>").isEmpty()) }
}
