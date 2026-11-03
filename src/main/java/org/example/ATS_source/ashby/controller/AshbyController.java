package org.example.ATS_source.ashby.controller;

import lombok.RequiredArgsConstructor;
import org.example.ATS_source.ashby.boardTokens.AshbyTools;
import org.example.ATS_source.ashby.service.AshbyJobService;
import org.example.ATS_source.llm.LayaService;
import org.example.ATS_source.model.JobPosting;
import org.example.ATS_source.ranking.*;
import org.example.ATS_source.search.JobSearchRequest;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/v1/ashby")
@RequiredArgsConstructor
public class AshbyController {

    private final AshbyTools ashbyTools;
    private final AshbyJobService ashbyJobService;
    private final JobRanker jobRanker;

    private final LayaService layaService;



    @PostMapping("/search")
    public List<RankedJob> filterAndRankAllJobs(
            @RequestBody JobSearchRequest request,
            @RequestParam JobRole role,
            @RequestParam(required = false) Integer minYears,
            @RequestParam(required = false) Integer maxYears
    ) throws IOException {

        return ashbyJobService.filterAndRankAllJobs(
                request,
                role,
                minYears,
                maxYears
        );
    }


    @PostMapping("/boards/discover")
    public Set<String> discoverCandidates() throws IOException, InterruptedException {

        return ashbyTools.discoverCandidates();
    }


    @PostMapping("/boards/validate")
    public Set<String> validateBoards() throws IOException {

        return ashbyTools.validateBoardNames();
    }

    @PostMapping("/boards/{boardName}/rank")
    public List<RankedJob> rankBoardJobs(
            @PathVariable String boardName,
            @RequestBody JobSearchRequest request,
            @RequestParam JobRole role,
            @RequestParam(required = false) Integer minYears,
            @RequestParam(required = false) Integer maxYears
    ) {

        List<JobPosting> jobs =
                ashbyJobService.findRequestRelatedJobs(
                        boardName,
                        request
                );


        return jobRanker.rankJobs(
                jobs,
                role,
                minYears,
                maxYears
        );
    }







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

        List<RankedJob> rankedJobs =
                ashbyJobService
                        .filterAndRankAllJobs(
                                request,
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
         * No Laya evaluation.
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
         * Only send top ranked jobs to Laya.
         */
        List<RankedJob> jobsForLaya =
                rankedJobs
                        .stream()
                        .limit(layaLimit)
                        .toList();


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