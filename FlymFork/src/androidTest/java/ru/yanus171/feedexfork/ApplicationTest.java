package ru.yanus171.feedexfork;

import android.app.Application;
import android.test.ApplicationTestCase;

import java.net.HttpURLConnection;
import java.util.Date;

import ru.yanus171.feedexfork.parser.RssAtomParser;
import ru.yanus171.feedexfork.utils.ArticleTextExtractor;
import ru.yanus171.feedexfork.utils.HtmlUtils;
import ru.yanus171.feedexfork.utils.NetworkUtils;

/**
 * <a href="http://d.android.com/tools/testing/testing_android.html">Testing Fundamentals</a>
 */
public class ApplicationTest extends ApplicationTestCase<Application> {

    public ApplicationTest() {
        super(Application.class);
    }

    public void testImproveHtml() throws Exception {
        String html = "<html> <head></head> <body> <br><br /> <br>TITLE <img width=\"1\" height=\"1\" src=\"http://test.gif\"> <br> <br> <br> <a href=\"http://test.htm\"><img src=\"http://test.img\"></a> <br> <a href=\"http://test.htm\"><img src=\"http://test.img\"></a> <br> <a href=\"http://test.htm\"><img src=\"http://test.img\"></a> <br> <br> <a href=\"http://test.htm\"><img src=\"http://test.img\"></a> <img width=\"1\" height=\"1\" src=\"http://test.img\"> <BR><br /> <BR></body> </html>";
        assertEquals(HtmlUtils.improveHtmlContent(html, "www."), "TITLE");
    }

    public void testArticleTextExtractor() throws Exception {
        String link = "http://www.20minutes.fr/societe/1556095-20150305-sivens-prefet-tarn-interdit-toute-manifestation-albi-vendredi";
        HttpURLConnection connection = NetworkUtils.setupConnection(link);

        String mobilizedHtml = ArticleTextExtractor.extractContent(connection.getInputStream(), link, "ou rassemblement ayant pour objet le projet de construction d",  ArticleTextExtractor.Mobilize.Yes);
        mobilizedHtml = HtmlUtils.improveHtmlContent(mobilizedHtml, NetworkUtils.getBaseUrl(link));
        if (mobilizedHtml.contains("Doit se conformer")) {
            throw new Exception("got comment part");
        }
    }

    public void testArticleTextExtractorThairath() throws Exception {
        String link = "http://www.thairath.co.th/content/544335";
        HttpURLConnection connection = NetworkUtils.setupConnection(link);

        String mobilizedHtml = ArticleTextExtractor.extractContent(connection.getInputStream(), "ฝนฟ้าคะนอง ฝนหนักบาง",  ArticleTextExtractor.Mobilize.Yes);
        System.out.println(mobilizedHtml);
        mobilizedHtml = HtmlUtils.improveHtmlContent(mobilizedHtml, NetworkUtils.getBaseUrl(link));
        if (!mobilizedHtml.contains("http://www.thairath.co.th/media/NjpUs24nCQKx5e1D74racLG80eobXUM1FQb68fZ0eH7.jpg")) {
            throw new Exception("no og image");
        }
    }

    public void testArticleTextExtractorNiceOppai() throws Exception {
        String link = "http://www.niceoppai.net/bleach/654/?all";
        HttpURLConnection connection = NetworkUtils.setupConnection(link);

        String mobilizedHtml = ArticleTextExtractor.extractContent(connection.getInputStream(), link, "Ch. 654 Dec 04, 2015", ArticleTextExtractor.Mobilize.Yes);
        System.out.println(mobilizedHtml);
        mobilizedHtml = HtmlUtils.improveHtmlContent(mobilizedHtml, NetworkUtils.getBaseUrl(link));
    }

    public void testRssAtomParserPubdate() throws Exception {
        RssAtomParser parser = new RssAtomParser(new Date(), 0, "1", "test2", "http://localhost/", false);
        assertEquals("Fri Dec 04 08:06:05 GMT+07:00 2015", parser.parsePubdateDate("Fri, 04 Dec 2015 01:06:05 +0000").toString());
    }
}