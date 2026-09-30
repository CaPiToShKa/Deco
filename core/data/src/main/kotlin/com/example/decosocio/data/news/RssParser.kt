package com.example.decosocio.data.news

import com.example.decosocio.domain.model.Article
import kotlinx.datetime.LocalDate
import org.w3c.dom.Element
import java.io.StringReader
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import javax.xml.parsers.DocumentBuilderFactory
import org.xml.sax.InputSource

/** Minimal RSS 2.0 reader (title, link, description, pubDate, category, guid). */
internal object RssParser {

    fun parse(xml: String, fallbackDate: LocalDate): List<Article> {
        val factory = DocumentBuilderFactory.newInstance().apply {
            isNamespaceAware = false
            isExpandEntityReferences = false
            // Block XXE where the platform parser supports it (Android's parser rejects unknown features).
            runCatching { setFeature("http://apache.org/xml/features/disallow-doctype-decl", true) }
        }
        val document = factory.newDocumentBuilder().parse(InputSource(StringReader(xml)))
        val items = document.getElementsByTagName("item")
        return (0 until items.length).mapNotNull { index ->
            val item = items.item(index) as? Element ?: return@mapNotNull null
            val title = item.text("title") ?: return@mapNotNull null
            val link = item.text("link")
            val description = stripHtml(item.text("description").orEmpty())
            Article(
                id = item.text("guid") ?: link ?: title.hashCode().toString(),
                title = stripHtml(title),
                summary = description.take(280),
                body = description,
                category = item.text("category").orEmpty(),
                publishedOn = item.text("pubDate")?.let(::parseDate) ?: fallbackDate,
                imageUrl = (item.getElementsByTagName("enclosure").item(0) as? Element)
                    ?.takeIf { it.getAttribute("type").startsWith("image") }
                    ?.getAttribute("url")
                    ?.ifBlank { null },
                url = link,
                membersOnly = false,
                isSample = false,
            )
        }
    }

    private fun Element.text(tag: String): String? =
        getElementsByTagName(tag).item(0)?.textContent?.trim()?.ifEmpty { null }

    private fun parseDate(value: String): LocalDate? = runCatching {
        val date = ZonedDateTime.parse(value.trim(), DateTimeFormatter.RFC_1123_DATE_TIME).toLocalDate()
        LocalDate(date.year, date.monthValue, date.dayOfMonth)
    }.getOrNull()

    private val tags = Regex("<[^>]+>")
    private val spaces = Regex("[ \\t\\x0B\\f\\r]+")

    fun stripHtml(html: String): String = html
        .replace(Regex("(?i)<br\\s*/?>|</p>"), "\n")
        .replace(tags, "")
        .replace("&nbsp;", " ")
        .replace("&quot;", "\"")
        .replace("&#39;", "'")
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&amp;", "&")
        .replace(spaces, " ")
        .lines()
        .joinToString("\n") { it.trim() }
        .replace(Regex("\n{3,}"), "\n\n")
        .trim()
}
