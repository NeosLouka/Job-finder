package org.example.ATS_source.greenhouse.controller;

import org.example.ATS_source.greenhouse.dto.JobSearchRequest;
import org.example.ATS_source.greenhouse.service.GreenhouseJobService;
import org.example.ATS_source.model.JobPosting;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/greenhouse")
public class GreenhouseController {

    private final GreenhouseJobService greenhouseJobService;

    public GreenhouseController(
            GreenhouseJobService greenhouseJobService
    ) {

        this.greenhouseJobService =
                greenhouseJobService;
    }


    /*
     * Search all known Greenhouse boards.
     */
    @PostMapping("/jobs/search")
    public List<JobPosting> searchJobs(
            @RequestBody JobSearchRequest request
    ) {

        return greenhouseJobService
                .searchAllBoardTokens(request);
    }


    /*
     * Useful while testing:
     * search only one company.
     */
    @PostMapping("/boards/{boardToken}/search")
    public List<JobPosting> searchBoard(
            @PathVariable String boardToken,
            @RequestBody JobSearchRequest request
    ) {

        return greenhouseJobService
                .findRequestRelatedJobs(
                        boardToken,
                        request
                );
    }
}