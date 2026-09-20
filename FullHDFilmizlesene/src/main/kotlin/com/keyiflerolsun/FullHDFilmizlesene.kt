// ! Bu araç @keyiflerolsun tarafından | @KekikAkademi için yazılmıştır.

package com.keyiflerolsun

import local.personal.evfullhdfilmizlesene.BuildConfig

import android.util.Log
import android.util.Base64
import org.jsoup.nodes.Element
import org.jsoup.nodes.Document
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.LoadResponse.Companion.addActors
import com.lagradost.cloudstream3.LoadResponse.Companion.addTrailer

class FullHDFilmizlesene : MainAPI() {
    override var mainUrl = BuildConfig.SITE_URL
    override var name = "FullHDFilmizlesene (Kişisel)"
    override val hasMainPage          = true
    override var lang                 = "tr"
    override val hasQuickSearch       = false
    override val supportedTypes       = setOf(TvType.Movie)

    override val mainPage = mainPageOf(
        "${mainUrl}/en-cok-izlenen-filmler-izle-hd/"            to "En Çok izlenen Filmler",
        "${mainUrl}/filmizle/imdb-puani-yuksek-filmler-izle-1/" to "IMDB Puanı Yüksek Filmler",
        "${mainUrl}/filmizle/aile-filmleri-hdf-izle/"           to "Aile Filmleri",
        "${mainUrl}/filmizle/aksiyon-filmleri-hdf-izle/"         to "Aksiyon Filmleri",
        "${mainUrl}/filmizle/animasyon-filmleri-fhd-izle/"      to "Animasyon Filmleri",
        "${mainUrl}/filmizle/belgesel-filmleri-izle/"           to "Belgeseller",
        "${mainUrl}/filmizle/bilim-kurgu-filmleri-izle-2/"      to "Bilim Kurgu Filmleri",
        "${mainUrl}/filmizle/bluray-filmler-izle/"              to "Blu Ray Filmler",
        "${mainUrl}/filmizle/cizgi-filmler-fhd-izle/"           to "Çizgi Filmler",
        "${mainUrl}/filmizle/dram-filmleri-hd-izle/"            to "Dram Filmleri",
        "${mainUrl}/filmizle/fantastik-filmler-hd-izle/"        to "Fantastik Filmler",
        "${mainUrl}/filmizle/gerilim-filmleri-fhd-izle/"        to "Gerilim Filmleri",
        "${mainUrl}/filmizle/gizem-filmleri-hd-izle/"           to "Gizem Filmleri",
        "${mainUrl}/filmizle/hint-filmleri-fhd-izle/"            to "Hint Filmleri",
        "${mainUrl}/filmizle/komedi-filmleri-fhd-izle/"         to "Komedi Filmleri",
        "${mainUrl}/filmizle/korku-filmleri-izle-3/"            to "Korku Filmleri",
        "${mainUrl}/filmizle/macera-filmleri-fhd-izle/"         to "Macera Filmleri",
        "${mainUrl}/filmizle/muzikal-filmler-izle/"             to "Müzikal Filmler",
        "${mainUrl}/filmizle/polisiye-filmleri-izle/"           to "Polisiye Filmleri",
        "${mainUrl}/filmizle/psikolojik-filmler-izle/"          to "Psikolojik Filmler",
        "${mainUrl}/filmizle/romantik-filmler-fhd-izle/"        to "Romantik Filmler",
        "${mainUrl}/filmizle/savas-filmleri-fhd-izle/"          to "Savaş Filmleri",
        "${mainUrl}/filmizle/suc-filmleri-izle/"                to "Suç Filmleri",
        "${mainUrl}/filmizle/tarih-filmleri-fhd-izle/"          to "Tarih Filmleri",
        "${mainUrl}/filmizle/western-filmler-hd-izle-3/"        to "Western Filmler",
        "${mainUrl}/filmizle/yerli-filmler-hd-izle/"            to "Yerli Filmler",
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val document = app.get("${request.data}${page}").document
        val home     = document.select("li.film").mapNotNull { it.toSearchResult() }

        return newHomePageResponse(request.name, home)
    }

    private fun Element.toSearchResult(): SearchResponse? {
        val title     = this.selectFirst("span.film-title")?.text() ?: return null
        val href      = fixUrlNull(this.selectFirst("a")?.attr("href")) ?: return null
        val posterUrl = fixUrlNull(this.selectFirst("img")?.attr("data-src"))

        return newMovieSearchResponse(title, href, TvType.Movie) { this.posterUrl = posterUrl }
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val document = app.get("${mainUrl}/arama/${query}").document

        return document.select("li.film").mapNotNull { it.toSearchResult() }
    }

