package local.personal
import com.fasterxml.jackson.databind.JsonNode
import java.net.URI

internal fun playerSubtitles(config: JsonNode, playerUrl: String): List<Pair<String, String>> =
    config.path("subs").toList().sortedBy {
        val label = it.path("label").asText("")
        if (label.contains("forced", true)) 1 else 0
    }.mapNotNull { sub ->
        val raw = listOf("src", "file", "url").firstNotNullOfOrNull {
            sub.path(it).asText("").takeIf(String::isNotBlank)
        } ?: return@mapNotNull null
        val url = runCatching { URI(playerUrl).resolve(raw).toString() }.getOrNull() ?: return@mapNotNull null
        if (!url.startsWith("https://") && !url.startsWith("http://")) return@mapNotNull null
        val label = sub.path("label").asText("Altyazı")
        val language = sub.path("lang").asText("")
        // Recognizable language names let CloudStream honor the preferred subtitle language.
        val display = if (language == "tr" && !label.contains("forced", true)) "Türkçe" else label
        display to url
    }.distinct()
