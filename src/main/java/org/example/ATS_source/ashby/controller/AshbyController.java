package org.example.ATS_source.ashby.controller;

import lombok.RequiredArgsConstructor;
import org.example.ATS_source.ashby.boardTokens.AshbyTools;
import org.example.ATS_source.ashby.service.AshbyJobService;
import org.example.ATS_source.model.JobPosting;
import org.example.ATS_source.ranking.JobRanker;
import org.example.ATS_source.ranking.JobRole;
import org.example.ATS_source.ranking.RankedJob;
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
}