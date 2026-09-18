package local.personal
import org.junit.Assert.*
import org.junit.Test

class OriginalAudioTest {
    private val master = """#EXTM3U
#EXT-X-MEDIA:TYPE=AUDIO,GROUP-ID="aud",NAME="audio_1",DEFAULT=YES,LANGUAGE="tr",URI="dub.m3u8"
#EXT-X-MEDIA:TYPE=AUDIO,GROUP-ID="aud",NAME="audio_2",DEFAULT=NO,URI="original.m3u8"
#EXT-X-STREAM-INF:BANDWIDTH=1000000,AUDIO="aud"
video.m3u8
"""
    @Test fun preservesOriginalAudioAndRelativeVideoUris() {
        val result = preferOriginalAudio(master, """[{"lang":"tr","label":"Türkçe Dublaj"},{"lang":"en","label":"Orijinal 2"}]""")
        assertFalse(result.contains("dub.m3u8"))
        assertTrue(result.contains("original.m3u8"))
        assertTrue(result.contains("DEFAULT=YES"))
        assertTrue(result.contains("LANGUAGE=\"en\""))
        assertTrue(result.contains("video.m3u8"))
    }
    @Test fun leavesSingleNativeAudioAndUnknownMappingsUntouched() {
        assertEquals(master, preferOriginalAudio(master, "[]"))
        assertEquals(master, preferOriginalAudio(master, """[{"lang":"tr","label":"Türkçe"}]"""))
        assertEquals(master, preferOriginalAudio(master, "bad json"))
    }
    @Test fun doesNotDropAudioGroupsUsedByOtherVariants() {
        val multi = master.replace("GROUP-ID=\"aud\",NAME=\"audio_2\"", "GROUP-ID=\"other\",NAME=\"audio_2\"")
        assertEquals(multi, preferOriginalAudio(multi, """[{"label":"Dublaj"},{"label":"Orijinal"}]"""))
    }
}
