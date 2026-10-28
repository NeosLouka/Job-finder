package org.example.ATS_source.ashby.boardTokens;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.ATS_source.util.Utilities;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.time.Duration;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.*;
import java.util.regex.Pattern;

@Component
public class AshbyTools extends Utilities {

    private static final Path OUTPUT = Path.of("data/ashby_board_names.txt");

    private static final Path CANDIDATES_OUTPUT = Path.of("data/ashby/ashby_candidates.txt");
    private static final Path FETCHED_PAGES_OUTPUT = Path.of("data/ashby/ashby_wayback_pages.txt");

    private static final Path VALID_BOARDS_OUTPUT = Path.of("data/ashby/ashby_valid_boardNames.txt");

    private static final long WAYBACK_REQUEST_INTERVAL_MS = 2000;
    private final Object waybackRateLock = new Object();
    private long nextWaybackRequestTime = 0;

    private final HttpClient waybackClient =
            HttpClient.newBuilder()
                    .version(HttpClient.Version.HTTP_1_1)
                    .connectTimeout(Duration.ofSeconds(20))
                    .followRedirects(HttpClient.Redirect.NORMAL)
                    .build();

    /*
     * Matches:
     *
     * /ashby
     * /ashby/1234-job-id
     * /notion/some-job
     *
     * group(1) = board name
     */
    private static final Pattern JOB_URL_PATTERN =
            Pattern.compile(
                    "^/([A-Za-z0-9._-]+)(?:/.*)?$"
            );

    private final ObjectMapper objectMapper;


    public AshbyTools(ObjectMapper objectMapper) {

        this.objectMapper = objectMapper;
    }


    public Set<String> retrieveExistingBoardNames() throws IOException {

        return loadExistingTokens(
                VALID_BOARDS_OUTPUT
        );
    }

