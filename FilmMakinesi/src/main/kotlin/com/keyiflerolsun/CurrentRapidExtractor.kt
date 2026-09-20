package com.keyiflerolsun

import android.util.Base64
import com.lagradost.cloudstream3.SubtitleFile
import com.lagradost.cloudstream3.app
import com.lagradost.cloudstream3.utils.ExtractorApi
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.ExtractorLinkType
import com.lagradost.cloudstream3.utils.Qualities
import com.lagradost.cloudstream3.utils.newExtractorLink
import java.net.URI

/** Current RapidVid/imgz player used by some FilmMakinesi mirrors. */
class CurrentRapidExtractor : ExtractorApi() {
    override val name = "RapidVid"
    override val mainUrl = "https://rapidvid.net"
    override val requiresReferer = true

    override suspend fun getUrl(
        url: String,
        referer: String?,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {
        val origin = runCatching { URI(url).let { "${it.scheme}://${it.host}" } }.getOrDefault(mainUrl)
        val html = app.get(url, referer = "$origin/").text
        var stream: String? = null
        Regex("""window\._p8\s*=\s*['"]([^'"]+)['"]""").find(html)?.groupValues?.get(1)?.let { blob ->
            val json = runCatching { decode(blob) }.getOrNull().orEmpty()
            stream = Regex("""["']cm["']\s*:\s*["']([^"']+)["']""").find(json)?.groupValues?.get(1)
                ?: Regex("""["']tm["']\s*:\s*["']([^"']+)["']""").find(json)?.groupValues?.get(1)
        }
        if (stream.isNullOrBlank()) {
            Regex("""['"]file['"]\s*:\s*(?:av|_)\(\s*['"]([^'"]+)['"]\s*\)""")
                .find(html)?.groupValues?.get(1)?.let { stream = runCatching { decode(it) }.getOrNull() }
        }
        if (stream.isNullOrBlank()) {
            stream = Regex("""https?://[^'"\\\s,}]+""").findAll(html)
                .map { it.value.replace("\\/", "/") }
                .firstOrNull { it.contains(".m3u8") || it.contains("imageshub") || it.contains("imagehub") }
        }
        val videoUrl = stream?.takeIf { it.startsWith("http") } ?: return
        callback(newExtractorLink(name, name, videoUrl, ExtractorLinkType.M3U8) {
            this.referer = "$origin/"
            this.quality = Qualities.Unknown.value
            this.headers = mapOf("Referer" to "$origin/", "Origin" to origin)
        })
    }

    private fun decode(blob: String): String {
        val first = String(Base64.decode(blob.reversed(), Base64.DEFAULT), Charsets.ISO_8859_1)
        val key = "K9L"
        val shifted = buildString(first.length) {
            first.forEachIndexed { index, char -> append((char.code - (key[index % key.length].code % 5 + 1)).toChar()) }
        }
        return String(Base64.decode(shifted, Base64.DEFAULT), Charsets.UTF_8)
    }
}
