package com.nikyokki

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import local.personal.evdizigom.BuildConfig
import local.personal.resolvePersonalPlayer
import local.personal.diziGomCards
import org.jsoup.nodes.Document
import java.net.URLEncoder

class DiziGom : MainAPI() {
    override var mainUrl = BuildConfig.SITE_URL
    override var name = "DiziGom (Kişisel)"
    override var lang = "tr"
    override val hasMainPage = true
    override val supportedTypes = setOf(TvType.TvSeries)
    override val mainPage = mainPageOf("$mainUrl/dizi-izle/" to "Diziler")
    private fun Document.results(): List<SearchResponse> = diziGomCards(this, mainUrl).map { card ->
        newTvSeriesSearchResponse(card.title, card.url, TvType.TvSeries) { posterUrl = card.poster }
    }
    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val url = if (page == 1) request.data else "${request.data.trimEnd('/')}/page/$page/"
        val doc = app.get(url).document
        return newHomePageResponse(listOf(HomePageList(request.name, doc.results())),
            doc.select("a.next.page-numbers, a[rel=next]").isNotEmpty())
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
