package net.fred.feedex;

import android.app.Application;
import android.test.ApplicationTestCase;

import net.fred.feedex.utils.ArticleTextExtractor;
import net.fred.feedex.utils.HtmlUtils;
import net.fred.feedex.utils.NetworkUtils;

import java.net.HttpURLConnection;

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

        String mobilizedHtml = ArticleTextExtractor.extractContent(connection.getInputStream(), "ou rassemblement ayant pour objet le projet de construction d");
        mobilizedHtml = HtmlUtils.improveHtmlContent(mobilizedHtml, NetworkUtils.getBaseUrl(link));
        if (mobilizedHtml.contains("Doit se conformer")) {
            throw new Exception("got comment part");
        }
    }
}