    override suspend fun quickSearch(query: String): List<SearchResponse> = search(query)

    override suspend fun load(url: String): LoadResponse? {
        val document = app.get(url).document

        val title           = document.selectFirst("div[class=izle-titles]")?.text()?.trim() ?: return null
        val poster          = fixUrlNull(document.selectFirst("div img")?.attr("data-src"))
        val year            = document.selectFirst("div.dd a.category")?.text()?.split(" ")?.get(0)?.trim()?.toIntOrNull()
        val description     = document.selectFirst("div.ozet-ic > p")?.text()?.trim()
        val tags            = document.select("a[rel='category tag']").map { it.text() }
        val duration        = document.selectFirst("span.sure")?.text()?.split(" ")?.get(0)?.trim()?.toIntOrNull()
        val trailer         = Regex("""embedUrl": "(.*)"""").find(document.html())?.groupValues?.get(1)
        val actors          = document.select("div.film-info ul li:nth-child(2) a > span").map {
            Actor(it.text())
        }


        val recommendations = document.selectXpath("//div[span[text()='Benzer Filmler']]/following-sibling::section/ul/li").mapNotNull {
            val recName      = it.selectFirst("span.film-title")?.text() ?: return@mapNotNull null
            val recHref      = fixUrlNull(it.selectFirst("a")?.attr("href")) ?: return@mapNotNull null
            val recPosterUrl = fixUrlNull(it.selectFirst("img")?.attr("data-src"))
            newMovieSearchResponse(recName, recHref, TvType.Movie) {
                this.posterUrl = recPosterUrl
            }
        }

        return newMovieLoadResponse(title, url, TvType.Movie, url) {
            this.posterUrl       = poster
            this.year            = year
            this.plot            = description
            this.tags            = tags
            this.duration        = duration
            this.recommendations = recommendations
            addActors(actors)
            addTrailer(trailer)
        }
    }

    override suspend fun loadLinks(data: String, isCasting: Boolean, subtitleCallback: (SubtitleFile) -> Unit, callback: (ExtractorLink) -> Unit): Boolean {
        val document = app.get(data, interceptor = com.lagradost.cloudstream3.network.CloudflareKiller()).document
        val links = extractScxLinks(document.html()).toMutableList()
        document.select("iframe[src], iframe[data-src]").forEach {
            val src = it.attr("src").ifBlank { it.attr("data-src") }
            if (src.isNotBlank()) links.add("Video" to src)
        }
        var found = false
        for ((label, value) in links.distinct()) {
            val videoUrl = fixUrlNull(value) ?: continue
            try {
                val onLink: (ExtractorLink) -> Unit = { found = true; callback(it) }
                val host = runCatching { java.net.URI(videoUrl).host.orEmpty() }.getOrDefault("")
                if (host == "turbo.imgz.me") {
                    // Pass the real URL directly to our extractor; a label||URL is not a valid URL for the registry.
                    TurboImgz().getUrl("$label||$videoUrl", data, subtitleCallback, onLink)
                } else if (host.contains("rapidvid") || host.contains("imgz.me")) {
                    RapidVid().getUrl(videoUrl, data, subtitleCallback, onLink)
                } else if (host.contains("vidmoxy")) {
                    VidMoxy().getUrl(videoUrl, data, subtitleCallback, onLink)
                } else {
                    loadExtractor(videoUrl, data, subtitleCallback, onLink)
                }
            } catch (e: kotlinx.coroutines.CancellationException) { throw e }
            catch (_: Exception) { /* A dead mirror must not suppress the remaining choices. */ }
        }
        return found
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class SCXData(
        @JsonProperty("atom")      val atom: AtomData?      = null,
        @JsonProperty("advid")     val advid: AtomData?     = null,
        @JsonProperty("advidprox") val advidprox: AtomData? = null,
        @JsonProperty("proton")    val proton: AtomData?    = null,
        @JsonProperty("fast")      val fast: AtomData?      = null,
        @JsonProperty("fastly")    val fastly: AtomData?    = null,
        @JsonProperty("tr")        val tr: AtomData?        = null,
        @JsonProperty("en")        val en: AtomData?        = null,
    )

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class AtomData(
        @JsonProperty("sx") var sx: SXData
    )

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class SXData(
        @JsonProperty("t") var t: Any
    )
}
