package com.example;


import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

public class Crawler {


    private static Set<String> visitedUrls = new HashSet<>();
    private static int maxDepth = 2;

    public static void crawl(String url,int depth) throws IOException {

        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            return;
        }

        if(depth > maxDepth || visitedUrls.contains(url)){
            return;
        }
        System.out.println("Crawling url: " + url);
        visitedUrls.add(url);
        Document doc = retrieveHTML(url);
        if(doc != null){
            Elements links = doc.select("a[href]");
            for(Element link:links){
                String nextUrl = link.absUrl("href");
                System.out.println(nextUrl);
                if(!nextUrl.isEmpty() && !visitedUrls.contains(url)){
                    crawl(nextUrl,depth++);
                }
            }
        }
    }

    public static Document retrieveHTML(String url) throws IOException {

        Document document = Jsoup.connect(url).get();
//        System.out.println(document.html());
        return document;

    }

    public static void main(String[] args) throws IOException {
        String seedUrl = "https://www.scrapingcourse.com/ecommerce/";
        Document document = retrieveHTML(seedUrl);
        if(document != null){
            System.out.println("Document fetched successfully!");
        }
        crawl(seedUrl, 1);
    }
}
