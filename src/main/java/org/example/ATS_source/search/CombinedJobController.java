package org.example.ATS_source.search;

import org.example.ATS_source.llm.LayaService;
import org.example.ATS_source.ranking.JobRole;
import org.example.ATS_source.ranking.RankedJob;
import org.example.ATS_source.ranking.RankedJobResult;
import org.example.ATS_source.ranking.RankedJobsResponse;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class CombinedJobController {

    private final CombinedJobService combinedJobService;
    private final LayaService layaService;

    public CombinedJobController(
            CombinedJobService combinedJobService,
            LayaService layaService
    ) {
        this.combinedJobService = combinedJobService;
        this.layaService = layaService;
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
                combinedJobService
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