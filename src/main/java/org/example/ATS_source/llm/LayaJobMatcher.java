package org.example.ATS_source.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.example.ATS_source.model.JobPosting;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

@Service
public class LayaJobMatcher {

    private static final String LAYA_URL = "http://localhost:8000/v1/systemone";


    private final ThreadLocal<HttpClient> httpClient;
    private final ObjectMapper objectMapper;

    public LayaJobMatcher(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.httpClient = ThreadLocal.withInitial(() ->
                HttpClient.newBuilder()
                        .version(HttpClient.Version.HTTP_1_1)
                        .build()
        );    }

    public JobMatchResult evaluate(JobPosting job, String candidateProfile, double MATCH_THRESHOLD) {

        if (job.description() == null || job.description().isBlank()) {

            return new JobMatchResult(0.0, false);
        }

        try {

            ObjectNode root = objectMapper.createObjectNode();

            ObjectNode state = root.putObject("state");

            state.put(
                    "candidateProfile",
                    candidateProfile
            );

            state.put(
                    "jobDescription",
                    job.description()
            );

            ObjectNode match =
                    root.putObject("questions")
                            .putObject("match");

            match.put(
                    "type",
                    "noul"
            );

            match.put(
                    "instructions",
                    "Is this candidate realistically a good enough match for this job to apply?"
            );



            String body = objectMapper.writeValueAsString(root);

            HttpRequest request =
                    HttpRequest.newBuilder()
                            .uri(URI.create(LAYA_URL))
                            .header(
                                    "Content-Type",
                                    "application/json"
                            )
                            .POST(
                                    HttpRequest.BodyPublishers
                                            .ofString(body)
                            )
                            .build();


            HttpResponse<String> response =
                    httpClient.get().send(
                            request,
                            HttpResponse.BodyHandlers.ofString()
                    );

            if (response.statusCode() != 200) {
                throw new RuntimeException(
                        "Laya returned HTTP "
                                + response.statusCode()
                                + ": "
                                + response.body()
                );
            }


            JsonNode json =
                    objectMapper.readTree(
                            response.body()
                    );

            double probability =
                    json.path("answers")
                            .path("match")
                            .path("noul")
                            .asDouble();


            return new JobMatchResult(
                    probability,
                    probability >= MATCH_THRESHOLD
            );

        } catch (Exception e) {

            throw new RuntimeException(
                    "Failed to evaluate job: "
                            + job.title(),
                    e
            );
        }
    }
}