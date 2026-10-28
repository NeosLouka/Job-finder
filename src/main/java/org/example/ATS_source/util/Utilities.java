package org.example.ATS_source.util;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class Utilities {

    protected final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(20))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    protected abstract Set<String> discoverFromWayback() throws IOException, InterruptedException;

    protected Optional<String> extractToken(String url, Pattern JOB_URL_PATTERN){
        try {

            URI uri = URI.create(url);

            String path = uri.getPath();

            Matcher matcher = JOB_URL_PATTERN.matcher(path);

            if (!matcher.matches()) {
                return Optional.empty();
            }

            String token = matcher.group(1);

            if (token.isBlank()) {
                return Optional.empty();
            }

            return Optional.of(token.toLowerCase());

        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    protected abstract boolean isCurrentlyValid(String token);

    protected Set<String> loadExistingTokens(Path OUTPUT) throws IOException {

        if (!Files.exists(OUTPUT)) {
            return new HashSet<>();
        }

        Set<String> tokens = new HashSet<>();

        for (String line : Files.readAllLines(OUTPUT)) {

            line = line.trim();

            if (!line.isEmpty()) {
                tokens.add(line);
            }
        }

        return tokens;
    }

    protected synchronized void appendToken(
            String token,
            Path output
    ) throws IOException {

        if (output.getParent() != null) {
            Files.createDirectories(
                    output.getParent()
            );
        }

        Files.writeString(
                output,
                token + System.lineSeparator(),
                StandardOpenOption.CREATE,
                StandardOpenOption.APPEND
        );
    }

    protected String encode(String value) {

        return URLEncoder.encode(
                value,
                StandardCharsets.UTF_8
        );
    }

    protected String encodePath(String value) {

        return value.replace(" ", "%20");
    }


    protected synchronized void rewriteTokens(Set<String> tokens, Path output) throws IOException {

        if (output.getParent() != null) {
            Files.createDirectories(
                    output.getParent()
            );
        }

        Files.write(
                output,
                tokens.stream()
                        .sorted()
                        .toList(),
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE
        );
    }
}
