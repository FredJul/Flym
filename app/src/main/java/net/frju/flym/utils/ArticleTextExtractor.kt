package net.frju.flym.utils

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.io.InputStream
import java.util.*
import java.util.regex.Pattern

/**
 * This class is thread safe.

 * @author Alex P (ifesdjeen from jreadability)
 * *
 * @author Peter Karich
 */
object ArticleTextExtractor {

    // Interesting nodes
    private val NODES = Pattern.compile("p|div|td|h1|h2|article|section")

    // Unlikely candidates
    private val UNLIKELY = Pattern.compile("com(bx|ment|munity)|dis(qus|cuss)|e(xtra|[-]?mail)|foot|"
            + "header|menu|re(mark|ply)|rss|sh(are|outbox)|social|twitter|facebook|sponsor"
            + "a(d|ll|gegate|rchive|ttachment)|(pag(er|ination))|popup|print|"
            + "login|si(debar|gn|ngle)|hinweis|expla(in|nation)?|metablock")

    // Most likely positive candidates
    private val POSITIVE = Pattern.compile("(^(body|content|h?entry|main|page|post|text|blog|story|haupt))" + "|arti(cle|kel)|instapaper_body")

    // Very most likely positive candidates, used by Joomla CMS
    private val ITSJOOMLA = Pattern.compile("articleBody")

    // Most likely negative candidates
    private val NEGATIVE = Pattern.compile("nav($|igation)|user|com(ment|bx)|(^com-)|contact|"
            + "foot|masthead|(me(dia|ta))|outbrain|promo|related|scroll|(sho(utbox|pping))|"
            + "sidebar|sponsor|tags|tool|widget|player|disclaimer|toc|infobox|vcard")

    private val NEGATIVE_STYLE = Pattern.compile("hidden|display: ?none|font-size: ?small")

    /**
     * @param input            extracts article text from given html string. wasn't tested
     * *                         with improper HTML, although jSoup should be able to handle minor stuff.
     * *
     * @param contentIndicator a text which should be included into the extracted content, or null
     * *
     * @return extracted article, all HTML tags stripped
     */
    @Throws(Exception::class)
    fun extractContent(input: InputStream, contentIndicator: String?): String? {
        return extractContent(Jsoup.parse(input, null, ""), contentIndicator)
    }

    fun extractContent(doc: Document?, contentIndicator: String?): String? {
        if (doc == null)
            throw NullPointerException("missing document")

        // now remove the clutter
        prepareDocument(doc)

        // init elements
        val nodes = getNodes(doc)
        var maxWeight = 0
        var bestMatchElement: Element? = null

        for (entry in nodes) {
            val currentWeight = getWeight(entry, contentIndicator)
            if (currentWeight > maxWeight) {
                maxWeight = currentWeight
                bestMatchElement = entry

                if (maxWeight > 300) {
                    break
                }
            }
        }

        val metas = getMetas(doc)
        val ogImage: String? = metas
                .firstOrNull { it.hasAttr("property") && "og:image" == it.attr("property") }
                ?.attr("content")

        if (bestMatchElement != null) {
            var ret = bestMatchElement.toString()
            if (ogImage != null && !ret.contains(ogImage)) {
                ret = "<img src=\"$ogImage\"><br>\n$ret"
            }
            return ret
        }

        return null
    }

    /**
     * Weights current element. By matching it with positive candidates and
     * weighting child nodes. Since it's impossible to predict which exactly
     * names, ids or class names will be used in HTML, major role is played by
     * child nodes

     * @param e                Element to weight, along with child nodes
     * *
     * @param contentIndicator a text which should be included into the extracted content, or null
     */
    private fun getWeight(e: Element, contentIndicator: String?): Int {
        var weight = calcWeight(e)
        weight += Math.round(e.ownText().length / 100.0 * 10).toInt()
        weight += weightChildNodes(e, contentIndicator)
        return weight
    }

