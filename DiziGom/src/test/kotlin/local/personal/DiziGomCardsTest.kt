package local.personal

import org.jsoup.Jsoup
import org.junit.Assert.*
import org.junit.Test

class DiziGomCardsTest {
    @Test fun menuCannotShadowActualPosterCard() {
        val doc = Jsoup.parse("""
            <nav><a href="/diziler/example/">Menu title</a></nav>
            <div id="content"><div class="single-item">
            <div class="cat-img"><a href="/diziler/example/"><img src="/poster.jpg"></a></div>
            <div class="categorytitle"><a href="/diziler/example/">Example</a></div>
            </div></div>
        """)
        val cards = diziGomCards(doc, "https://example.org")
        assertEquals(1, cards.size)
        assertEquals("Example", cards.single().title)
        assertEquals("https://example.org/poster.jpg", cards.single().poster)
    }
    @Test fun emptySearchDoesNotReturnSidebarSeries() {
        val doc = Jsoup.parse("""<nav><a href="/diziler/unrelated/">Unrelated</a></nav><div id="content">No results</div>""")
        assertTrue(diziGomCards(doc, "https://example.org").isEmpty())
    }
}
