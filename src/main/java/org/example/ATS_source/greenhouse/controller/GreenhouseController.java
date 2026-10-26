package org.example.ATS_source.greenhouse.controller;

import lombok.RequiredArgsConstructor;
import org.example.ATS_source.greenhouse.dto.JobSearchRequest;
import org.example.ATS_source.greenhouse.service.GreenhouseJobService;
import org.example.ATS_source.llm.LayaService;
import org.example.ATS_source.model.JobPosting;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/v1/greenhouse")
@RequiredArgsConstructor
public class GreenhouseController {

    private final GreenhouseJobService greenhouseJobService;
    private final LayaService layaService;




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

    @GetMapping("/companies")
    public Set<String> retrieveCompanies(){

        return greenhouseJobService.retrieveCompanies();
    }


    @PostMapping("/companies/{companyName}/search")
    public List<JobPosting> evaluateCompany(
            @PathVariable String companyName,
            @RequestParam(defaultValue = "65") double matchPercentage,
            @RequestBody JobSearchRequest request
    ) {

        return layaService.evaluateCompany(
                companyName,
                request,
                matchPercentage
        );
    }
}