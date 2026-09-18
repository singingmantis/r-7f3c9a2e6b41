package local.personal

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import okhttp3.Interceptor
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody

/** Keep the real master URL so relative video/audio segments retain their base URL. */
class OriginalAudioInterceptor(private val masterUrl: String, private val audioJson: String) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val response = chain.proceed(chain.request())
        if (chain.request().url.toString() != masterUrl || !response.isSuccessful) return response
        val body = response.body ?: return response
        val text = response.peekBody(2L * 1024 * 1024).string()
        val rewritten = preferOriginalAudio(text, audioJson)
        if (rewritten == text) return response
        val type = body.contentType()
        body.close()
        return response.newBuilder().removeHeader("Content-Length").body(rewritten.toResponseBody(type)).build()
    }
}

internal fun preferOriginalAudio(master: String, audioJson: String): String {
    if (!master.startsWith("#EXTM3U") || !master.contains("#EXT-X-STREAM-INF:")) return master
    val tracks = runCatching { jacksonObjectMapper().readTree(audioJson).toList() }.getOrNull() ?: return master
    // The player maps its audio metadata to HLS renditions by position, not the generic audio_N name.
    val selected = tracks.indexOfFirst {
        val label = it.path("label").asText("")
        label.contains("orijinal", true) || label.contains("original", true)
    }.takeIf { it >= 0 } ?: return master
    val lines = master.lines()
    val audioLines = lines.filter { it.startsWith("#EXT-X-MEDIA:") && it.contains("TYPE=AUDIO") }
    if (audioLines.size != tracks.size) return master
    // Do not modify unusual multi-group playlists whose video variants may depend on different groups.
    val groups = audioLines.map { Regex("""GROUP-ID="([^"]+)"""").find(it)?.groupValues?.get(1) }.toSet()
    if (groups.size != 1 || groups.contains(null)) return master
    var index = -1
    return lines.mapNotNull { line ->
        if (!line.startsWith("#EXT-X-MEDIA:") || !line.contains("TYPE=AUDIO")) return@mapNotNull line
        index++
        if (index != selected) return@mapNotNull null
        var out = line.replace(Regex("DEFAULT=(YES|NO)"), "DEFAULT=YES")
        if (!out.contains("DEFAULT=")) out += ",DEFAULT=YES"
        val language = tracks[selected].path("lang").asText("").takeIf { it.matches(Regex("[A-Za-z]{2,3}")) }
        if (language != null) {
            out = out.replace(Regex(""",?LANGUAGE="[^"]*""""), "") + ",LANGUAGE=\"$language\""
        }
        out.replace(Regex("""NAME="[^"]*""""), "NAME=\"Orijinal\"")
    }.joinToString("\n")
}
