package org.example.ATS_source.ranking.controller;

import lombok.RequiredArgsConstructor;
import org.example.ATS_source.explored.repository.ExploredJobsRepository;
import org.example.ATS_source.search.JobSearchRequest;
import org.example.ATS_source.greenhouse.service.GreenhouseJobService;
import org.example.ATS_source.llm.LayaService;
import org.example.ATS_source.model.JobPosting;
import org.example.ATS_source.ranking.*;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/ranking")
@RequiredArgsConstructor
public class JobRankingController {

    private final GreenhouseJobService greenhouseJobService;
    private final JobRanker jobRanker;
    private final LayaService layaService;
    private final ExploredJobsRepository exploredJobsRepository;


    /*
     * Rank matching jobs from all Greenhouse boards.
     */
    @PostMapping("/jobs")
    public RankedJobsResponse<?> rankAllJobs(

            @RequestParam(defaultValue = "BACKEND")
            JobRole role,

            @RequestParam(required = false)
            Integer minYears,

            @RequestParam(required = false)
            Integer maxYears,

            @RequestParam(defaultValue = "25")
            int minScore,

            @RequestParam(defaultValue = "50")
            int limit,

            @RequestParam(defaultValue = "false")
            boolean useLaya,

            @RequestParam(defaultValue = "100")
            int layaLimit,

            @RequestParam(defaultValue = "65")
            double matchPercentage,

            @RequestBody
            JobSearchRequest request
    ) {

        /*
         * STEP 1
         * Fetch jobs and apply JobFilter.
         */
        List<JobPosting> jobs = greenhouseJobService.searchAllBoardTokens(request);


        /*
         * STEP 2
         * Experience filtering + ranking.
         */
        List<RankedJob> rankedJobs =
                jobRanker
                        .rankJobs(
                                jobs,
                                role,
                                minYears,
                                maxYears
                        )
                        .stream()
                        .filter(
                                rankedJob ->
                                        rankedJob.score() >= minScore
                        )
                        .toList();


        /*
         * STEP 3
         * If Laya is not requested,
         * return the ranked results normally.
         */
        if (!useLaya) {

            List<RankedJobResult> result =
                    rankedJobs
                            .stream()
                            .limit(limit)
                            .map(
                                    rankedJob ->
                                            new RankedJobResult(
                                                    rankedJob.job(),
                                                    rankedJob.score(),
                                                    rankedJob.experience()
                                            )
                            )
                            .toList();


            return new RankedJobsResponse<>(
                    result.size(),
                    rankedJobs.size(),
                    limit,
                    rankedJobs.size() > limit,
                    result
            );
        }


        /*
         * STEP 4
         * Only send the best ranked jobs to Laya.
         */
        List<RankedJob> jobsForLaya =
                rankedJobs
                        .stream()
                        .limit(layaLimit)
                        .toList();


        /*
         * STEP 5
         * Candidate-specific evaluation.
         */
        List<RankedJob> layaResults =
                layaService.evaluateJobs(
                        jobsForLaya,
                        matchPercentage
                );


        return new RankedJobsResponse<>(
                layaResults.size(),
                rankedJobs.size(),
                layaLimit,
                rankedJobs.size() > layaLimit,
                layaResults
        );
    }

}