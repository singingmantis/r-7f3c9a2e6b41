package com.nikyokki

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import local.personal.evdizigom.BuildConfig
import local.personal.resolvePersonalPlayer
import org.jsoup.nodes.Document
import java.net.URLEncoder

class DiziGom : MainAPI() {
    override var mainUrl = BuildConfig.SITE_URL
    override var name = "DiziGom (Kişisel)"
    override var lang = "tr"
    override val hasMainPage = true
    override val supportedTypes = setOf(TvType.TvSeries)
    override val mainPage = mainPageOf("$mainUrl/" to "Diziler")
    private fun Document.results(): List<SearchResponse> = select("a[href*=/diziler/]").mapNotNull { a ->
        val href = fixUrl(a.attr("href"))
        val title = a.attr("title").ifBlank { a.text().trim() }.ifBlank { a.selectFirst("img")?.attr("title").orEmpty() }
        if (title.isBlank()) return@mapNotNull null
        val image = a.selectFirst("img") ?: a.parent()?.selectFirst("img")
        newTvSeriesSearchResponse(title, href, TvType.TvSeries) { posterUrl = fixUrlNull(image?.attr("src")) }
    }.distinctBy { it.url }
    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val doc = app.get(mainUrl).document
        return newHomePageResponse(listOf(HomePageList(request.name, doc.results())), false)
    }
    override suspend fun search(query: String): List<SearchResponse> {
        val results = app.get("$mainUrl/?s=${URLEncoder.encode(query, "UTF-8")}").document.results()
        // This theme includes a series directory in its sidebar even on search pages.
        return results.filter { it.name.contains(query, ignoreCase = true) }
    }
    override suspend fun load(url: String): LoadResponse? {
        val doc = app.get(url).document
        val title = doc.selectFirst("#content h1")?.text()?.substringBefore(" izle") ?: return null
        val poster = fixUrlNull(doc.selectFirst(".category_image img")?.attr("src"))
        val plot = doc.selectFirst(".category_desc")?.text()
        val episodes = doc.select("div.bolumust a[href]").mapNotNull { a ->
            val href = fixUrl(a.attr("href"))
            val match = Regex("-(\\d+)-sezon-(\\d+)-bolum").find(href) ?: return@mapNotNull null
            newEpisode(href) { season = match.groupValues[1].toInt(); episode = match.groupValues[2].toInt(); name = a.text().trim() }
        }.distinctBy { it.data }.sortedWith(compareBy({ it.season }, { it.episode }))
        return newTvSeriesLoadResponse(title, url, TvType.TvSeries, episodes) { posterUrl = poster; this.plot = plot }
    }
    override suspend fun loadLinks(data: String, isCasting: Boolean, subtitleCallback: (SubtitleFile) -> Unit, callback: (ExtractorLink) -> Unit): Boolean =
        resolvePersonalPlayer(data, "$mainUrl/", subtitleCallback, callback, sourceName = name)
}
