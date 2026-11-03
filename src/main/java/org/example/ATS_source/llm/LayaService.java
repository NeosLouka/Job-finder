package org.example.ATS_source.llm;

import lombok.RequiredArgsConstructor;
import org.example.ATS_source.ranking.RankedJob;
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

    private final LayaJobMatcher layaJobMatcher;
    private final CandidateProfileReader candidateProfileReader;

    private static final int LAYA_THREADS = 4;

    private static final double DEFAULT_MATCH_THRESHOLD = 0.65;


    public List<RankedJob> evaluateJobs(List<RankedJob> jobs, double matchPercentage) {

        double threshold = validateThreshold(matchPercentage);

        if (jobs.isEmpty()) {
            return List.of();
        }

        String candidateProfile = candidateProfileReader.readProfile();

        List<RankedJob> matchedJobs = Collections.synchronizedList(new ArrayList<>());


        try (ExecutorService executor = Executors.newFixedThreadPool(LAYA_THREADS)) {

            for (RankedJob rankedJob : jobs) {

                executor.submit(() -> {

                    try {

                        JobMatchResult result =
                                layaJobMatcher.evaluate(
                                        rankedJob.job(),
                                        candidateProfile,
                                        threshold
                                );


                        System.out.println(
                                rankedJob.job().title()
                                        + " -> "
                                        + result.probability()
                        );


                        if (result.match()) {

                            matchedJobs.add(
                                    rankedJob
                            );
                        }

                    } catch (Exception e) {

                        System.err.println(
                                "Failed to evaluate: "
                                        + rankedJob.job().title()
                        );
                    }
                });
            }
        }


        return matchedJobs;
    }


    private double validateThreshold(double matchPercentage) {

        if (matchPercentage < 0 || matchPercentage > 100) {

            return DEFAULT_MATCH_THRESHOLD;
        }

        return matchPercentage / 100.0;
    }
}