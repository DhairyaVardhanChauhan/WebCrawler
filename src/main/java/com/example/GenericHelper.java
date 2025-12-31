package com.example;

import com.google.common.hash.Hashing;
import redis.clients.jedis.Jedis;

import java.nio.charset.StandardCharsets;
import java.util.List;

public class GenericHelper {

    private static Jedis jedis = new Jedis("localhost", 6379);

    public static void main(String[] args) {

        String contentHash =
                Hashing.sha256()
                        .hashString("fasdfsadfsadfasfsasadfsfsa", StandardCharsets.UTF_8)
                        .toString();

        if(jedis.sadd("content:hashes", contentHash) == 0){
            System.out.println("Duplicate!!!!");
        }
        else{
            System.out.println("Not duplicate!!!");
        }

    }
}
