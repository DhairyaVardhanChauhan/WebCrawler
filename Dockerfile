# ---------- Build stage ----------
FROM maven:3.9.6-eclipse-temurin-17 AS builder

WORKDIR /app

COPY pom.xml .
RUN mvn -B -q dependency:go-offline

COPY src ./src
RUN mvn clean package -DskipTests

# ---------- Runtime stage ----------
FROM eclipse-temurin:17-jre

WORKDIR /app

# COPY EXACT shaded JAR (no wildcard)
COPY --from=builder /app/target/WebCrawler-1.0-SNAPSHOT.jar app.jar

ENV JAVA_OPTS="-Xms512m -Xmx512m -XX:+UseG1GC"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
