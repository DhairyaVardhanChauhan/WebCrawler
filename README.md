🕷️ Distributed Web Crawler (Java + Redis)

A production-grade, distributed web crawler built in Java using Jsoup, Redis, and Docker.
Designed to scale horizontally across machines while respecting robots.txt, crawl delays, and domain-level rate limits.

🚀 Features

✅ Distributed architecture using Redis as a coordination layer

✅ Multi-threaded workers per node

✅ Global URL deduplication (Redis-backed)

✅ Robots.txt compliance + crawl-delay enforcement

✅ Per-domain concurrency control

✅ Retry & dead-letter queues

✅ Crash-safe processing queue

✅ Dockerized for easy deployment


🧠 Architecture Overview
+------------------+
|  Crawler Worker  |
|  (Java JVM)      |
|  - Thread Pool   |
+--------+---------+
         |
         v
+-------------------------+
|        Redis            |
|-------------------------|
| queue                   |
| queue:retry             |
| queue:processing        |
| visitedUrls             |
| seenUrls                |
| domain:lock:*           |
| domain:active:*         |
| content:hashes          |
+-------------------------+


Key idea:

Workers are stateless. Redis is the single source of truth.

🛠️ Tech Stack

Java 17

Jsoup – HTML parsing

Jedis – Redis client

Redis / Redis Stack

Maven

Docker

📦 Project Structure
.
├── Dockerfile
├── pom.xml
├── README.md
└── src/
    └── main/java/com/example/
        ├── Crawler.java
        ├── RobotsHelper.java
        └── RobotsRules.java

⚙️ Prerequisites

Java 17+

Maven

Docker

Redis (local or containerized)


# To Start
docker compose up 
