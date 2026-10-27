package org.example.ATS_source.ranking.controller;

import lombok.RequiredArgsConstructor;
import org.example.ATS_source.explored.repository.ExploredJobsRepository;
import org.example.ATS_source.greenhouse.dto.JobSearchRequest;
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
     * Rank jobs from a specific company.
     * Optionally evaluate the top ranked jobs with Laya.
     */
    @PostMapping("/companies/{companyName}")
    public List<?> rankCompanyJobs(
            @PathVariable String companyName,

            @RequestParam(defaultValue = "BACKEND")
            JobRole role,

            @RequestParam(required = false)
            Integer minYears,

            @RequestParam(required = false)
            Integer maxYears,

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
         * STEP 1:
         * Get matching jobs from all Greenhouse boards
         * belonging to this company.
         */
        List<JobPosting> jobs = greenhouseJobService.findRequestRelatedJobsByCompany( companyName, request);

        /*
         * STEP 2:
         * Filter by experience and rank by role relevance.
         */
        List<RankedJob> rankedJobs = jobRanker
                .rankJobs(
                        jobs,
                        role,
                        minYears,
                        maxYears
                );


        /*
         * STEP 3:
         * No Laya requested.
         */
        if (!useLaya) {
            return rankedJobs;
        }


        /*
         * STEP 4:
         * Take only the top ranked jobs for Laya.
         */
        List<JobPosting> jobsForLaya =
                rankedJobs
                        .stream()
                        .limit(layaLimit)
                        .map(RankedJob::job)
                        .toList();


        /*
         * STEP 5:
         * Candidate-specific evaluation.
         */
        return layaService.evaluateJobs(
                jobsForLaya,
                matchPercentage
        );
    }



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

        List<JobPosting> jobs =
                greenhouseJobService.searchAllBoardTokens(request);


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
                                                    rankedJob.experience(),
                                                    exploredJobsRepository.isExplored(
                                                            rankedJob.job()
                                                    )
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


        List<JobPosting> jobsForLaya =
                rankedJobs
                        .stream()
                        .limit(layaLimit)
                        .map(RankedJob::job)
                        .toList();


        List<JobPosting> layaResults =
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