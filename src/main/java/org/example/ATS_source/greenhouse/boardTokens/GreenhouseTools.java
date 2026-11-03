package org.example.ATS_source.greenhouse.boardTokens;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import org.example.ATS_source.model.AtsProvider;
import org.example.ATS_source.util.HttpRetryPolicy;
import org.example.ATS_source.util.TokenRepository;
import org.example.ATS_source.util.Utilities;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.Pattern;

@Getter
@Component
public class GreenhouseTools extends Utilities {

    private final Set<String> boardTokens = ConcurrentHashMap.newKeySet();
    private final Map<String, List<String>> companies = new ConcurrentSkipListMap<>();
    private final TokenRepository tokenRepository;
    private final Path OUTPUT = Path.of("data/greenhouse_tokens.txt");
    private final Pattern JOB_URL_PATTERN = Pattern.compile("^/v1/boards/([^/]+)/jobs/?$");

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public GreenhouseTools(
            TokenRepository tokenRepository,
            ObjectMapper objectMapper
    ){

        this.tokenRepository = tokenRepository;
        this.objectMapper = objectMapper;

        this.httpClient = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();

        try {
            this.boardTokens.addAll(loadExistingTokens(OUTPUT));
            System.out.println("Loaded " + boardTokens.size() + " board tokens.");

        } catch (IOException e) {
            System.err.println("Failed to pre-load tokens on startup: " + e.getMessage());
        }

    }

    @Override
    protected Set<String> discoverFromWayback() throws IOException, InterruptedException{

        Set<String> tokens = new HashSet<>();
        String prefix = "boards-api.greenhouse.io/v1/boards/";
        String resumeKey = null;

        do {

            StringBuilder url = new StringBuilder(
                    "https://web.archive.org/cdx/search/cdx"
            );

            url.append("?url=").append(encode(prefix));
            url.append("&matchType=prefix");
            url.append("&fl=original");
            url.append("&filter=statuscode:200");
            url.append("&collapse=urlkey");
            url.append("&limit=5000");
            url.append("&showResumeKey=true");

            if (resumeKey != null) {
                url.append("&resumeKey=").append(encode(resumeKey));
            }

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url.toString()))
                    .header(
                            "User-Agent",
                            "GreenhouseBoardResearch/1.0"
                    )
                    .GET()
                    .build();

            HttpResponse<String> response =
                    CLIENT.send(
                            request,
                            HttpResponse.BodyHandlers.ofString()
                    );

            if (response.statusCode() != 200) {
                System.err.println(
                        "Wayback returned HTTP " +
                                response.statusCode()
                );

                break;
            }

            String[] lines = response.body().split("\\R");

            resumeKey = null;

            for (String line : lines) {

                line = line.trim();

                if (line.isEmpty()) {
                    continue;
                }

                if (line.startsWith("http://") || line.startsWith("https://")) {

                    extractToken(line, JOB_URL_PATTERN).ifPresent(tokens::add);

                } else {

                    // With showResumeKey=true the last
                    // non-URL line is the continuation key.
                    resumeKey = line;
                }
            }

            System.out.println(
                    "Unique candidates so far: " +
                            tokens.size()
            );

