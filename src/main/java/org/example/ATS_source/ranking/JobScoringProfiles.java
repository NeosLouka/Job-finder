package org.example.ATS_source.ranking;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

public class JobScoringProfiles {

    // A lightweight record to hold both the points and the pre-compiled pattern
    public record ScoredKeyword(int points, Pattern pattern) {}


    public static final Map<String, ScoredKeyword> BACKEND_ROLE =
            buildProfile(Map.ofEntries(
                    /* CORE BACKEND SIGNALS */
                    Map.entry("backend", 20),
                    Map.entry("back-end", 20),
                    Map.entry("backend services", 20),
                    Map.entry("backend development", 20),
                    Map.entry("server-side", 18),
                    Map.entry("web services", 15),

                    /* APIs / ARCHITECTURE */
                    Map.entry("rest api", 15),
                    Map.entry("restful api", 15),
                    Map.entry("graphql", 10),
                    Map.entry("grpc", 12),
                    Map.entry("microservices", 15),
                    Map.entry("event-driven architecture", 15),
                    Map.entry("event-driven", 10),
                    Map.entry("message queues", 10),
                    Map.entry("message queue", 10),
                    Map.entry("message broker", 10),
                    Map.entry("webhooks", 8),
                    Map.entry("api versioning", 8),
                    Map.entry("openapi", 8),
                    Map.entry("swagger", 7),

                    /* LANGUAGES / RUNTIMES */
                    Map.entry("java", 8),
                    Map.entry("python", 7),
                    Map.entry("go", 7),
                    Map.entry("golang", 7),
                    Map.entry("node.js", 7),
                    Map.entry("ruby", 6),
                    Map.entry("c#", 7),
                    Map.entry("rust", 7),
                    Map.entry("kotlin", 6),
                    Map.entry("php", 6),
                    Map.entry("typescript", 3),

                    /* BACKEND FRAMEWORKS */
                    Map.entry("spring boot", 12),
                    Map.entry("django", 10),
                    Map.entry("fastapi", 10),
                    Map.entry("flask", 8),
                    Map.entry("express.js", 9),
                    Map.entry("express", 7),
                    Map.entry("nestjs", 10),
                    Map.entry(".net core", 10),
                    Map.entry("asp.net core", 10),
                    Map.entry("ruby on rails", 10),
                    Map.entry("rails", 7),
                    Map.entry("gin", 8),
                    Map.entry("actix", 8),

                    /* DATABASES / PERSISTENCE */
                    Map.entry("postgresql", 9),
                    Map.entry("mysql", 8),
                    Map.entry("mongodb", 8),
                    Map.entry("redis", 7),
                    Map.entry("elasticsearch", 7),
                    Map.entry("dynamodb", 8),
                    Map.entry("cassandra", 8),
                    Map.entry("sql optimization", 10),
                    Map.entry("database indexing", 9),
                    Map.entry("database design", 9),
                    Map.entry("data modeling", 8),
                    Map.entry("sqlalchemy", 6),
                    Map.entry("hibernate", 7),
                    Map.entry("prisma", 6),

                    /* MESSAGING / DATA FLOW */
                    Map.entry("kafka", 10),
                    Map.entry("rabbitmq", 9),
                    Map.entry("stream processing", 10),
                    Map.entry("batch processing", 7),
                    Map.entry("etl pipelines", 7),
                    Map.entry("warehouse integration", 6),

                    /* CLOUD / INFRASTRUCTURE */
                    Map.entry("aws", 5),
                    Map.entry("ec2", 4),
                    Map.entry("lambda", 5),
                    Map.entry("s3", 3),
                    Map.entry("rds", 5),
                    Map.entry("sqs", 6),
                    Map.entry("gcp", 5),
                    Map.entry("azure", 5),
                    Map.entry("docker", 6),
                    Map.entry("kubernetes", 7),
                    Map.entry("terraform", 5),
                    Map.entry("serverless", 6),
                    Map.entry("load balancing", 8),
                    Map.entry("auto-scaling", 8),
                    Map.entry("autoscaling", 8),

                    /* SCALE / DISTRIBUTED SYSTEMS */
                    Map.entry("distributed systems", 15),
                    Map.entry("high availability", 12),
                    Map.entry("horizontal scaling", 12),
                    Map.entry("caching strategies", 9),
                    Map.entry("cdn", 5),
                    Map.entry("rate limiting", 10),
                    Map.entry("millions of requests", 12),
                    Map.entry("high-throughput", 12),
                    Map.entry("high throughput", 12),
                    Map.entry("sub-100ms latency", 10),
                    Map.entry("low latency", 9),
                    Map.entry("99.9% uptime", 10),
                    Map.entry("scalability", 8),
                    Map.entry("scalable", 6),
                    Map.entry("resilience", 8),
                    Map.entry("reliability", 7),

                    /* SECURITY / FINTECH BACKEND */
                    Map.entry("pci-dss", 9),
                    Map.entry("oauth 2.0", 8),
                    Map.entry("oauth2", 8),
                    Map.entry("jwt", 7),
                    Map.entry("encryption at rest", 8),
                    Map.entry("soc 2", 7),
                    Map.entry("idempotency", 10),
                    Map.entry("transaction processing", 10),
                    Map.entry("audit logging", 8),

                    /* DEVOPS / QUALITY */
                    Map.entry("ci/cd", 4),
                    Map.entry("github actions", 3),
                    Map.entry("jenkins", 3),
                    Map.entry("gitlab ci", 3),
                    Map.entry("unit testing", 5),
                    Map.entry("integration testing", 6),
                    Map.entry("tdd", 4),
                    Map.entry("code review", 3),
                    Map.entry("agile", 2),
                    Map.entry("scrum", 2),

                    /* PRODUCTION / OPERATIONS */
                    Map.entry("observability", 6),
                    Map.entry("monitoring", 4),
                    Map.entry("logging", 4),
                    Map.entry("metrics", 4),
                    Map.entry("production services", 10)
            ));

    /**
     * Sorts entries by key length (descending) and pre-compiles regex patterns ONCE
     * when the class is loaded.
     */
    private static Map<String, ScoredKeyword> buildProfile(Map<String, Integer> rawMap) {
        List<Map.Entry<String, Integer>> entries = new ArrayList<>(rawMap.entrySet());

        entries.sort(
                Comparator.comparingInt(
                        (Map.Entry<String, Integer> entry) -> entry.getKey().length()
                ).reversed()
        );

        Map<String, ScoredKeyword> profileMap = new LinkedHashMap<>();
        for (Map.Entry<String, Integer> entry : entries) {
            String keyword = entry.getKey();
            int points = entry.getValue();

            Pattern pattern = Pattern.compile(
                    "(?<![a-z0-9])"
                            + Pattern.quote(keyword.toLowerCase(Locale.ROOT))
                            + "(?![a-z0-9])",
                    Pattern.CASE_INSENSITIVE
            );

            profileMap.put(keyword, new ScoredKeyword(points, pattern));
        }

        return profileMap;
    }

    private JobScoringProfiles() {
    }
}