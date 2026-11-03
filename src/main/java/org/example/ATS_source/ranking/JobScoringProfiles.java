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

                    /*
                     * =========================================================
                     * 1. CORE BACKEND SOFTWARE ENGINEERING
                     * Main goal: find real backend development jobs.
                     * =========================================================
                     */

                    Map.entry("backend engineering", 35),
                    Map.entry("backend engineer", 35),
                    Map.entry("backend development", 32),
                    Map.entry("backend developer", 32),
                    Map.entry("backend services", 30),
                    Map.entry("backend systems", 30),
                    Map.entry("backend infrastructure", 28),

                    Map.entry("backend", 25),
                    Map.entry("back-end", 25),
                    Map.entry("server-side", 22),

                    Map.entry("software engineering", 15),
                    Map.entry("software development", 15),

                    Map.entry("production systems", 15),
                    Map.entry("production services", 15),

                    /*
                     * =========================================================
                     * 2. YOUR PRIMARY STACK
                     * These should push Java/Spring jobs upwards.
                     * =========================================================
                     */

                    Map.entry("java", 25),
                    Map.entry("jvm", 25),

                    Map.entry("spring boot", 25),
                    Map.entry("spring framework", 20),
                    Map.entry("spring ecosystem", 20),
                    Map.entry("spring security", 15),
                    Map.entry("spring data", 15),

                    Map.entry("hibernate", 8),

                    /*
                     * =========================================================
                     * 3. APIs / BACKEND ARCHITECTURE
                     * =========================================================
                     */

                    Map.entry("rest api", 15),
                    Map.entry("restful api", 15),
                    Map.entry("api design", 14),
                    Map.entry("web services", 12),

                    Map.entry("grpc", 15),
                    Map.entry("graphql", 8),

                    Map.entry("microservices", 15),

                    Map.entry("webhooks", 8),
                    Map.entry("api gateway", 8),

                    Map.entry("authentication", 8),
                    Map.entry("authorization", 8),

                    Map.entry("oauth2", 8),
                    Map.entry("oauth 2.0", 8),
                    Map.entry("jwt", 8),

                    /*
                     * =========================================================
                     * 4. DISTRIBUTED / CONCURRENT / PARALLEL
                     * Strong bonus, but NOT required for a job to rank well.
                     * =========================================================
                     */

                    Map.entry("distributed systems", 25),
                    Map.entry("distributed system", 22),
                    Map.entry("distributed computing", 25),
                    Map.entry("distributed architecture", 20),

                    Map.entry("concurrency", 25),
                    Map.entry("concurrent programming", 25),

                    Map.entry("multithreading", 25),
                    Map.entry("multi-threading", 25),
                    Map.entry("multithreaded", 25),
                    Map.entry("multi-threaded", 25),

                    Map.entry("threading", 18),
                    Map.entry("thread pool", 20),
                    Map.entry("worker pool", 15),

                    Map.entry("parallel programming", 25),
                    Map.entry("parallel computing", 25),
                    Map.entry("parallel processing", 20),
                    Map.entry("parallelism", 22),

                    Map.entry("synchronization", 12),
                    Map.entry("non-blocking", 18),
                    Map.entry("lock-free", 20),

                    /*
                     * =========================================================
                     * 5. PERFORMANCE / SCALE
                     * Another area you're especially interested in.
                     * =========================================================
                     */

                    Map.entry("performance engineering", 25),
                    Map.entry("performance optimization", 22),
                    Map.entry("performance optimisation", 22),
                    Map.entry("performance tuning", 20),
                    Map.entry("performance profiling", 20),

                    Map.entry("profiling", 10),

                    Map.entry("high throughput", 20),
                    Map.entry("high-throughput", 20),

                    Map.entry("low latency", 20),
                    Map.entry("low-latency", 20),

                    Map.entry("throughput", 8),
                    Map.entry("latency", 8),

                    Map.entry("horizontal scaling", 15),
                    Map.entry("scalability", 10),
                    Map.entry("scalable systems", 12),
                    Map.entry("scalable services", 12),

                    Map.entry("high availability", 12),
                    Map.entry("fault tolerance", 15),
                    Map.entry("fault-tolerant", 15),

                    Map.entry("load balancing", 10),
                    Map.entry("rate limiting", 10),

                    /*
                     * =========================================================
                     * 6. ASYNC / EVENT-DRIVEN BACKENDS
                     * =========================================================
                     */

                    Map.entry("asynchronous processing", 18),
                    Map.entry("async processing", 18),
                    Map.entry("asynchronous", 10),

                    Map.entry("event-driven architecture", 18),
                    Map.entry("event-driven", 15),

                    Map.entry("message queues", 12),
                    Map.entry("message queue", 12),
                    Map.entry("message broker", 12),

                    Map.entry("kafka", 15),
                    Map.entry("rabbitmq", 10),

                    Map.entry("stream processing", 15),
                    Map.entry("real-time processing", 12),

                    Map.entry("idempotency", 12),
                    Map.entry("retries", 8),

                    /*
                     * =========================================================
                     * 7. DATABASES
                     * Strongly relevant to normal backend work.
                     * =========================================================
                     */

                    Map.entry("postgresql", 10),
                    Map.entry("mysql", 10),
                    Map.entry("couchbase", 10),

                    Map.entry("mongodb", 7),
                    Map.entry("redis", 8),
                    Map.entry("dynamodb", 7),
                    Map.entry("cassandra", 8),
                    Map.entry("elasticsearch", 6),

                    Map.entry("relational database", 8),
                    Map.entry("relational databases", 8),

                    Map.entry("database design", 10),
                    Map.entry("data modeling", 8),
                    Map.entry("database indexing", 10),

                    Map.entry("sql optimization", 12),
                    Map.entry("query optimization", 12),

                    /*
                     * =========================================================
                     * 8. OTHER BACKEND LANGUAGES
                     *
                     * Still relevant because you're open to learning another
                     * language if the role is genuinely backend engineering.
                     * =========================================================
                     */

                    Map.entry("kotlin", 8),
                    Map.entry("scala", 8),

                    Map.entry("c++", 8),
                    Map.entry("rust", 8),

                    Map.entry("golang", 7),

                    Map.entry("python", 5),
                    Map.entry("c#", 5),
                    Map.entry("ruby", 4),
                    Map.entry("php", 4),
                    Map.entry("node.js", 4),


                    /*
                     * =========================================================
                     * 9. PLATFORM / INFRASTRUCTURE SOFTWARE
                     *
                     * Interesting when it involves actual software development.
                     * =========================================================
                     */

                    Map.entry("platform engineering", 15),
                    Map.entry("platform engineer", 15),

                    Map.entry("infrastructure software", 15),
                    Map.entry("developer infrastructure", 15),

                    Map.entry("internal platform", 10),
                    Map.entry("developer platform", 10),

                    /*
                     * =========================================================
                     * 10. CLOUD / DEVOPS
                     *
                     * Useful backend skills but should NOT dominate ranking.
                     * =========================================================
                     */

                    Map.entry("docker", 6),
                    Map.entry("kubernetes", 6),

                    Map.entry("linux", 6),

                    Map.entry("aws", 4),
                    Map.entry("gcp", 4),
                    Map.entry("azure", 4),

                    Map.entry("terraform", 3),
                    Map.entry("serverless", 4),

                    Map.entry("lambda", 4),
                    Map.entry("sqs", 5),

                    /*
                     * =========================================================
                     * 11. OBSERVABILITY / PRODUCTION
                     * =========================================================
                     */

                    Map.entry("observability", 8),

                    Map.entry("prometheus", 5),
                    Map.entry("grafana", 5),
                    Map.entry("zipkin", 5),

                    Map.entry("distributed tracing", 8),
                    Map.entry("tracing", 4),

                    Map.entry("monitoring", 4),
                    Map.entry("logging", 3),
                    Map.entry("metrics", 3),

                    /*
                     * =========================================================
                     * 12. TESTING / ENGINEERING QUALITY
                     * =========================================================
                     */

                    Map.entry("unit testing", 4),
                    Map.entry("integration testing", 5),

                    Map.entry("junit", 5),
                    Map.entry("mockito", 4),

                    Map.entry("ci/cd", 3),
                    Map.entry("github actions", 2),
                    Map.entry("jenkins", 2),

                    Map.entry("code review", 2)
            ));


    public static final Map<String, ScoredKeyword> BACKEND_ROLE_PENALTIES =
            buildProfile(Map.ofEntries(

                    /*
                     * =========================================================
                     * MICROSOFT LOW-CODE / BUSINESS AUTOMATION
                     * =========================================================
                     */

                    Map.entry("power automate", 80),
                    Map.entry("microsoft power automate", 80),

                    Map.entry("power platform", 70),
                    Map.entry("microsoft power platform", 70),

                    Map.entry("copilot studio", 70),
                    Map.entry("microsoft copilot studio", 70),

                    Map.entry("power apps", 60),
                    Map.entry("powerapps", 60),

                    Map.entry("power virtual agents", 60),

                    /*
                     * Azure Logic Apps can sometimes appear in legitimate
                     * backend roles, so smaller penalty.
                     */
                    Map.entry("logic apps", 25),
                    Map.entry("azure logic apps", 25),

                    /*
                     * =========================================================
                     * LOW-CODE / NO-CODE
                     * =========================================================
                     */

                    Map.entry("low-code", 60),
                    Map.entry("low code", 60),

                    Map.entry("no-code", 60),
                    Map.entry("no code", 60),

                    Map.entry("citizen developer", 50),
                    Map.entry("citizen development", 50),

                    /*
                     * =========================================================
                     * RPA
                     * =========================================================
                     */

                    Map.entry("robotic process automation", 65),

                    /*
                     * "RPA" is relatively specific, unlike words such as
                     * automation or pipeline.
                     */
                    Map.entry("rpa", 50),

                    Map.entry("uipath", 60),
                    Map.entry("automation anywhere", 60),
                    Map.entry("blue prism", 60),

                    /*
                     * =========================================================
                     * ROLE / RESPONSIBILITY SIGNALS
                     * =========================================================
                     */

                    Map.entry("workflow automation specialist", 50),
                    Map.entry("business process automation", 40),
                    Map.entry("business automation specialist", 50),

                    Map.entry("low-code developer", 70),
                    Map.entry("low code developer", 70),

                    Map.entry("power platform developer", 80),
                    Map.entry("power automate developer", 80),

                    Map.entry("rpa developer", 70),
                    Map.entry("rpa engineer", 60)
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