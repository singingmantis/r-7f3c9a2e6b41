package com.keyiflerolsun
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.junit.Assert.*
import org.junit.Test

class OkMetadataTest {
    @Test fun readsNestedEscapedJsonFromHtmlAttribute() {
        val mapper = jacksonObjectMapper()
        val metadata = """{"videos":[{"name":"sd","url":"https://cdn.example/video?x=1&y=2"}]}"""
        val options = mapper.writeValueAsString(mapOf("flashvars" to mapOf("metadata" to metadata)))
        val escaped = options.replace("&", "&amp;").replace("\"", "&quot;")
        val result = readOkMetadata("<div data-options=\"{}\"></div><div data-options=\"$escaped\"></div>")
        assertEquals("https://cdn.example/video?x=1&y=2", result!!.path("videos")[0].path("url").asText())
    }
    @Test fun handlesMissingPlayerWithoutInventingLinks() { assertNull(readOkMetadata("<html>Unavailable</html>")) }
}
