// Adapted from @keyiflerolsun / @KekikAkademi; attribution retained.
package com.keyiflerolsun

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.jsoup.Jsoup
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*

internal fun readOkMetadata(html: String): JsonNode? {
    val mapper = jacksonObjectMapper()
    for (element in Jsoup.parse(html).select("[data-options]")) {
        val options = runCatching { mapper.readTree(element.attr("data-options")) }.getOrNull() ?: continue
        val value = options.path("flashvars").path("metadata")
        val metadata = if (value.isTextual) runCatching { mapper.readTree(value.asText()) }.getOrNull() else value
        if (metadata != null && (metadata.has("videos") || metadata.has("hlsManifestUrl"))) return metadata
    }
    return null
}

open class Odnoklassniki : ExtractorApi() {
    override val name = "Odnoklassniki"
    override val mainUrl = "https://odnoklassniki.ru"
    override val requiresReferer = false

    override suspend fun getUrl(url: String, referer: String?, subtitleCallback: (SubtitleFile) -> Unit, callback: (ExtractorLink) -> Unit) {
        val requestHeaders = mapOf("User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/131.0.0.0 Safari/537.36")
        val metadata = readOkMetadata(app.get(url, referer = referer, headers = requestHeaders).text)
            ?: throw ErrorLoadingException("OK.ru video bilgisi bulunamadı")
        val qualities = mapOf("mobile" to 144, "lowest" to 240, "low" to 360, "sd" to 480, "hd" to 720, "full" to 1080, "quad" to 1440, "ultra" to 2160)
        for (video in metadata.path("videos")) {
            val raw = video.path("url").asText("")
            val media = if (raw.startsWith("//")) "https:$raw" else raw
            if (!media.startsWith("http")) continue
            callback(newExtractorLink(name, name, media, ExtractorLinkType.VIDEO) {
                this.referer = "https://ok.ru/"
                this.headers = requestHeaders
                this.quality = qualities[video.path("name").asText("")] ?: Qualities.Unknown.value
            })
        }
        val hls = metadata.path("hlsManifestUrl").asText("")
        if (hls.startsWith("https://")) callback(newExtractorLink(name, "$name HLS", hls, ExtractorLinkType.M3U8) {
            this.referer = "https://ok.ru/"
            this.headers = requestHeaders
        })
    }
}
