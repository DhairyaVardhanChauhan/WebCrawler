package com.example;


import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class Crawler {


    private static Set<String> visitedUrls = new LinkedHashSet<>();
    private static int maxDepth = 2;
    private static List<List<String>> productData = new ArrayList<>();
    private static int total = 0;
    private static final ExecutorService executorService = Executors.newFixedThreadPool(15);
    public static void crawl(String url, int depth) throws IOException {

        if (!url.startsWith("http://") && !url.startsWith("https://")) return;
        if (depth >= maxDepth) return;

        synchronized (visitedUrls) {
            if (visitedUrls.contains(url)) return;
            visitedUrls.add(url);
        }

        System.out.println(Thread.currentThread().getName() + " Crawling url: " + url);

        Document doc = retrieveHTML(url);
        if (doc == null) return;

        extractProductData(doc);

        Elements links = doc.select("a[href]");
        List<String> nextUrls = new ArrayList<>();

        for (Element link : links) {
            String nextUrl = link.absUrl("href");
            if (!nextUrl.isEmpty()) nextUrls.add(nextUrl);
        }

        Collections.sort(nextUrls);

        for (String nextUrl : nextUrls) {
            executorService.submit(() -> {
                try {
                    crawl(nextUrl, depth + 1);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        }
    }


    public static Document retrieveHTML(String url) throws IOException {

        Document document = Jsoup.connect(url).get();
//        System.out.println(document.html());
        return document;

    }

    public static void extractProductData(Document document){
        Elements products = document.select("li.product");
        total += products.size();
        for(Element product:products){
            String productName = product.select(".product-name").text();
            String price = product.select(".product-price").text();
            String imgUrl = product.select(".product-image").attr("src");

//            System.out.println("product-name: " + productName);
//            System.out.println("product-price: " + price);
//            System.out.println("product-image: " + imgUrl);
            productData.add(List.of(productName, price, imgUrl));
        }
    }

    private static void exportDataToCSV(String filePath) throws IOException {
        try(FileWriter fileWriter = new FileWriter(filePath)){
            fileWriter.append("Product Name,Price,Image URL\n");
            for(List<String> row: productData){
                fileWriter.append(String.join(",", row));
                fileWriter.append("\n");
            }
            System.out.println("Data saved!");
        }catch (IOException e){
            e.printStackTrace();
        }
    }

    private static void shutdownExecutorService() {
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
        }
    }
    public static void main(String[] args) throws IOException, InterruptedException {
        String seedUrl = "https://www.scrapingcourse.com/ecommerce/";
        Document document = retrieveHTML(seedUrl);
        if(document != null){
            System.out.println("Document fetched successfully!");
        }
        crawl(seedUrl, 0);
        shutdownExecutorService();
        exportDataToCSV("product.csv");
        System.out.println(visitedUrls.size());
        System.out.println(total);
    }package com.example;


import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

    public class Crawler {


        private static Set<String> visitedUrls = new LinkedHashSet<>();
        private static int maxDepth = 2;
        private static List<List<String>> productData = new ArrayList<>();
        private static int total = 0;
        private static final ExecutorService executorService = Executors.newFixedThreadPool(15);
        public static void crawl(String url, int depth) throws IOException {

            if (!url.startsWith("http://") && !url.startsWith("https://")) return;
            if (depth >= maxDepth) return;

            synchronized (visitedUrls) {
                if (visitedUrls.contains(url)) return;
                visitedUrls.add(url);
            }

            System.out.println(Thread.currentThread().getName() + " Crawling url: " + url);

            Document doc = retrieveHTML(url);
            if (doc == null) return;

            extractProductData(doc);

            Elements links = doc.select("a[href]");
            List<String> nextUrls = new ArrayList<>();

            for (Element link : links) {
                String nextUrl = link.absUrl("href");
                if (!nextUrl.isEmpty()) nextUrls.add(nextUrl);
            }

            Collections.sort(nextUrls);

            for (String nextUrl : nextUrls) {
                executorService.submit(() -> {
                    try {
                        crawl(nextUrl, depth + 1);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
            }
        }


        public static Document retrieveHTML(String url) throws IOException {

            Document document = Jsoup.connect(url).get();
//        System.out.println(document.html());
            return document;

        }

        public static void extractProductData(Document document){
            Elements products = document.select("li.product");
            total += products.size();
            for(Element product:products){
                String productName = product.select(".product-name").text();
                String price = product.select(".product-price").text();
                String imgUrl = product.select(".product-image").attr("src");

//            System.out.println("product-name: " + productName);
//            System.out.println("product-price: " + price);
//            System.out.println("product-image: " + imgUrl);
                productData.add(List.of(productName, price, imgUrl));
            }
        }

        private static void exportDataToCSV(String filePath) throws IOException {
            try(FileWriter fileWriter = new FileWriter(filePath)){
                fileWriter.append("Product Name,Price,Image URL\n");
                for(List<String> row: productData){
                    fileWriter.append(String.join(",", row));
                    fileWriter.append("\n");
                }
                System.out.println("Data saved!");
            }catch (IOException e){
                e.printStackTrace();
            }
        }

        private static void shutdownExecutorService() {
            executorService.shutdown();
            try {
                if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
                    executorService.shutdownNow();
                }
            } catch (InterruptedException e) {
                executorService.shutdownNow();
            }
        }
        public static void main(String[] args) throws IOException, InterruptedException {
            String seedUrl = "https://www.scrapingcourse.com/ecommerce/";
            Document document = retrieveHTML(seedUrl);
            if(document != null){
                System.out.println("Document fetched successfully!");
            }
            crawl(seedUrl, 0);
            shutdownExecutorService();
            exportDataToCSV("product.csv");
            System.out.println(visitedUrls.size());
            System.out.println(total);
        }
    }

}
