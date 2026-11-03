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

            root.put("max_len", 4096);
            root.put("head_max_len", 256);

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
                    "choice"
            );

            match.put(
                    "instructions",
                    """
                    Evaluate whether this candidate is realistically suitable
                    enough to apply for this job.
            
                    Consider:
                    - required years of experience
                    - seniority
                    - backend/software engineering relevance
                    - mandatory technical requirements
                    - transferable skills
            
                    Do not consider the candidate a good match if the role
                    clearly requires substantially more professional experience
                    or specialized expertise than the candidate has.
                    """
            );

            ObjectNode criteria =
                    match.putObject("criteria");

            criteria.put(
                    "A",
                    """
                    Realistic match. The candidate has appropriate or reasonably
                    close seniority and enough relevant or transferable skills
                    that applying for this position makes sense.
                    """
            );

            criteria.put(
                    "B",
                    """
                    Poor match. The position requires substantially greater
                    seniority, mandatory professional experience, or specialized
                    expertise that the candidate does not have.
                    """
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
                            .path("probabilities")
                            .path("A")
                            .asDouble();

            JsonNode usage = json.path("usage");

            System.out.println(
                    job.title()
                            + " -> "
                            + probability
                            + " | stateTokens="
                            + usage.path("state_tokens").asInt()
                            + " | dropped="
                            + usage.path("state_tokens_dropped").asInt()
                            + " | truncated="
                            + usage.path("truncated").asBoolean()
            );



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