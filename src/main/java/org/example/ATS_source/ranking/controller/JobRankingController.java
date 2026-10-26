package org.example.ATS_source.ranking.controller;

import lombok.RequiredArgsConstructor;
import org.example.ATS_source.greenhouse.dto.JobSearchRequest;
import org.example.ATS_source.greenhouse.service.GreenhouseJobService;
import org.example.ATS_source.llm.LayaService;
import org.example.ATS_source.model.JobPosting;
import org.example.ATS_source.ranking.JobRanker;
import org.example.ATS_source.ranking.JobRole;
import org.example.ATS_source.ranking.RankedJob;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/ranking")
@RequiredArgsConstructor
public class JobRankingController {

    private final GreenhouseJobService greenhouseJobService;
    private final JobRanker jobRanker;
    private final LayaService layaService;

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
    public List<?> rankAllJobs(

            @RequestParam(defaultValue = "BACKEND")
            JobRole role,

            @RequestParam(required = false)
            Integer minYears,

            @RequestParam(required = false)
            Integer maxYears,

            /*
             * Number of ranked jobs to return
             * when Laya is NOT used.
             */
            @RequestParam(defaultValue = "50")
            int limit,

            @RequestParam(defaultValue = "false")
            boolean useLaya,

            /*
             * Number of top jobs to send to Laya
             * when Laya IS used.
             */
            @RequestParam(defaultValue = "100")
            int layaLimit,

            @RequestParam(defaultValue = "65")
            double matchPercentage,

            @RequestBody
            JobSearchRequest request
    ) {

        /*
         * STEP 1:
         * Greenhouse title/location/etc filtering.
         */
        List<JobPosting> jobs = greenhouseJobService.searchAllBoardTokens(request);

        /*
         * STEP 2:
         * Experience filtering +
         * backend relevance ranking.
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
         * User doesn't want Laya.
         */
        if (!useLaya) {
            return rankedJobs
                    .stream()
                    .limit(limit)
                    .toList();
        }


        /*
         * STEP 4:
         * Take only the best ranked jobs
         * and evaluate those with Laya.
         */
        List<JobPosting> jobsForLaya = rankedJobs
                .stream()
                .limit(layaLimit)
                .map(RankedJob::job)
                .toList();


        return layaService.evaluateJobs(
                jobsForLaya,
                matchPercentage
        );
    }
}