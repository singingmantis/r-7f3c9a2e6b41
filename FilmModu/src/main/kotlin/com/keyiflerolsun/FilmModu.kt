package com.keyiflerolsun

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import local.personal.evfilmmodu.BuildConfig
import local.personal.resolvePersonalPlayer
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.net.URLEncoder

class FilmModu : MainAPI() {
    override var mainUrl = BuildConfig.SITE_URL
    override var name = "FilmModu (Kişisel)"
    override var lang = "tr"
    override val hasMainPage = true
    override val supportedTypes = setOf(TvType.Movie, TvType.TvSeries)
    override val mainPage = mainPageOf("$mainUrl/filmler" to "Filmler", "$mainUrl/diziler" to "Diziler")

    private fun Element.result(): SearchResponse? {
        val href = fixUrl(attr("href"))
        if (!Regex("/((film)|(dizi))/[^/?#]+/?$").containsMatchIn(href)) return null
        val img = selectFirst("img") ?: return null
        val title = img.attr("alt").ifBlank { text().trim() }
        if (title.isBlank()) return null
        val poster = fixUrlNull(img.attr("src").ifBlank { img.attr("data-src") })
        return if (href.contains("/dizi/")) newTvSeriesSearchResponse(title, href, TvType.TvSeries) { posterUrl = poster }
        else newMovieSearchResponse(title, href, TvType.Movie) { posterUrl = poster }
    }
    private fun Document.results() = select("main a[href], article a[href]").mapNotNull { it.result() }.distinctBy { it.url }
    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val doc = app.get("${request.data}?page=$page").document
        return newHomePageResponse(listOf(HomePageList(request.name, doc.results())), doc.select("a[rel=next]").isNotEmpty())
    }
    override suspend fun search(query: String): List<SearchResponse> =
        app.get("$mainUrl/ara?q=${URLEncoder.encode(query, "UTF-8")}").document.results()

    private fun Document.metadata(): JsonNode? {
        for (script in select("script[type=application/ld+json]")) {
            val root = runCatching { jacksonObjectMapper().readTree(script.data()) }.getOrNull() ?: continue
            val nodes = if (root.has("@graph")) root.path("@graph").toList() else listOf(root)
            nodes.firstOrNull { it.path("@type").asText() in listOf("Movie", "TVSeries") }?.let { return it }
        }
        return null
    }
    override suspend fun load(url: String): LoadResponse? {
        val doc = app.get(url).document
        val meta = doc.metadata()
        val title = meta?.path("name")?.asText()?.takeIf { it.isNotBlank() } ?: doc.selectFirst("h1")?.text() ?: return null
        val poster = meta?.path("image")?.asText()?.takeIf { it.startsWith("http") }
            ?: fixUrlNull(doc.selectFirst("meta[property=og:image]")?.attr("content"))
        val plot = meta?.path("description")?.asText()
        val year = meta?.path("datePublished")?.asText()?.take(4)?.toIntOrNull()
        if (url.contains("/dizi/")) {
            val pages = mutableListOf(doc)
            val seasonLinks = doc.select("a[href*=sezon=]").map { fixUrl(it.attr("href")).substringBefore('#') }.distinct()
            for (season in seasonLinks) pages.add(app.get(season).document)
            val episodes = pages.flatMap { it.select("a[href*=/bolum-]") }.mapNotNull { a ->
                val href = fixUrl(a.attr("href"))
                val match = Regex("/sezon-(\\d+)/bolum-(\\d+)").find(href) ?: return@mapNotNull null
                newEpisode(href) {
                    season = match.groupValues[1].toInt(); episode = match.groupValues[2].toInt()
                    name = a.text().trim().takeIf { it.isNotBlank() }
                }
            }.distinctBy { it.data }.sortedWith(compareBy({ it.season }, { it.episode }))
            return newTvSeriesLoadResponse(title, url, TvType.TvSeries, episodes) { posterUrl = poster; this.plot = plot; this.year = year }
        }
        val watch = doc.selectFirst("a[href*=/izle]")?.attr("href")?.let { fixUrl(it) } ?: "${url.trimEnd('/')}/izle"
        return newMovieLoadResponse(title, url, TvType.Movie, watch) { posterUrl = poster; this.plot = plot; this.year = year }
    }
    override suspend fun loadLinks(data: String, isCasting: Boolean, subtitleCallback: (SubtitleFile) -> Unit, callback: (ExtractorLink) -> Unit): Boolean =
        resolvePersonalPlayer(data, "$mainUrl/", subtitleCallback, callback)
}
