package com.example.decosocio.data

import com.example.decosocio.data.news.RssParser
import com.example.decosocio.data.news.SampleArticles
import kotlinx.datetime.LocalDate
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RssParserTest {
    private val today = LocalDate(2026, 9, 30)

    @Test
    fun readsItemsDatesAndStripsHtml() {
        val xml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <rss version="2.0"><channel><title>Feed</title>
              <item>
                <title>Primeiro artigo</title>
                <link>https://example.pt/a1</link>
                <guid>a1</guid>
                <category>Direitos</category>
                <pubDate>Mon, 28 Sep 2026 10:00:00 +0100</pubDate>
                <description><![CDATA[<p>Olá <b>mundo</b> &amp; amigos</p>]]></description>
              </item>
              <item>
                <title>Sem data</title>
                <description>Texto</description>
              </item>
            </channel></rss>
        """.trimIndent()
        val articles = RssParser.parse(xml, today)
        assertEquals(2, articles.size)
        assertEquals("a1", articles[0].id)
        assertEquals(LocalDate(2026, 9, 28), articles[0].publishedOn)
        assertEquals("Olá mundo & amigos", articles[0].summary)
        assertEquals("Direitos", articles[0].category)
        assertEquals(today, articles[1].publishedOn)
    }

    @Test
    fun sampleArticlesAreLabelledAsSamples() {
        val samples = SampleArticles.all(today)
        assertTrue(samples.isNotEmpty())
        assertTrue(samples.all { it.isSample })
        assertEquals(samples.size, samples.map { it.id }.toSet().size)
    }
}