    public Set<String> validateBoardNames() throws IOException {

        Set<String> candidates =
                loadExistingTokens(
                        CANDIDATES_OUTPUT
                );

        Set<String> validTokens = ConcurrentHashMap.newKeySet();
        System.out.println(
                "Validating "
                        + candidates.size()
                        + " Ashby candidates..."
        );


        try (ExecutorService executor = Executors.newFixedThreadPool(40)) {

            for (String boardName : candidates) {

                executor.submit(() -> {

                    try{
                        if(isCurrentlyValid(boardName)){

                            validTokens.add(boardName);
                        }
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                });

            }
        }


        rewriteTokens(
                validTokens,
                VALID_BOARDS_OUTPUT
        );


        System.out.println(
                "Validation complete."
        );

        System.out.println(
                "Valid Ashby boards: "
                        + validTokens.size()
        );


        return validTokens;
    }



    @Override
    protected Set<String> discoverFromWayback() throws IOException, InterruptedException {

        int totalPages = getWaybackPageCount();
        System.out.println("Total Wayback pages: " + totalPages);

        Set<String> candidates = ConcurrentHashMap.newKeySet();
        candidates.addAll(loadExistingTokens(CANDIDATES_OUTPUT));

        Set<Integer> fetchedPages = ConcurrentHashMap.newKeySet();
        fetchedPages.addAll(loadFetchedPages());

        System.out.println(
                "Already fetched pages: "
                        + fetchedPages.size()
        );

        System.out.println(
                "Already known candidates: "
                        + candidates.size()
        );


        /*
         * Maximum 3 Wayback requests actually active.
         */
        Semaphore semaphore = new Semaphore(3);


        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {

            for (int page = 0; page < totalPages; page++) {

                if (fetchedPages.contains(page)) {
                    continue;
                }


                int currentPage = page;


                executor.submit(() -> {

                    boolean acquired = false;

                    try {

                        semaphore.acquire();
                        acquired = true;


                        JsonNode root = fetchWaybackPage(currentPage);

                        Set<String> pageCandidates = extractBoardNames(root);


                        /*
                         * Save candidates immediately.
                         */
                        for (String boardName : pageCandidates) {

                            if (candidates.add(boardName)) {

                                appendToken(
                                        boardName,
                                        CANDIDATES_OUTPUT
                                );
                            }
                        }


                        /*
                         * IMPORTANT:
                         *
                         * Mark page completed ONLY AFTER
                         * its candidates were successfully saved.
                         */
                        appendToken(
                                String.valueOf(currentPage),
                                FETCHED_PAGES_OUTPUT
                        );

                        fetchedPages.add(
                                currentPage
                        );


                        System.out.println(
                                "Page "
                                        + currentPage
                                        + " completed"
                                        + " | found: "
                                        + pageCandidates.size()
                                        + " | total candidates: "
                                        + candidates.size()
                        );


                    } catch (Exception e) {

                        System.err.println(
                                "Page "
                                        + currentPage
                                        + " failed: "
                                        + e.getMessage()
                        );

                        /*
                         * DO NOT mark page as fetched.
                         *
                         * Next run will try this page again.
                         */

                    } finally {

                        if (acquired) {
                            semaphore.release();
                        }
                    }
                });
            }
        }


        System.out.println(
                "Fetched pages: "
                        + fetchedPages.size()
                        + "/"
                        + totalPages
        );

        System.out.println(
                "Ashby candidates: "
                        + candidates.size()
        );


        return candidates;
    }


    private void pauseWayback(
            long millis
    ) {

        synchronized (waybackRateLock) {

            long pauseUntil =
                    System.currentTimeMillis()
                            + millis;

            if (pauseUntil
                    > nextWaybackRequestTime) {

                nextWaybackRequestTime =
                        pauseUntil;
            }
        }
    }

    private void waitForWaybackSlot() throws InterruptedException {

        synchronized (waybackRateLock) {

            long now = System.currentTimeMillis();

            long wait = nextWaybackRequestTime - now;

            if (wait > 0) {

                Thread.sleep(wait);
            }

            nextWaybackRequestTime = System.currentTimeMillis() + WAYBACK_REQUEST_INTERVAL_MS;
        }
    }

    private Set<Integer> loadFetchedPages() throws IOException {

        Set<Integer> pages = new HashSet<>();


        for (String value : loadExistingTokens(FETCHED_PAGES_OUTPUT)) {

            try {
                pages.add(Integer.parseInt(value));

            } catch (NumberFormatException ignored) {
            }
        }


        return pages;
    }


    private JsonNode fetchWaybackPage(int page) throws IOException, InterruptedException {

        String url =
                "https://web.archive.org/cdx/search/cdx"
                        + "?url=jobs.ashbyhq.com/*"
                        + "&output=json"
                        + "&fl=urlkey,original"
                        + "&filter=statuscode:200"
                        + "&collapse=urlkey"
                        + "&pageSize=1"
                        + "&page="
                        + page;


        HttpRequest request =
                HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .header(
                                "User-Agent",
                                "Mozilla/5.0"
                        )
                        .GET()
                        .build();


        HttpResponse<String> response =
                sendWaybackRequestWithRetry(
                        request
                );


        return objectMapper.readTree(response.body());
    }


    private int getWaybackPageCount() throws IOException, InterruptedException {

        String url =
                "https://web.archive.org/cdx/search/cdx"
                        + "?url=jobs.ashbyhq.com/*"
                        + "&filter=statuscode:200"
                        + "&collapse=urlkey"
                        + "&pageSize=1"
                        + "&showNumPages=true";

        HttpRequest request =
                HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .header(
                                "User-Agent",
                                "Mozilla/5.0"
                        )
                        .GET()
                        .build();

        HttpResponse<String> response = sendWaybackRequestWithRetry(request);

        String body = response.body().trim();

        try {

            return Integer.parseInt(body);

        } catch (NumberFormatException e) {

            throw new IOException(
                    "Invalid Wayback page count: "
                            + body,
                    e
            );
        }
    }

    @Override
    protected boolean isCurrentlyValid(String boardName) {

        String url = "https://api.ashbyhq.com/posting-api/job-board/" + boardName;

        HttpRequest request =
                HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .GET()
                        .build();

        int maxAttempts = 20;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {

            try {

                HttpResponse<Void> response =
                        CLIENT.send(
                                request,
                                HttpResponse.BodyHandlers.discarding()
                        );

                if (response.statusCode() == 200) {
                    return true;
                }

                if (response.statusCode() == 404) {
                    return false;
                }

                if (response.statusCode() == 429) {

                    long delay = attempt * 5000L;

                    System.out.println(
                            "Rate limited: "
                                    + boardName
                                    + " | retrying in "
                                    + delay / 1000
                                    + "s"
                    );

                    Thread.sleep(delay);

                    continue;
                }

                if (response.statusCode() >= 500) {

                    Thread.sleep(attempt * 3000L);

                    continue;
                }

                return false;

            } catch (IOException e) {

                try {
                    Thread.sleep(attempt * 3000L);
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    return false;
                }

            } catch (InterruptedException e) {

                Thread.currentThread().interrupt();
                return false;
            }
        }

        return false;
    }

    private HttpResponse<String> sendWaybackRequestWithRetry(HttpRequest request) throws IOException, InterruptedException {

        int maxAttempts = 5;

        IOException lastException = null;


        for (int attempt = 1; attempt <= maxAttempts; attempt++) {

            /*
             * Every attempt respects our Wayback rate.
             */
            waitForWaybackSlot();

            try {

                HttpResponse<String> response =
                        waybackClient.send(
                                request,
                                HttpResponse.BodyHandlers.ofString()
                        );


                if (response.statusCode() == 200) {

                    return response;
                }


                /*
                 * Wayback explicitly rate-limited us.
                 */
                if (response.statusCode() == 429) {

                    long retryDelay =
                            getRetryAfterMillis(
                                    response
                            );

                    System.err.println(
                            "Wayback rate limited us (429). "
                                    + "Pausing requests for "
                                    + retryDelay / 1000
                                    + " seconds."
                    );

                    pauseWayback(
                            retryDelay
                    );

                    continue;
                }


                /*
                 * Temporary server problems.
                 */
                if (response.statusCode() == 502
                        || response.statusCode() == 503
                        || response.statusCode() == 504) {

                    long delay =
                            attempt * 5000L;

                    System.err.println(
                            "Wayback HTTP "
                                    + response.statusCode()
                                    + " - retrying in "
                                    + delay / 1000
                                    + " seconds"
                    );

                    Thread.sleep(
                            delay
                    );

                    continue;
                }


                throw new IOException(
                        "Wayback returned HTTP "
                                + response.statusCode()
                );


            } catch (IOException e) {

                lastException = e;

                System.err.println(
                        "Wayback connection failed: "
                                + e.getMessage()
                                + " attempt "
                                + attempt
                                + "/"
                                + maxAttempts
                );


                if (attempt < maxAttempts) {

                    Thread.sleep(
                            attempt * 5000L
                    );
                }
            }
        }


        throw new IOException(
                "Wayback failed after "
                        + maxAttempts
                        + " attempts",
                lastException
        );
    }


    private long getRetryAfterMillis(
            HttpResponse<?> response
    ) {

        return response.headers()
                .firstValue("Retry-After")
                .map(value -> {

                    try {

                        return Long.parseLong(value)
                                * 1000L;

                    } catch (NumberFormatException e) {

                        return 30_000L;
                    }
                })
                .orElse(
                        30_000L
                );
    }


    private Set<String> extractBoardNames(JsonNode root) {

        Set<String> boardNames = new HashSet<>();


        /*
         * index 0:
         *
         * ["urlkey", "original"]
         *
         * so skip it.
         */
        for (int i = 1; i < root.size(); i++) {

            JsonNode row = root.get(i);

            if (!row.isArray() || row.size() < 2) {

                continue;
            }

            String originalUrl = row.get(1).asText();

            extractToken(
                    originalUrl,
                    JOB_URL_PATTERN
            ).ifPresent(
                    boardNames::add
            );
        }


        return boardNames;
    }

    public Set<String> discoverCandidates() throws IOException, InterruptedException {

        return discoverFromWayback();
    }

}