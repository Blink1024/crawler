package com.github.blink1024;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.sql.*;
import java.util.stream.Collectors;

public class Crawler {

    private CrawlerDao dao = new JdbcCrawlerDao();

    public void run() throws SQLException, IOException {
        String link;
        while ((link = dao.getNextLinkThenDelete()) != null) {
            if (dao.isLinkProcessed(link)) {
                continue;
            }
            if (isInterestingLink(link)) {
                Document doc = httpGetAndParseHtml(link);
                parseUrlsFromPageAndStoreIntoDatabase(doc);
                storeIntoDatabaseIfItIsNewsPage(doc, link);
                dao.updateDatabase(link, "insert into LINKS_ALREADY_PROCESSED (LINK)values ( ? )");
            }
        }
    }

    @SuppressFBWarnings("DMI_CONSTANT_DB_PASSWORD")
    public static void main(String[] args) throws IOException, SQLException {
        new Crawler().run();
    }

    private void parseUrlsFromPageAndStoreIntoDatabase(Document doc) throws SQLException {
        for (Element aTag : doc.select("a")) {
            String href = aTag.attr("href");
            if (href.startsWith("//")) {
                href = "https:" + href;
            }
            if (!href.toLowerCase().startsWith("javascript")) {
                dao.updateDatabase(href, "insert into LINKS_TO_BE_PROCESSED (LINK)values ( ? )");
            }
        }
    }


    private void storeIntoDatabaseIfItIsNewsPage(Document doc, String link) throws SQLException {
        Elements articleTags = doc.select("article.art_box");
        if (!articleTags.isEmpty()) {
            Element articleTag = doc.selectFirst("article.art_box");
            String title = articleTag.selectFirst("h1").text();
            String content = articleTag.select(".art_content .art_p").stream().map(Element::text).collect(Collectors.joining("\n"));
            System.out.println(title);
            dao.storeNewsIntoDatabase(title, content, link);
        }
    }

    @SuppressFBWarnings("RCN_REDUNDANT_NULLCHECK_WOULD_HAVE_BEEN_A_NPE")
    private static Document httpGetAndParseHtml(String link) throws IOException {
        CloseableHttpClient httpClient = HttpClients.createDefault();

        System.out.println(link);

        HttpGet httpGet = new HttpGet(link);
        httpGet.addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
                "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/78.0.3904.108 Safari/537.36");
        try (CloseableHttpResponse response = httpClient.execute(httpGet)) {
            System.out.println(response.getStatusLine());
            HttpEntity entity = response.getEntity();
            String html = EntityUtils.toString(entity);
            return Jsoup.parse(html);
        }
    }

    private static boolean isInterestingLink(String link) {
        return (link.contains("news.sina.cn")
                || link.equals("https://sina.cn")
                || link.contains("nba.sina.cn")
                || link.contains("auto.sina.cn")
                || link.contains("tech.sina.cn")
                || link.contains("finance.sina.cn")
                || link.contains("house.sina.cn")
                || link.contains("top.sina.cn")
                || link.contains("cul.sina.cn")
                || link.contains("edu,.cn")
                && !link.contains("\\")
                && !link.contains("passport.sina.cn")
        );
    }
}
