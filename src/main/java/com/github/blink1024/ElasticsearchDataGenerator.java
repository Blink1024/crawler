package com.github.blink1024;

import org.apache.http.HttpHost;
import org.apache.ibatis.io.Resources;

import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;


public class ElasticsearchDataGenerator {
    public static void main(String[] args) {
        SqlSessionFactory sqlSessionFactory;
        try {
            String resource = "db/mybatis/config.xml";
            InputStream inputStream = Resources.getResourceAsStream(resource);
            sqlSessionFactory = new SqlSessionFactoryBuilder().build(inputStream);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        List<News> newsFromMySQL = getNewsFromMySQL(sqlSessionFactory);
        for (int i = 0; i < 10; i++) {
            new Thread(() -> writeSingleThread(newsFromMySQL)).start();
        }

    }

    private static void writeSingleThread(List<News> newsFromMySQL) {
        try (RestHighLevelClient client = new RestHighLevelClient(RestClient.builder(new HttpHost("localhost", 9200, "http")))) {
            for (int i = 0; i < 100; i++) {
                BulkRequest bulkRequest = new BulkRequest();
                for (News news : newsFromMySQL) {
                    IndexRequest request = new IndexRequest("news");
                    HashMap<String, Object> map = new HashMap<>();
                    map.put("title", news.getTitle());
                    map.put("content", news.getContent().length() > 10 ? news.getContent().substring(0, 10) : news.getContent());
                    map.put("url", news.getUrl());

                    request.source(map, XContentType.JSON);
                    bulkRequest.add(request);
                }
                BulkResponse bulk = client.bulk(bulkRequest, RequestOptions.DEFAULT);
                System.out.println(bulk.status().getStatus());
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static List<News> getNewsFromMySQL(SqlSessionFactory sqlSessionFactory) {
        try (SqlSession session = sqlSessionFactory.openSession()) {
            return session.selectList("com.github.blink1024.MockMapper.selectNews");
        }
    }
}

