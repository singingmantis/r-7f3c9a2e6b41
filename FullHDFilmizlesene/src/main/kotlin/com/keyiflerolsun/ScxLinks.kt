package com.keyiflerolsun

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.jsoup.Jsoup

/** Read the assigned object rather than assuming the first script is the player. */
internal fun extractScxLinks(html: String): List<Pair<String, String>> {
    val mapper = jacksonObjectMapper()
    val result = mutableListOf<Pair<String, String>>()
    fun collect(label: String, node: JsonNode) {
        when {
            node.isTextual -> {
                val raw = node.asText()
                val decoded = if (raw.startsWith("http") || raw.startsWith("//")) raw else runCatching {
                    val rot = raw.map { c -> when(c) {
                        in 'a'..'z' -> ('a'.code + (c - 'a' + 13) % 26).toChar()
                        in 'A'..'Z' -> ('A'.code + (c - 'A' + 13) % 26).toChar()
                        else -> c
                    }}.joinToString("")
                    String(org.bouncycastle.util.encoders.Base64.decode(rot), Charsets.UTF_8)
                }.getOrNull()
                if (decoded != null && (decoded.startsWith("http") || decoded.startsWith("//"))) result.add(label to decoded)
            }
            node.isArray -> node.forEach { collect(label, it) }
            node.isObject -> node.fields().forEachRemaining { collect(it.key, it.value) }
        }
    }
    for (script in Jsoup.parse(html).select("script")) {
        val source = script.data()
        val assignment = Regex("""\bscx\s*=\s*""").find(source) ?: continue
        val value = source.substring(assignment.range.last + 1).trimStart()
        // Jackson consumes one JSON value; it tolerates the trailing semicolon/next JS statement.
        val root = runCatching { mapper.readTree(value) }.getOrNull() ?: continue
        root.fields().forEachRemaining { entry -> collect(entry.key, entry.value.path("sx").path("t")) }
    }
    return result.distinct()
}
