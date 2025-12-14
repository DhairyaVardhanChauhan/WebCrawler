package com.example;

import org.apache.hc.client5.http.fluent.Request;

public class APIRequest {
    public static void main(final String... args) throws Exception {
        String apiUrl = "https://api.zenrows.com/v1/?apikey=4b4be2e36a5d5921482686eb41218496c3b434ac&url=https%3A%2F%2Fwww.scrapingcourse.com%2Fecommerce%2F";
        String response = Request.get(apiUrl)
                .execute().returnContent().asString();

        System.out.println(response);
    }
}