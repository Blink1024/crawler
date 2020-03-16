package com.github.blink1024;

import java.sql.SQLException;

public interface CrawlerDao {
    String getNextLink(String sql) throws SQLException;
    String getNextLinkThenDelete() throws SQLException;
    void updateDatabase(String link, String sql) throws SQLException;
    void storeNewsIntoDatabase(String title, String content, String url) throws SQLException;
    boolean isLinkProcessed(String link) throws SQLException;
}
