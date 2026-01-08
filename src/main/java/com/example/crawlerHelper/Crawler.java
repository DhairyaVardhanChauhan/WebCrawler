package com.example.crawlerHelper;


import com.google.common.hash.Hashing;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.params.SetParams;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;

public class Crawler {

    static JedisPool jedisPool = new JedisPool(System.getenv().getOrDefault("REDIS_HOST", "localhost"), 6379);
    public static Jedis getJedis() {
        return jedisPool.getResource();
    }
    
    public static String getDomain(String url) throws URISyntaxException {
        URI uri = new URI(url);
        String host = uri.getHost();
        if (host.startsWith("www.")) {
            return host.substring(4);
        }
        return host;
    }

}
