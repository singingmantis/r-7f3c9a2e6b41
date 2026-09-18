package local.personal

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import java.net.URI

/** Resolve normal embedded players, including the observed Pilavyer bootstrap. */
suspend fun resolvePersonalPlayer(url: String, referer: String, subtitleCallback: (SubtitleFile) -> Unit,
    callback: (ExtractorLink) -> Unit, depth: Int = 0): Boolean {
    if (depth > 3) return false
    val response = app.get(url, referer = referer)
    val document = response.document
    val configText = Regex("""window\.__PLAYER__\s*=\s*(\{.*?\});""", RegexOption.DOT_MATCHES_ALL)
        .find(response.text)?.groupValues?.get(1)
    if (configText != null) {
        val config = jacksonObjectMapper().readTree(configText)
        val stream = config.path("stream").asText("")
        if (stream.startsWith("https://")) {
            val origin = URI(url).let { "${it.scheme}://${it.authority}" }
            config.path("subs").forEach { sub ->
                val subUrl = sub.path("file").asText("").ifBlank { sub.path("url").asText("") }
                if (subUrl.isNotBlank()) subtitleCallback(SubtitleFile(sub.path("label").asText("Altyazı"), URI(url).resolve(subUrl).toString()))
            }
            callback(newExtractorLink("Pilavyer", "Pilavyer", stream, ExtractorLinkType.M3U8) {
                this.referer = "$origin/"
                this.headers = mapOf("Origin" to origin)
                this.quality = Qualities.Unknown.value
            })
            return true
        }
    }
    val targets = document.select("iframe[src]").map { URI(url).resolve(it.attr("src")).toString() }.toMutableList()
    val slug = document.selectFirst("[data-pv]")?.attr("data-pv")
    val core = document.select("script[src]").firstOrNull { it.attr("src").contains("/assets/js/core.js") }?.attr("src")
    if (!slug.isNullOrBlank() && core != null) {
        val base = URI(url).resolve(core).toString().substringBefore("/assets/js/core.js")
        targets.add("$base/assets/js/s.php?s=${java.net.URLEncoder.encode(slug, "UTF-8")}")
    }
    var found = false
    for (target in targets.distinct().filter { it.startsWith("http") && it != url }) {
        if (target.contains("/assets/js/s.php")) {
            found = resolvePersonalPlayer(target, url, subtitleCallback, callback, depth + 1) || found
        } else {
            loadExtractor(target, url, subtitleCallback) { found = true; callback(it) }
        }
    }
    for (source in document.select("video[src], video source[src]")) {
        val media = URI(url).resolve(source.attr("src")).toString()
        val type = if (media.contains(".m3u8")) ExtractorLinkType.M3U8 else ExtractorLinkType.VIDEO
        callback(newExtractorLink("Video", "Video", media, type) { this.referer = url })
        found = true
    }
    return found
}
