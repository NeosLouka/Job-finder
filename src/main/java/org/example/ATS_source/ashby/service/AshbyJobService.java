package org.example.ATS_source.ashby.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.ATS_source.ashby.boardTokens.AshbyTools;
import org.example.ATS_source.ashby.dto.AshbyJobDto;
import org.example.ATS_source.ashby.dto.AshbyJobsResponse;
import org.example.ATS_source.model.AtsProvider;
import org.example.ATS_source.model.JobPosting;
import org.example.ATS_source.ranking.JobRanker;
import org.example.ATS_source.ranking.JobRole;
import org.example.ATS_source.ranking.RankedJob;
import org.example.ATS_source.search.JobFilter;
import org.example.ATS_source.search.JobSearchRequest;
import org.example.ATS_source.util.HttpRetryPolicy;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;

@Service
public class AshbyJobService {

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final JobFilter jobFilter;

    private final AshbyTools ashbyTools;
    private final JobRanker jobRanker;



    public AshbyJobService(ObjectMapper objectMapper, JobFilter jobFilter, AshbyTools ashbyTools, JobRanker jobRanker) {

        this.objectMapper = objectMapper;

        this.httpClient =
                HttpClient.newBuilder()
                        .followRedirects(
                                HttpClient.Redirect.NORMAL
                        )
                        .build();

        this.jobFilter = jobFilter;
        this.ashbyTools = ashbyTools;
        this.jobRanker = jobRanker;
    }


    public List<RankedJob> filterAndRankAllJobs(
            JobSearchRequest request,
            JobRole role,
            Integer minYears,
            Integer maxYears
    ) throws IOException {

        Set<String> boardNames = ashbyTools.retrieveExistingBoardNames();

        List<JobPosting> jobs =
                Collections.synchronizedList(
                        new ArrayList<>()
                );

        Semaphore semaphore = new Semaphore(30);

        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {

            for (String boardName : boardNames) {

                executor.submit(() -> {

                    try {

                        semaphore.acquire();

                        jobs.addAll(
                                findRequestRelatedJobs(
                                        boardName,
                                        request
                                )
                        );

                    } catch (Exception e) {

                        System.err.println(
                                "Failed to fetch jobs for "
                                        + boardName
                                        + ": "
                                        + e.getMessage()
                        );
                    } finally {

                        semaphore.release();
                    }
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




    public List<JobPosting> findRequestRelatedJobs(String boardName, JobSearchRequest request) {

        List<JobPosting> jobs = fetchCompanyJobs(boardName);

        return jobs.stream()
                .filter(job ->
                        jobFilter.matches(
                                job,
                                request
                        )
                )
                .toList();
    }


    public List<JobPosting> fetchCompanyJobs(String boardName) {

        String url = "https://api.ashbyhq.com/posting-api/job-board/" + boardName;


        HttpRequest request =
                HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .header("Accept", "application/json")
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

                response = HttpRetryPolicy.retryPolicy(
                        httpClient,
                        request,
                        AtsProvider.ASHBY.name() + boardName
                );
            }

            if (response.statusCode() != 200) {

                throw new IllegalStateException(
                        "Ashby returned HTTP "
                                + response.statusCode()
                                + " for "
                                + boardName
                );
            }


            AshbyJobsResponse ashbyResponse =
                    objectMapper.readValue(
                            response.body(),
                            AshbyJobsResponse.class
                    );


            if (ashbyResponse.jobs() == null) {
                return List.of();
            }


            return ashbyResponse.jobs()
                    .stream()

                    // Don't include hidden/direct-link jobs
                    .filter(job ->
                            !Boolean.FALSE.equals(
                                    job.isListed()
                            )
                    )

                    .map(job ->
                            toJobPosting(
                                    boardName,
                                    job
                            )
                    )

                    .toList();


        } catch (IOException e) {

            throw new RuntimeException(
                    "Failed to fetch Ashby jobs for "
                            + boardName
                            + " | "
                            + e.getClass().getSimpleName()
                            + ": "
                            + e.getMessage(),
                    e
            );

        } catch (InterruptedException e) {

            Thread.currentThread().interrupt();

            throw new RuntimeException(
                    "Ashby request interrupted",
                    e
            );
        }
    }


    private JobPosting toJobPosting(
            String boardName,
            AshbyJobDto job
    ) {

        return new JobPosting(

                extractExternalId(
                        job.jobUrl()
                ),

                AtsProvider.ASHBY,

                boardName,

                job.title(),

                boardName,

                job.location(),

                null,

                job.jobUrl(),

                parsePublishedAt(
                        job.publishedAt()
                ),

                job.descriptionHtml(),

                buildOffices(job)
        );
    }


    private List<JobPosting.Office> buildOffices(
            AshbyJobDto job
    ) {

        List<JobPosting.Office> offices =
                new ArrayList<>();


        if (
                job.location() != null
                        && !job.location().isBlank()
        ) {

            offices.add(
                    new JobPosting.Office(
                            job.location(),
                            job.location()
                    )
            );
        }


        if (job.secondaryLocations() != null) {

            for (
                    AshbyJobDto.SecondaryLocation secondary :
                    job.secondaryLocations()
            ) {

                if (
                        secondary.location() != null
                                && !secondary.location().isBlank()
                ) {

                    offices.add(
                            new JobPosting.Office(
                                    secondary.location(),
                                    secondary.location()
                            )
                    );
                }
            }
        }


        return offices;
    }


    private OffsetDateTime parsePublishedAt(
            String publishedAt
    ) {

        if (
                publishedAt == null
                        || publishedAt.isBlank()
        ) {
            return null;
        }


        try {

            return OffsetDateTime.parse(
                    publishedAt
            );

        } catch (Exception e) {

            return null;
        }
    }


    private String extractExternalId(
            String jobUrl
    ) {

        if (
                jobUrl == null
                        || jobUrl.isBlank()
        ) {
            return null;
        }


        try {

            String path =
                    URI.create(jobUrl)
                            .getPath();

            String[] parts =
                    path.split("/");


            for (
                    int i = parts.length - 1;
                    i >= 0;
                    i--
            ) {

                if (!parts[i].isBlank()) {
                    return parts[i];
                }
            }

        } catch (Exception ignored) {
        }


        return jobUrl;
    }
}