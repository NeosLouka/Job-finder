package org.example.ATS_source.llm;

import lombok.RequiredArgsConstructor;
import org.example.ATS_source.search.JobSearchRequest;
import org.example.ATS_source.greenhouse.service.GreenhouseJobService;
import org.example.ATS_source.model.JobPosting;
import org.example.candidate.CandidateProfileReader;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class LayaService {

    private final GreenhouseJobService greenhouseJobService;
    private final LayaJobMatcher layaJobMatcher;
    private final CandidateProfileReader candidateProfileReader;

    private static final int LAYA_THREADS = 4;

    private static final double DEFAULT_MATCH_THRESHOLD = 0.65;


    public List<JobPosting> evaluateAll(JobSearchRequest request, double matchPercentage) {

        double threshold = validateThreshold(matchPercentage);

        List<JobPosting> jobs = greenhouseJobService.searchAllBoardTokens(request);

        return evaluateConcurrently(jobs, threshold);
    }


    public List<JobPosting> evaluate(String boardToken, JobSearchRequest request, double matchPercentage) {

        double threshold = validateThreshold(matchPercentage);

        List<JobPosting> jobs = greenhouseJobService
                .findRequestRelatedJobs(
                        boardToken,
                        request
                );

        return evaluateConcurrently(jobs, threshold);
    }


    public List<JobPosting> evaluateCompany(String companyName, JobSearchRequest request, double matchPercentage) {

        double threshold = validateThreshold(matchPercentage);

        List<JobPosting> jobs = greenhouseJobService
                .findRequestRelatedJobsByCompany(
                        companyName,
                        request
                );

        return evaluateConcurrently(jobs, threshold);
    }


    private double validateThreshold(double matchPercentage) {

        if (matchPercentage < 0 || matchPercentage > 100) {

            return DEFAULT_MATCH_THRESHOLD;
        }

        return matchPercentage / 100.0;
    }


    private List<JobPosting> evaluateConcurrently(List<JobPosting> jobs, double matchThreshold) {

        if (jobs.isEmpty()) {
            return List.of();
        }

        String candidateProfile = candidateProfileReader.readProfile();

        List<JobPosting> matchedJobs = Collections.synchronizedList(new ArrayList<>());

        ExecutorService executor = Executors.newFixedThreadPool(LAYA_THREADS);


        for (JobPosting job : jobs) {

            executor.submit(() -> {

                try {

                    JobMatchResult result =
                            layaJobMatcher.evaluate(
                                    job,
                                    candidateProfile,
                                    matchThreshold
                            );

                    System.out.println(job.title() + " -> " + result.probability());

                    if (result.match()) {
                        matchedJobs.add(job);
                    }

                } catch (Exception e) {

                    System.err.println("Failed to evaluate: " + job.title());
                    e.printStackTrace();
                }
            });
        }


        executor.shutdown();

        try {

            executor.awaitTermination(30, TimeUnit.MINUTES);

        } catch (InterruptedException e) {

            Thread.currentThread().interrupt();

            throw new RuntimeException("Laya evaluation interrupted", e);
        }


        return matchedJobs;
    }


    public List<JobPosting> evaluateJobs(List<JobPosting> jobs, double matchPercentage) {

        double threshold = validateThreshold(matchPercentage);

        return evaluateConcurrently(
                jobs,
                threshold
        );
    }
}