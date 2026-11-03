package org.example.ATS_source.greenhouse.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.ATS_source.greenhouse.boardTokens.GreenhouseTools;
import org.example.ATS_source.greenhouse.dto.GreenhouseJobsResponse;
import org.example.ATS_source.model.AtsProvider;
import org.example.ATS_source.ranking.JobRanker;
import org.example.ATS_source.ranking.JobRole;
import org.example.ATS_source.ranking.RankedJob;
import org.example.ATS_source.search.JobFilter;
import org.example.ATS_source.search.JobSearchRequest;
import org.example.ATS_source.model.JobPosting;
import org.example.ATS_source.util.HttpRetryPolicy;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;

@Service
public class GreenhouseJobService {

    private final GreenhouseTools greenhouseTools;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    private final JobFilter jobFilter;
    private final JobRanker jobRanker;

    public GreenhouseJobService(
            ObjectMapper objectMapper,
            GreenhouseTools greenhouseTools,
            JobFilter jobFilter,
            JobRanker jobRanker
    ) {

        this.greenhouseTools = greenhouseTools;
        this.objectMapper = objectMapper;

        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();

        this.jobFilter = jobFilter;
        this.jobRanker = jobRanker;
    }


    public List<RankedJob> filterAndRankAllJobs(
            JobSearchRequest request,
            JobRole role,
            Integer minYears,
            Integer maxYears
    ) {

        List<String> boardTokens;

        try {
            boardTokens = greenhouseTools.retrieveExistingTokens();
        } catch (IOException e) {

            throw new RuntimeException("Failed to retrieve Greenhouse board tokens", e);
        }


        List<JobPosting> jobs =
                Collections.synchronizedList(
                        new ArrayList<>()
                );


        Semaphore semaphore = new Semaphore(30);

        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {

            for (String boardToken : boardTokens) {

                executor.submit(() -> {

                    boolean acquired = false;
                    acquired = true;


                    try {

                        semaphore.acquire();
                        acquired = true;


                        jobs.addAll(
                                findRequestRelatedJobs(
                                        boardToken,
                                        request
                                )
                        );

                    } catch (Exception e) {

                        System.err.println(
                                "Failed to fetch jobs for "
                                        + boardToken
                                        + ": "
                                        + e.getMessage()
                        );

                    } finally {

                        if (acquired) {
                            semaphore.release();
                        }                    }
                });
            }
        }


        return jobRanker.rankJobs(
                jobs,
                role,
                minYears,
                maxYears
        );
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

        List<JobPosting> jobs =
                fetchCompanyJobs(
                        boardToken
                );


        return jobs.stream()
                .filter(job ->
                        jobFilter.matches(
                                job,
                                request
                        )
                )
                .toList();
    }


    /*
     * Actual HTTP call to Greenhouse.
     */
    public List<JobPosting> fetchCompanyJobs(
            String boardToken
    ) {

        String url =
                "https://boards-api.greenhouse.io/v1/boards/"
                        + boardToken
                        + "/jobs?content=true";


        HttpRequest request =
                HttpRequest.newBuilder()
                        .uri(
                                URI.create(url)
                        )
                        .timeout(
                                Duration.ofSeconds(30)
                        )
                        .header(
                                "Accept",
                                "application/json"
                        )
                        .GET()
                        .build();


        try {

            HttpResponse<String> response =
                    httpClient.send(
                            request,
                            HttpResponse.BodyHandlers.ofString()
                    );


            if (response.statusCode() == 404) {
                return List.of();
            }


            if (response.statusCode() == 429) {

                response =
                        HttpRetryPolicy.retryPolicy(
                                httpClient,
                                request,
                                AtsProvider.GREENHOUSE.name()
                                        + boardToken
                        );
            }


            if (response.statusCode() != 200) {

                throw new IllegalStateException(
                        "Greenhouse returned HTTP "
                                + response.statusCode()
                                + " for "
                                + boardToken
                );
            }


            GreenhouseJobsResponse greenhouseResponse =
                    objectMapper.readValue(
                            response.body(),
                            GreenhouseJobsResponse.class
                    );


            if (greenhouseResponse.jobs() == null) {
                return List.of();
            }


            return greenhouseResponse.jobs()
                    .stream()
                    .map(job ->
                            job.toJobPosting(
                                    boardToken
                            )
                    )
                    .toList();


        } catch (IOException e) {

            throw new RuntimeException(
                    "Failed to fetch Greenhouse jobs for "
                            + boardToken
                            + " | "
                            + e.getClass().getSimpleName()
                            + ": "
                            + e.getMessage(),
                    e
            );

        } catch (InterruptedException e) {

            Thread.currentThread()
                    .interrupt();

            throw new RuntimeException(
                    "Greenhouse request interrupted",
                    e
            );
        }
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