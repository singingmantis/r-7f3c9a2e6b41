package com.keyiflerolsun

import android.util.Base64
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.lagradost.cloudstream3.SubtitleFile
import com.lagradost.cloudstream3.app
import com.lagradost.cloudstream3.newSubtitleFile
import com.lagradost.cloudstream3.utils.ExtractorApi
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.ExtractorLinkType
import com.lagradost.cloudstream3.utils.Qualities
import com.lagradost.cloudstream3.utils.newExtractorLink
import java.net.URI

/** Resolves both current RapidVid player variants used by FullHDFilmizlesene. */
open class RapidVid : ExtractorApi() {
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
        var videoUrl: String? = null

        Regex("""window\._p8\s*=\s*['"]([^'"]+)['"]""").find(html)?.groupValues?.get(1)?.let { blob ->
            runCatching {
                val cfg = jacksonObjectMapper().readTree(decodeImgzBlob(blob))
                videoUrl = cfg.path("cm").asText().ifBlank { cfg.path("tm").asText() }
                cfg.path("ct").forEach { track ->
                    val file = track.path("file").asText()
                    if (file.startsWith("http")) subtitleCallback(newSubtitleFile(track.path("label").asText("Altyazı"), file))
                }
            }
        }

        if (videoUrl.isNullOrBlank()) {
            Regex("""['"]file['"]\s*:\s*(?:av|_)\(\s*['"]([^'"]+)['"]\s*\)""")
                .find(html)?.groupValues?.get(1)?.let { videoUrl = runCatching { decodeImgzBlob(it) }.getOrNull() }
        }
        if (videoUrl.isNullOrBlank()) {
            videoUrl = Regex("""https?://[^'"\\\s,}]+""").findAll(html)
                .map { it.value.replace("\\/", "/") }
                .firstOrNull { it.contains(".m3u8") || it.contains("imageshub") || it.contains("imagehub") }
        }

        parseTracks(html).forEach { (label, file) -> subtitleCallback(newSubtitleFile(label, file)) }
        val stream = videoUrl?.takeIf { it.startsWith("http") } ?: return
        callback(newExtractorLink(name, name, stream, ExtractorLinkType.M3U8) {
            this.referer = "$origin/"
            this.quality = Qualities.Unknown.value
            this.headers = mapOf("Referer" to "$origin/", "Origin" to origin)
        })
    }

    internal fun decodeImgzBlob(blob: String): String {
        val first = String(Base64.decode(blob.reversed(), Base64.DEFAULT), Charsets.ISO_8859_1)
        val key = "K9L"
        val shifted = buildString(first.length) {
            first.forEachIndexed { index, char -> append((char.code - (key[index % key.length].code % 5 + 1)).toChar()) }
        }
        return String(Base64.decode(shifted, Base64.DEFAULT), Charsets.UTF_8)
    }

    private fun parseTracks(html: String): List<Pair<String, String>> {
        return Regex("""\{[^{}]*['"]file['"]\s*:\s*['"]([^'"]+)['"][^{}]*['"]label['"]\s*:\s*['"]([^'"]+)['"][^{}]*\}""")
            .findAll(html).mapNotNull {
                val file = it.groupValues[1].replace("\\/", "/")
                if (file.startsWith("http") && !file.contains(".m3u8")) it.groupValues[2] to file else null
            }.distinct().toList()
    }
}