            Thread.sleep(1000);

        } while (resumeKey != null);

        return tokens;

    }

    @Override
    protected boolean isCurrentlyValid(String token) {

        String url =
                "https://boards-api.greenhouse.io/v1/boards/"
                        + encodePath(token)
                        + "/jobs";

        HttpRequest request =
                HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .timeout(
                                Duration.ofSeconds(20)
                        )
                        .header(
                                "User-Agent",
                                "GreenhouseBoardResearch/1.0"
                        )
                        .GET()
                        .build();


        try {

            HttpResponse<String> response =
                    CLIENT.send(
                            request,
                            HttpResponse.BodyHandlers.ofString()
                    );


            if (response.statusCode() == 429) {

                System.err.println(
                        "RATE LIMITED -> "
                                + token
                );

                response =
                        HttpRetryPolicy.retryPolicy(
                                CLIENT,
                                request,
                                AtsProvider.GREENHOUSE.name()
                                        + " "
                                        + token
                        );
            }


            if (response.statusCode() != 200) {

                System.err.println(
                        "INVALID -> "
                                + token
                                + " | HTTP "
                                + response.statusCode()
                );

                return false;
            }


            return response.body()
                    .contains("\"jobs\"");


        } catch (IOException e) {

            System.err.println(
                    "Validation failed for "
                            + token
                            + " | "
                            + e.getClass().getSimpleName()
                            + ": "
                            + e.getMessage()
            );

            return false;

        } catch (InterruptedException e) {

            Thread.currentThread()
                    .interrupt();

            System.err.println(
                    "Validation interrupted for "
                            + token
            );

            return false;
        }
    }
    @Async
    public void findTokens() throws Exception{

        boardTokens.addAll(loadExistingTokens(OUTPUT));

        System.out.println("Already known: " + boardTokens.size());

        //Wayback discovery
        Set<String> discovered = discoverFromWayback();
        System.out.println("Wayback candidates v1: " + discovered.size());

        for (String token : discovered) {

            if (boardTokens.contains(token)) {
                continue;
            }

            System.out.println("Checking: " + token);

            if (isCurrentlyValid(token)) {

                System.out.println("VALID -> " + token);

                boardTokens.add(token);

                tokenRepository.saveToken(token);

                addTokenToCompany(token);

            } else {
                System.out.println("Dead/invalid -> " + token);
            }

            Thread.sleep(200);
        }

        System.out.println("\nFinished.");
        System.out.println("Total valid tokens: " + boardTokens.size());

    }

    public List<String> retrieveExistingTokens() throws IOException {
        if (boardTokens.isEmpty()) {
            boardTokens.addAll(loadExistingTokens(OUTPUT));
        }

        return new ArrayList<>(loadExistingTokens(OUTPUT));
    }

    public List<String> putTokensInAlphabeticOrder() throws IOException {


        List<String> tokens = retrieveExistingTokens();

        Collections.sort(tokens); //Alphabetic order
        System.out.println("Tokens shorted in alphabetic order.");

        return tokens;
    }

    public Map<String, List<String>> groupTokensByCompany() {

        List<String> tokens = new ArrayList<>(boardTokens);

        Map<String, List<String>> groupedCompanies = new ConcurrentHashMap<>();

        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {

            for (String token : tokens) {

                executor.submit(() -> {

                    String companyName = getCompanyName(token);

                    if (companyName == null || companyName.isBlank()) {
                        return;
                    }

                    groupedCompanies.computeIfAbsent(
                            companyName,
                            key -> Collections.synchronizedList(new ArrayList<>())).add(token);
                }
                );
            }
        }


        return new TreeMap<>(groupedCompanies);
    }


    private String getCompanyName(String token) {

        try {

            String url = "https://boards-api.greenhouse.io/v1/boards/" + token;

            HttpRequest request = HttpRequest
                    .newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient
                    .send(
                            request,
                            HttpResponse.BodyHandlers.ofString()
                    );

            if (response.statusCode() != 200) {
                return null;
            }

            JsonNode json = objectMapper
                    .readTree(
                            response.body()
                    );

            return json.path("name").asText();

        } catch (Exception e) {

            System.err.println("Failed company lookup for token: " + token);

            return null;
        }
    }

    private void addTokenToCompany(String token) {

        String companyName = getCompanyName(token);

        if (companyName == null || companyName.isBlank()) {
            return;
        }

        companies.computeIfAbsent(companyName, key -> new ArrayList<>()).add(token);
    }

    public void mergeCompanies() {

        Map<String, List<String>> mergedCompanies = new ConcurrentSkipListMap<>();

        String previousCompany = null;
        String previousFirstWord = null;


        for (String company : companies.keySet()) {

            List<String> tokens = companies.get(company);

            if (previousCompany == null) {

                mergedCompanies.put(company, new ArrayList<>(tokens));

                previousCompany = company;

                previousFirstWord = getFirstWord(company);

                continue;
            }


            if (company.toLowerCase().contains(previousFirstWord.toLowerCase())) {

                mergedCompanies.get(previousCompany).addAll(tokens);

            } else {

                mergedCompanies.put(company, new ArrayList<>(tokens));

                previousCompany = company;

                previousFirstWord = getFirstWord(company);
            }
        }


        companies.clear();

        companies.putAll(mergedCompanies);
    }


    public Set<String> validateExistingTokens() throws IOException {

        Set<String> existingTokens =
                loadExistingTokens(OUTPUT);

        Set<String> validTokens =
                ConcurrentHashMap.newKeySet();

        System.out.println(
                "Validating "
                        + existingTokens.size()
                        + " Greenhouse tokens..."
        );


        try (
                ExecutorService executor =
                        Executors.newVirtualThreadPerTaskExecutor()
        ) {

            Semaphore semaphore =
                    new Semaphore(20);


            for (String token : existingTokens) {

                executor.submit(() -> {

                    boolean acquired = false;

                    try {

                        semaphore.acquire();
                        acquired = true;

                        if (isCurrentlyValid(token)) {

                            validTokens.add(token);

                        } else {

                            System.out.println(
                                    "INVALID -> "
                                            + token
                            );
                        }

                    } catch (Exception e) {

                        System.err.println(
                                "Failed validation for "
                                        + token
                                        + ": "
                                        + e.getMessage()
                        );

                    } finally {

                        if (acquired) {
                            semaphore.release();
                        }
                    }
                });
            }
        }


        rewriteTokens(
                validTokens,
                OUTPUT
        );


        boardTokens.clear();
        boardTokens.addAll(validTokens);


        System.out.println(
                "Validation complete. Valid tokens: "
                        + validTokens.size()
        );

        return validTokens;
    }


    private String getFirstWord(String companyName) {

        return companyName
                .trim()
                .split("\\s+")[0];
    }


}
