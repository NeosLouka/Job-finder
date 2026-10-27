package org.example.ATS_source.greenhouse.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.ATS_source.greenhouse.boardTokens.GreenhouseTools;
import org.example.ATS_source.greenhouse.dto.GreenhouseJobsResponse;
import org.example.ATS_source.greenhouse.dto.JobSearchRequest;
import org.example.ATS_source.llm.LayaJobMatcher;
import org.example.ATS_source.model.JobPosting;
import org.example.candidate.CandidateProfileReader;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
public class GreenhouseJobService {

    private final GreenhouseTools greenhouseTools;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final EuropeanCountries europeanCountries;

    public GreenhouseJobService(
            ObjectMapper objectMapper,
            GreenhouseTools greenhouseTools,
            LayaJobMatcher layaJobMatcher,
            CandidateProfileReader candidateProfileReader,
            EuropeanCountries europeanCountries
    ) {

        this.greenhouseTools = greenhouseTools;
        this.objectMapper = objectMapper;

        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();

        this.europeanCountries = europeanCountries;
    }

    /*
     * Search every Greenhouse board that we have discovered.
     */
    public List<JobPosting> searchAllBoardTokens(JobSearchRequest request) {

        List<String> tokens;

        try {
            tokens = greenhouseTools.retrieveExistingTokens();
        } catch (IOException e) {
            throw new RuntimeException("Failed to retrieve tokens", e);
        }

        List<JobPosting> results =
                Collections.synchronizedList(new ArrayList<>());

        try (ExecutorService executor =
                     Executors.newVirtualThreadPerTaskExecutor()) {

            for (String token : tokens) {

                executor.submit(() -> {

                    try {

                        List<JobPosting> jobs =
                                findRequestRelatedJobs(token, request);

                        results.addAll(jobs);

                    } catch (Exception e) {

                        System.err.println(
                                "Failed board " + token
                                        + ": " + e.getMessage()
                        );
                    }
                });
            }
        }

        return results;
    }

    /*
     * Search a single Greenhouse board.
     */
    public List<JobPosting> findRequestRelatedJobs(
            String boardToken,
            JobSearchRequest request
    ) {

        if (boardToken == null || boardToken.isBlank()) {

            throw new IllegalArgumentException(
                    "boardToken cannot be null or blank"
            );
        }

        List<JobPosting> results = new ArrayList<>();

        try {

            GreenhouseJobsResponse response = fetchCompanyJobs(boardToken);

            if (response.jobs() == null) {
                return results;
            }

            for (GreenhouseJobsResponse.JobDto jobDto : response.jobs()) {

                JobPosting job = jobDto.toJobPosting(boardToken);

                if (filterJob(job, request)) {
                    results.add(job);
                }
            }

        } catch (IOException e) {

            throw new RuntimeException(
                    "Failed to fetch Greenhouse jobs for "
                            + boardToken,
                    e
            );

        } catch (InterruptedException e) {

            Thread.currentThread().interrupt();

            throw new RuntimeException(
                    "Greenhouse request interrupted for "
                            + boardToken,
                    e
            );
        }

        return results;
    }


    /*
     * Actual HTTP call to Greenhouse.
     */
    private GreenhouseJobsResponse fetchCompanyJobs(String boardToken) throws IOException, InterruptedException {

        String url =
                "https://boards-api.greenhouse.io/v1/boards/"
                        + boardToken
                        + "/jobs?content=true";

        HttpRequest request =
                HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .timeout(Duration.ofSeconds(10))
                        .header(
                                "Accept",
                                "application/json"
                        )
                        .GET()
                        .build();

        HttpResponse<String> response =
                httpClient.send(
                        request,
                        HttpResponse.BodyHandlers.ofString()
                );

        if (response.statusCode() != 200) {

            throw new IllegalStateException(
                    "Greenhouse returned HTTP "
                            + response.statusCode()
                            + " for "
                            + boardToken
            );
        }

        return objectMapper.readValue(
                response.body(),
                GreenhouseJobsResponse.class
        );
    }


