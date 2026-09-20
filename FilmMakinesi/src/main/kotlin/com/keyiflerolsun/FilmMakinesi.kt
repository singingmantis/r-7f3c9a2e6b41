package com.keyiflerolsun

import local.personal.evfilmmakinesi.BuildConfig

import android.util.Log
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.Actor
import com.lagradost.cloudstream3.LoadResponse.Companion.addActors
import com.lagradost.cloudstream3.LoadResponse.Companion.addTrailer

import org.jsoup.nodes.Element

class FilmMakinesi : MainAPI() {
    override var mainUrl = BuildConfig.SITE_URL
    override var name = "FilmMakinesi (Kişisel)"
    override val hasMainPage = true
    override var lang = "tr"
    override val hasQuickSearch = false
    override val supportedTypes = setOf(TvType.Movie, TvType.TvSeries)
    override val mainPage = mainPageOf(
        "${mainUrl}/filmler-1/" to "Son Filmler",
        "${mainUrl}/yabanci-dizi-izle-1/" to "Son Diziler",
        "${mainUrl}/tur/aksiyon-fm1/film/" to "Aksiyon",
        "${mainUrl}/tur/korku-fm2/film/" to "Korku",
        "${mainUrl}/tur/bilim-kurgu-fm3/film/" to "Bilim Kurgu",
        "${mainUrl}/tur/komedi-fm1/film/" to "Komedi",
        "${mainUrl}/tur/gerilim-fm1/film/" to "Gerilim",
        "${mainUrl}/tur/macera-fm1/film/" to "Macera",
        "${mainUrl}/tur/fantastik-fm1/film/" to "Fantastik"
    )
    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val url = if (page == 1) request.data else "${request.data.removeSuffix("/")}/sayfa/$page/"
        val document = app.get(url).document
        val items = document.select("a.item")
        val list = items.mapNotNull { it.toSearchResult() }
        val hasNext = document.select("ul.pagination li a[href]").isNotEmpty()
        return newHomePageResponse(request.name, list, hasNext = hasNext)
    }
    override suspend fun search(query: String): List<SearchResponse> {
        val document = app.get("${mainUrl}/arama/?s=${query}").document
        return document.select("a.item").mapNotNull { it.toSearchResult() }
    }
    private fun Element.toSearchResult(): SearchResponse? {
        val href = fixUrlNull(this.attr("href")) ?: return null
        val title = this.attr("data-title")
            .ifBlank { this.selectFirst(".item-footer .title")?.text()?.trim() }
            ?: return null
        val poster = fixUrlNull(this.selectFirst(".thumbnail-outer img")?.attr("src"))
        val year = this.selectFirst(".item-footer .info span")?.text()?.toIntOrNull()
        val score = this.attr("data-score").toFloatOrNull()
        val type = if (href.contains("/dizi/")) TvType.TvSeries else TvType.Movie

        return if (type == TvType.TvSeries) {
            newTvSeriesSearchResponse(title, href, TvType.TvSeries) {
                this.posterUrl = poster
                this.year = year
                this.score = score?.let { Score.from10(it) }
            }
        } else {
            newMovieSearchResponse(title, href, TvType.Movie) {
                this.posterUrl = poster
                this.year = year
                this.score = score?.let { Score.from10(it) }
            }
        }
    }
    override suspend fun load(url: String): LoadResponse? {
        val document = app.get(url).document
        val title = document.selectFirst("h1.title")?.ownText()?.trim()
            ?.removeSuffix(" izle")
            ?: return null
        val poster = fixUrlNull(document.selectFirst("#info--box .cover img")?.attr("src"))
        val year = document.selectFirst("h1.title .date a")?.text()?.toIntOrNull()
        val imdbText = document.selectFirst(".imdb b")?.text()
        val rating = imdbText?.toFloatOrNull()
        val durationText = document.selectFirst(".time")?.text()
        val duration = durationText?.replace(Regex("[^0-9]"), "")?.toIntOrNull()
        val tags = document.select("#info--box .content .type a").map { it.text().trim() }
        val description = document.selectFirst(".info-description")?.text()?.trim()
        val director = document.selectFirst(".director a")?.text()?.trim()
        val actors = document.select("#cast .cast").mapNotNull { cast ->
            val actorName = cast.selectFirst(".cast-name")?.text()?.trim()
            val actorImg = fixUrlNull(cast.selectFirst("img")?.attr("src"))
            actorName?.let { Actor(it, actorImg) }
        }
        val trailer = document.selectFirst(".trailer-button")?.attr("data-video_url") ?: ""
        val recommendations = document.select(".related a.item").mapNotNull { it.toSearchResult() }

        val isSeries = url.contains("/dizi/")

        return if (isSeries) {
            val episodes = mutableListOf<Episode>()
            document.select("a.item-ep").forEach { ep ->
                val epHref = fixUrlNull(ep.attr("href")) ?: return@forEach
                val epName = ep.selectFirst(".ep-details")?.text()?.trim() ?: ""
                val epTitleText = ep.selectFirst(".ep-title")?.text()?.trim() ?: ""
                val seasonMatch = Regex("""(\d+)\.\s*Sezon""").find(epTitleText)
                val epMatch = Regex("""(\d+)\.\s*Bölüm""").find(epTitleText)
                val seasonNum = seasonMatch?.groupValues?.get(1)?.toIntOrNull()
                    ?: Regex("""/sezon-(\d+)/""").find(epHref)?.groupValues?.get(1)?.toIntOrNull()
                    ?: 1

                val epNum = epMatch?.groupValues?.get(1)?.toIntOrNull()
                    ?: Regex("""/bolum-(\d+)/""").find(epHref)?.groupValues?.get(1)?.toIntOrNull()
                    ?: return@forEach

                episodes.add(newEpisode(epHref) {
                    this.name = epName.ifBlank { "$seasonNum. Sezon $epNum. Bölüm" }
                    this.season = seasonNum
                    this.episode = epNum
                    this.description = epTitleText
                    this.posterUrl = poster
                })
            }

            newTvSeriesLoadResponse(title, url, TvType.TvSeries, episodes) {
                this.posterUrl = poster
                this.year = year
                this.plot = description
                this.tags = tags
                this.score = rating?.let { Score.from10(it) }
                this.duration = duration
                this.recommendations = recommendations
                addActors(actors)
                if (trailer.isNotBlank()) addTrailer(trailer)
            }
        } else {
            newMovieLoadResponse(title, url, TvType.Movie, url) {
                this.posterUrl = poster
                this.year = year
                this.plot = description
                this.tags = tags
                this.score = rating?.let { Score.from10(it) }
                this.duration = duration
                this.recommendations = recommendations
                addActors(actors)
                if (trailer.isNotBlank()) addTrailer(trailer)
            }
        }
    }
    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        Log.d(name, "loadLinks çağrıldı, data: $data")

        val document = app.get(data, referer = mainUrl).document
        Log.d(name, "Sayfa yüklendi")

        val targets = mutableListOf<String>()
        document.select("[data-video_url], [data-video-url]").forEach {
            targets += it.attr("data-video_url").ifBlank { it.attr("data-video-url") }
        }
        document.select("iframe[src], iframe[data-src]").forEach {
            targets += it.attr("src").ifBlank { it.attr("data-src") }
        }
        // Player attributes are occasionally injected into malformed HTML that Jsoup normalizes away.
        Regex("""(?:data-video_url|data-video-url|data-src|src)\s*=\s*["']([^"']+)["']""", RegexOption.IGNORE_CASE)
            .findAll(document.html())
            .map { it.groupValues[1].replace("&amp;", "&") }
            .filter { it.startsWith("http://") || it.startsWith("https://") || it.startsWith("//") }
            .forEach { targets += it }

        var found = false
        for (raw in targets.filter { it.isNotBlank() }.distinct()) {
            val embedUrl = fixUrlNull(raw) ?: continue
            val host = runCatching { java.net.URI(embedUrl).host.orEmpty() }.getOrDefault("")
            if (host.isBlank() || host == java.net.URI(mainUrl).host ||
                host.contains("youtube") || host.contains("youtu.be")) continue
            val onLink: (ExtractorLink) -> Unit = { found = true; callback(it) }
            try {
                when {
                    host.contains("rapidvid") || host.contains("imgz.me") ->
                        CurrentRapidExtractor().getUrl(embedUrl, data, subtitleCallback, onLink)
                    host.contains("closeload") ->
                        CloseLoadExtractor().getUrl(embedUrl, data, subtitleCallback, onLink)
                    host.startsWith("rapid.") ->
                        RapidExtractor().getUrl(embedUrl, data, subtitleCallback, onLink)
                    else -> loadExtractor(embedUrl, data, subtitleCallback, onLink)
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(name, "Oynatıcı çözülemedi: $embedUrl", e)
            }
        }
        return found
    }
}
