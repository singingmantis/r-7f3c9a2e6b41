package local.personal
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.junit.Assert.*
import org.junit.Test

class PlayerSubtitlesTest {
    @Test fun acceptsObservedSrcFieldAndPrefersFullSubtitles() {
        val config = jacksonObjectMapper().readTree("""{"subs":[{"label":"Türkçe (Forced)","src":"/forced.vtt"},{"label":"Tam (Türkçe)","lang":"tr","src":"/full.vtt"},{"label":"Broken","src":"http://[bad"}]}""")
        val subs = playerSubtitles(config, "https://player.example/watch")
        assertEquals(listOf("Türkçe" to "https://player.example/full.vtt", "Türkçe (Forced)" to "https://player.example/forced.vtt"), subs)
    }
    @Test fun stillSupportsLegacyFileField() {
        val config = jacksonObjectMapper().readTree("""{"subs":[{"label":"English","file":"https://cdn.example/en.vtt"}]}""")
        assertEquals("https://cdn.example/en.vtt", playerSubtitles(config, "https://player.example")[0].second)
    }
}
