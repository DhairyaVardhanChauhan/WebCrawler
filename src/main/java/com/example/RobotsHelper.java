package com.example;

import com.google.gson.Gson;
import org.jsoup.Jsoup;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisCluster;

import java.awt.print.Pageable;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static com.example.Crawler.getJedis;

public class RobotsHelper{

    public static String fetchRobotsTxt(String domain){
        String url = "https://" + domain + "/robots.txt";
        try{
            String details = Jsoup.connect(url)
                    .userAgent("SimpleCrawler/1.0")
                    .timeout(5000)
                    .ignoreHttpErrors(true)
                    .execute()
                    .body();
//            System.out.println("Details: " + details);
            return details;
        } catch (IOException e) {
            return null;
        }
    }

    public static List<RobotsGroup> parseRobotsGroups(String content) {
        List<RobotsGroup> groups = new ArrayList<>();
        RobotsGroup current = null;

        if (content == null) return groups;

        for (String rawLine : content.split("\n")) {
            String line = rawLine.split("#", 2)[0].trim();
            if (line.isEmpty()) continue;
            String lower = line.toLowerCase(Locale.ROOT);
            if (lower.startsWith("user-agent:")) {
                String agent = line.split(":", 2)[1].trim().toLowerCase();
                if (current == null || !current.userAgents.isEmpty() && (!current.allow.isEmpty() || !current.disallow.isEmpty())) {
                    current = new RobotsGroup();
                    groups.add(current);
                }
                current.userAgents.add(agent);
            }
            else if (current != null && lower.startsWith("disallow:")) {
                String path = line.split(":", 2)[1].trim();
                current.disallow.add(path);
            }
            else if (current != null && lower.startsWith("allow:")) {
                String path = line.split(":", 2)[1].trim();
                current.allow.add(path);
            }
            else if (current != null && lower.startsWith("crawl-delay:")) {
                try {
                    current.crawlDelaySeconds =
                            Integer.parseInt(line.split(":", 2)[1].trim());
                } catch (Exception ignored) {}
            }
        }
        return groups;
    }

    public static RobotsRules resolveRules(
            List<RobotsGroup> groups,
            String myAgent
    ) {
        myAgent = myAgent.toLowerCase(Locale.ROOT);

        RobotsGroup best = null;
        int bestLen = -1;

        for (RobotsGroup group : groups) {
            for (String agent : group.userAgents) {
                if (agent.equals("*") || myAgent.contains(agent)) {
                    if (agent.length() > bestLen) {
                        bestLen = agent.length();
                        best = group;
                    }
                }
            }
        }

        RobotsRules rules = new RobotsRules();
        if (best == null) return rules;

        rules.getAllowed().addAll(best.allow);
        rules.getDisallowed().addAll(best.disallow);

        if (best.crawlDelaySeconds != null) {
            rules.crawlDelayMs = best.crawlDelaySeconds * 1000;
        }

        return rules;
    }



    public static RobotsRules parseRobots(String content, String myAgent) {
        List<RobotsGroup> groups = parseRobotsGroups(content);
        return resolveRules(groups, myAgent);
    }


    public static RobotsRules getRobotsRules(String domain) {
        String key = "robots:" + domain;
        try(Jedis jedis = getJedis()){
            String cached = jedis.get(key);
            if (cached != null) {
                return new Gson().fromJson(cached, RobotsRules.class);
            }
        }
        String robotsContent = fetchRobotsTxt(domain);
        RobotsRules parseRobotsRule = parseRobots(robotsContent,"*");

        try (Jedis jedis = getJedis()) {
            jedis.setex(
                    key,
                    24 * 3600,
                    new Gson().toJson(parseRobotsRule)
            );
        }
        return parseRobotsRule;
    }


    public static boolean isAllowedByRobots(String url, RobotsRules rules) {
        try {
            URI uri = new URI(url);
            String path = uri.getPath();
            for (String allow : rules.getAllowed()) {
                if (path.startsWith(allow)) return true;
            }
            for (String disallow : rules.getDisallowed()) {
                if (path.startsWith(disallow)) return false;
            }
            return true;
        } catch (Exception e) {
            return true;
        }
    }

}
