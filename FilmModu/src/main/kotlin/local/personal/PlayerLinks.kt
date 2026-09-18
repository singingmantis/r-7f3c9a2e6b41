package local.personal

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.network.CloudflareKiller
import com.lagradost.cloudstream3.utils.*
import kotlinx.coroutines.CancellationException
import java.net.URI

/** Resolve each embedded player independently so a broken mirror cannot hide working ones. */
suspend fun resolvePersonalPlayer(url: String, referer: String, subtitleCallback: (SubtitleFile) -> Unit,
    callback: (ExtractorLink) -> Unit, depth: Int = 0, sourceName: String = "Pilavyer",
    preferOriginal: Boolean = false): Boolean {
    if (depth > 3) return false
    val response = app.get(url, referer = referer, interceptor = CloudflareKiller())
    val document = response.document
    val configText = Regex("""window\.__PLAYER__\s*=\s*(\{.*?\});""", RegexOption.DOT_MATCHES_ALL)
        .find(response.text)?.groupValues?.get(1)
    if (configText != null) {
        val config = jacksonObjectMapper().readTree(configText)
        val stream = config.path("stream").asText("")
        if (stream.startsWith("https://")) {
            val origin = URI(url).let { "${it.scheme}://${it.authority}" }
            playerSubtitles(config, url).forEach { (label, subUrl) ->
                subtitleCallback(SubtitleFile(label, subUrl))
            }
            callback(newExtractorLink(sourceName, "Pilavyer" + if (preferOriginal) " • Özgün ses" else " • Çoklu ses", stream, ExtractorLinkType.M3U8) {
                this.referer = "$origin/"
                this.headers = mapOf("Origin" to origin)
                this.quality = Qualities.Unknown.value
                if (preferOriginal) this.extractorData = config.path("audios").toString()
            })
            return true
        }
    }
    val targets = document.select("iframe[src], iframe[data-src]").mapNotNull {
        val src = it.attr("src").ifBlank { it.attr("data-src") }.trim()
        runCatching { URI(url).resolve(src).toString() }.getOrNull()
    }.toMutableList()
    val slug = document.selectFirst("[data-pv]")?.attr("data-pv")
    val core = document.select("script[src]").firstOrNull { it.attr("src").contains("/assets/js/core.js") }?.attr("src")
    if (!slug.isNullOrBlank() && core != null) {
        val base = URI(url).resolve(core).toString().substringBefore("/assets/js/core.js")
        targets.add("$base/assets/js/s.php?s=${java.net.URLEncoder.encode(slug, "UTF-8")}")
    }
    var found = false
    for (target in targets.distinct().filter { it.startsWith("http") && it != url }) {
        try {
            if (target.contains("/assets/js/s.php")) {
                found = resolvePersonalPlayer(target, url, subtitleCallback, callback, depth + 1, sourceName, preferOriginal) || found
            } else {
                loadExtractor(target, url, subtitleCallback) { found = true; callback(it) }
            }
        } catch (e: CancellationException) { throw e }
        catch (_: Exception) { /* Continue to the next mirror. */ }
    }
    for (source in document.select("video[src], video source[src]")) {
        val media = URI(url).resolve(source.attr("src")).toString()
        val type = if (media.contains(".m3u8")) ExtractorLinkType.M3U8 else ExtractorLinkType.VIDEO
        callback(newExtractorLink(sourceName, "Video", media, type) { this.referer = url })
        found = true
    }
    return found
}
