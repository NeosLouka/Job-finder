package org.example.ATS_source.util;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class HttpRetryPolicy {

    public static HttpResponse<String> retryPolicy(
            HttpClient client,
            HttpRequest request,
            String source
    ) throws IOException, InterruptedException {

        int maxRetries = 10;
        for (int attempt = 1; attempt <= maxRetries; attempt++) {

            long delay = attempt * 5000L;

            System.out.println(
                    source
                            + " rate limited"
                            + " | retry "
                            + attempt
                            + "/"
                            + maxRetries
                            + " in "
                            + delay / 1000
                            + " seconds"
            );

            Thread.sleep(delay);

            HttpResponse<String> response =
                    client.send(
                            request,
                            HttpResponse.BodyHandlers.ofString()
                    );

            if (response.statusCode() != 429) {
                return response;
            }
        }

        throw new IOException(
                source
                        + " still returned 429 after "
                        + maxRetries
                        + " retries"
        );
    }

    private HttpRetryPolicy() {}
}