    /**
     * Weights a child nodes of given Element. During tests some difficulties
     * were met. For instance, not every single document has nested paragraph
     * tags inside of the major article tag. Sometimes people are adding one
     * more nesting level. So, we're adding 4 points for every 100 symbols
     * contained in tag nested inside of the current weighted element, but only
     * 3 points for every element that's nested 2 levels deep. This way we give
     * more chances to extract the element that has less nested levels,
     * increasing probability of the correct extraction.

     * @param rootEl           Element, who's child nodes will be weighted
     * *
     * @param contentIndicator a text which should be included into the extracted content, or null
     */
    private fun weightChildNodes(rootEl: Element, contentIndicator: String?): Int {
        var weight = 0
        var caption: Element? = null
        val pEls = ArrayList<Element>(5)
        for (child in rootEl.children()) {
            val text = child.text()
            val textLength = text.length
            if (textLength < 20) {
                continue
            }

            if (contentIndicator != null && text.contains(contentIndicator)) {
                weight += 100 // We certainly found the item
            }

            val ownText = child.ownText()
            val ownTextLength = ownText.length
            if (ownTextLength > 200) {
                weight += Math.max(50, ownTextLength / 10)
            }

            if (child.tagName() == "h1" || child.tagName() == "h2") {
                weight += 30
            } else if (child.tagName() == "div" || child.tagName() == "p") {
                weight += calcWeightForChild(ownText)
                if (child.tagName() == "p" && textLength > 50)
                    pEls.add(child)

                if (child.className().toLowerCase() == "caption")
                    caption = child
            }
        }

        // use caption and image
        if (caption != null)
            weight += 30

        if (pEls.size >= 2) {
            rootEl.children()
                    .filter { "h1;h2;h3;h4;h5;h6".contains(it.tagName()) }
                    .forEach {
                        weight += 20
                        // headerEls.add(subEl);
                    }
        }
        return weight
    }

    private fun calcWeightForChild(text: String): Int {
        return text.length / 25
        //		return Math.min(100, text.length() / ((child.getAllElements().size()+1)*5));
    }

    private fun calcWeight(e: Element): Int {
        var weight = 0
        if (POSITIVE.matcher(e.className()).find())
            weight += 35

        if (POSITIVE.matcher(e.id()).find())
            weight += 40

        if (ITSJOOMLA.matcher(e.attributes().toString()).find())
            weight += 200

        if (UNLIKELY.matcher(e.className()).find())
            weight -= 20

        if (UNLIKELY.matcher(e.id()).find())
            weight -= 20

        if (NEGATIVE.matcher(e.className()).find())
            weight -= 50

        if (NEGATIVE.matcher(e.id()).find())
            weight -= 50

        val style = e.attr("style")
        if (style != null && !style.isEmpty() && NEGATIVE_STYLE.matcher(style).find())
            weight -= 50
        return weight
    }

    /**
     * Prepares document. Currently only stipping unlikely candidates, since
     * from time to time they're getting more score than good ones especially in
     * cases when major text is short.

     * @param doc document to prepare. Passed as reference, and changed inside
     * *            of function
     */
    private fun prepareDocument(doc: Document) {
        // stripUnlikelyCandidates(doc);
        removeSelectsAndOptions(doc)
        removeScriptsAndStyles(doc)
    }

    /**
     * Removes unlikely candidates from HTML. Currently takes id and class name
     * and matches them against list of patterns

     * @param doc document to strip unlikely candidates from
     */
    //    protected void stripUnlikelyCandidates(Document doc) {
    //        for (Element child : doc.select("body").select("*")) {
    //            String className = child.className().toLowerCase();
    //            String id = child.id().toLowerCase();
    //
    //            if (NEGATIVE.matcher(className).find()
    //                    || NEGATIVE.matcher(id).find()) {
    //                child.remove();
    //            }
    //        }
    //    }
    private fun removeScriptsAndStyles(doc: Document): Document {
        val scripts = doc.getElementsByTag("script")
        for (item in scripts) {
            item.remove()
        }

        val noscripts = doc.getElementsByTag("noscript")
        for (item in noscripts) {
            item.remove()
        }

        val styles = doc.getElementsByTag("style")
        for (style in styles) {
            style.remove()
        }

        return doc
    }

    private fun removeSelectsAndOptions(doc: Document): Document {
        val scripts = doc.getElementsByTag("select")
        for (item in scripts) {
            item.remove()
        }

        val noscripts = doc.getElementsByTag("option")
        for (item in noscripts) {
            item.remove()
        }

        return doc
    }

    /**
     * @return a set of all meta nodes
     */
    private fun getMetas(doc: Document): Collection<Element> {
        val nodes = HashSet<Element>(64)
        nodes += doc.select("head").select("meta")
        return nodes
    }

    /**
     * @return a set of all important nodes
     */
    private fun getNodes(doc: Document): Collection<Element> {
        return doc.select("body").select("*").filterTo(HashSet<Element>(64)) { NODES.matcher(it.tagName()).matches() }
    }
}
