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

public class Main {
    @SuppressFBWarnings("DMI_CONSTANT_DB_PASSWORD")
    public static void main(String[] args) throws IOException, SQLException {
        Connection connection = DriverManager.getConnection("jdbc:h2:file:C:/Users/ZN/IdeaProjects/crawler/news", "root", "root");
        String link;
        while ((link = getNextLinkThenDelete(connection)) != null) {
            if (isLinkProcessed(connection, link)) {
                continue;
            }
            if (isInterestingLink(link)) {
                Document doc = httpGetAndParseHtml(link);
                parseUrlsFromPageAndStoreIntoDatabase(connection, doc);
                storeIntoDatabaseIfItIsNewsPage(connection, doc, link);
                updateDatabase(connection, link, "insert into LINKS_ALREADY_PROCESSED (LINK)values ( ? )");
            }
        }
    }

    private static void parseUrlsFromPageAndStoreIntoDatabase(Connection connection, Document doc) throws SQLException {
        for (Element aTag : doc.select("a")) {
            String href = aTag.attr("href");
            if (href.startsWith("//")) {
                href = "https:" + href;
            }
            if (!href.toLowerCase().startsWith("javascript")) {
                updateDatabase(connection, href, "insert into LINKS_TO_BE_PROCESSED (LINK)values ( ? )");
            }
        }
    }

    private static boolean isLinkProcessed(Connection connection, String link) throws SQLException {
        ResultSet resultSet = null;
        try (PreparedStatement statement = connection.prepareStatement("select LINK from LINKS_ALREADY_PROCESSED where LINK=?")) {
            statement.setString(1, link);
            resultSet = statement.executeQuery();
            if (resultSet.next()) {
                return true;
            }
        } finally {
            if (resultSet != null) {
                resultSet.close();
            }
        }
        return false;
    }

    private static void updateDatabase(Connection connection, String link, String sql) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, link);
            statement.executeUpdate();
        }
    }

    private static String getNextLinkThenDelete(Connection connection) throws SQLException {
        String link = getNextLink(connection, "select LINK from LINKS_TO_BE_PROCESSED limit 1");
        if (link != null) {
            updateDatabase(connection, link, "delete from LINKS_TO_BE_PROCESSED where LINK=?");
        }
        return link;
    }

    private static String getNextLink(Connection connection, String sql) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql); ResultSet resultSet = statement.executeQuery()) {
            if (resultSet.next()) {
                return resultSet.getString(1);
            }
        }
        return null;
    }

    private static void storeIntoDatabaseIfItIsNewsPage(Connection connection, Document doc, String link) throws SQLException {
        Elements articleTags = doc.select("article.art_box");
        if (!articleTags.isEmpty()) {
            Element articleTag = doc.selectFirst("article.art_box");
            String title = articleTag.selectFirst("h1").text();
            String content = articleTag.select(".art_content .art_p").stream().map(Element::text).collect(Collectors.joining("\n"));
            System.out.println(title);
            try (PreparedStatement statement = connection.prepareStatement
                    ("insert into NEWS(title, content, url, created_at, modified_at)values ( ?,?,?,now(),now() ) ")) {
                statement.setString(1, title);
                statement.setString(2, content);
                statement.setString(3, link);
                statement.executeUpdate();
            }
        }
        }

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
