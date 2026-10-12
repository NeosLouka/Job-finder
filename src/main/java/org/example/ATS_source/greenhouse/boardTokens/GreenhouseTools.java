package org.example.ATS_source.greenhouse.boardTokens;

import lombok.Getter;
import org.example.ATS_source.util.TokenRepository;
import org.example.ATS_source.util.Utilities;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

@Getter
@Component
public class GreenhouseTools extends Utilities {

    private final Set<String> boardTokens = ConcurrentHashMap.newKeySet();

    private final TokenRepository tokenRepository;

    private final Path OUTPUT = Path.of("data/greenhouse_tokens.txt");

    private final Pattern JOB_URL_PATTERN = Pattern.compile("^/v1/boards/([^/]+)/jobs/?$");

    public GreenhouseTools(TokenRepository tokenRepository){

        this.tokenRepository = tokenRepository;

        try {
            this.boardTokens.addAll(loadExistingTokens(OUTPUT));
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
                    .timeout(Duration.ofSeconds(60))
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

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(20))
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

            if (response.statusCode() != 200) {
                return false;
            }

            /*
             * A valid Greenhouse jobs response normally
             * contains the "jobs" field.
             */
            return response.body().contains("\"jobs\"");

        } catch (Exception e) {

            System.err.println(
                    "Validation failed for "
                            + token
                            + ": "
                            + e.getMessage()
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



}
