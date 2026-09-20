package local.personal

import java.net.URI
import org.jsoup.nodes.Document

internal data class DiziGomCard(val title: String, val url: String, val poster: String?)

internal fun diziGomCards(document: Document, baseUrl: String): List<DiziGomCard> {
    fun resolve(raw: String?): String? = raw?.trim()?.takeIf { it.isNotEmpty() }?.let {
        runCatching { URI(baseUrl).resolve(it).toString() }.getOrNull()
    }
    // Scope to actual catalogue cards; navigation contains the same series URLs without posters.
    return document.select("#content .single-item, #content .episode-box").mapNotNull { card ->
        val link = card.selectFirst(".categorytitle a[href], .serie-name a[href]")
            ?: card.selectFirst("a[href*=/diziler/]") ?: return@mapNotNull null
        val image = card.selectFirst("img")
        val title = link.text().trim().ifBlank { link.attr("title") }
            .ifBlank { image?.attr("title").orEmpty() }
        val url = resolve(link.attr("href")) ?: return@mapNotNull null
        if (title.isBlank()) return@mapNotNull null
        val poster = image?.let { it.attr("data-src").ifBlank { it.attr("src") } }
        DiziGomCard(title, url, resolve(poster))
    }.distinctBy { it.url }
}