    /*
     * Apply filters from JobSearchRequest.
     */
    private boolean filterJob(
            JobPosting job,
            JobSearchRequest request
    ) {

        if (request == null) {
            return true;
        }

        if (request.europeOnly() && !europeanCountries.isEuropeanLocation(job)) {

            return false;
        }

        /*
         * EXCLUDED TITLE TERMS
         */
        if (request.excludedTitleTerms() != null && !request.excludedTitleTerms().isEmpty()) {

            boolean excludedTitle =
                    request.excludedTitleTerms()
                            .stream()
                            .filter(term ->
                                    term != null
                                            && !term.isBlank()
                            )
                            .anyMatch(term ->

                                    job.title() != null
                                            &&
                                            job.title()
                                                    .toLowerCase(Locale.ROOT)
                                                    .contains(
                                                            term.toLowerCase(Locale.ROOT)
                                                    )
                            );

            if (excludedTitle) {
                return false;
            }
        }

        /*
         * TITLE
         */
        if (
                request.titles() != null && !request.titles().isEmpty()
        ) {

            boolean titleMatch =
                    request.titles()
                            .stream()
                            .filter(title ->
                                    title != null
                                            && !title.isBlank()
                            )
                            .anyMatch(title ->

                                    job.title() != null
                                            &&
                                            job.title()
                                                    .toLowerCase(Locale.ROOT)
                                                    .contains(
                                                            title
                                                                    .toLowerCase(Locale.ROOT)
                                                    )
                            );

            if (!titleMatch) {
                return false;
            }
        }


        /*
         * LOCATION
         */
        if (
                request.locations() != null
                        && !request.locations().isEmpty()
        ) {

            boolean locationMatch =
                    request.locations()
                            .stream()
                            .filter(location ->
                                    location != null
                                            && !location.isBlank()
                            )
                            .anyMatch(location ->

                                    job.location() != null
                                            &&
                                            job.location()
                                                    .toLowerCase(Locale.ROOT)
                                                    .contains(
                                                            location
                                                                    .toLowerCase(Locale.ROOT)
                                                    )
                            );

            if (!locationMatch) {
                return false;
            }
        }


        /*
         * LANGUAGE
         */
        if (
                request.language() != null
                        && !request.language().isBlank()
        ) {

            if (
                    job.language() == null
                            ||
                            !job.language()
                                    .equalsIgnoreCase(
                                            request.language()
                                    )
            ) {

                return false;
            }
        }


        /*
         * LAST UPDATED
         */
        OffsetDateTime lastUpdated =
                request.lastUpdated();

        if (lastUpdated != null) {

            if (
                    job.updatedAt() == null
                            ||
                            job.updatedAt()
                                    .isBefore(lastUpdated)
            ) {

                return false;
            }
        }

        return true;
    }

    public Set<String> retrieveCompanies() {
        return greenhouseTools.getCompanies().keySet();
    }

    public List<JobPosting> findRequestRelatedJobsByCompany(
            String companyName,
            JobSearchRequest request
    ) {

        List<String> boardTokens = greenhouseTools
                .getCompanies()
                .get(findCompanyKey(companyName));

        if (boardTokens == null) {
            throw new RuntimeException(
                    "Company not found: " + companyName
            );
        }

        List<JobPosting> jobs = new ArrayList<>();

        for (String boardToken : boardTokens) {

            List<JobPosting> boardJobs = findRequestRelatedJobs( boardToken, request);

            jobs.addAll(boardJobs);
        }

        return jobs;
    }

    private String findCompanyKey(String companyName) {

        for (String company : greenhouseTools.getCompanies().keySet()) {

            if (company.equalsIgnoreCase(companyName)) {
                return company;
            }
        }

        return null;
    }
}