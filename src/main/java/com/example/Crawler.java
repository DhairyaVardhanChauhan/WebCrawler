package com.example;


import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Crawler {


    private static Set<String> visitedUrls = new HashSet<>();
    private static int maxDepth = 2;
    private static List<String[]> productData = new ArrayList<>();

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
            extractProductData(doc);
            Elements links = doc.select("a[href]");
            for(Element link:links){
                String nextUrl = link.absUrl("href");
                System.out.println(nextUrl);
                if(!nextUrl.isEmpty() && !visitedUrls.contains(nextUrl)){
                    crawl(nextUrl,depth+1);
                }
            }
        }
    }

    public static Document retrieveHTML(String url) throws IOException {

        Document document = Jsoup.connect(url).get();
//        System.out.println(document.html());
        return document;

    }

    public static void extractProductData(Document document){
        Elements products = document.select("li.product");
        for(Element product:products){
            String productName = product.select(".product-name").text();
            String price = product.select(".product-price").text();
            String imgUrl = product.select(".product-image").attr("src");

            System.out.println("product-name: " + productName);
            System.out.println("product-price: " + price);
            System.out.println("product-image: " + imgUrl);
            productData.add(new String[]{productName,price,imgUrl});
        }
    }

    private static void exportDataToCSV(String filePath) throws IOException {
        try(FileWriter fileWriter = new FileWriter(filePath)){
            fileWriter.append("Product Name,Price,Image URL\n");
            for(String[] row: productData){
                fileWriter.append(String.join(",", row));
                fileWriter.append("\n");
            }
            System.out.println("Data saved!");
        }catch (IOException e){
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws IOException {
        String seedUrl = "https://www.scrapingcourse.com/ecommerce/";
        Document document = retrieveHTML(seedUrl);
        if(document != null){
            System.out.println("Document fetched successfully!");
        }
        crawl(seedUrl, 1);
        exportDataToCSV("product.csv");
    }
}
