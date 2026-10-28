package org.example.ATS_source.greenhouse.controller;

import lombok.RequiredArgsConstructor;
import org.example.ATS_source.search.JobSearchRequest;
import org.example.ATS_source.greenhouse.service.GreenhouseJobService;
import org.example.ATS_source.llm.LayaService;
import org.example.ATS_source.model.JobPosting;
import org.springframework.web.bind.annotation.*;

import java.util.*;

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

    @GetMapping("/locations")
    public Map<String, List<String>> getLocationFormats() {

        JobSearchRequest request =
                new JobSearchRequest(
                        List.of(),
                        List.of(),
                        List.of(),
                        null,
                        null,
                        false
                );

        List<JobPosting> jobs = greenhouseJobService.searchAllBoardTokens(request);


        Set<String> jobLocations = new TreeSet<>();

        Set<String> officeNames = new TreeSet<>();

        Set<String> officeLocations = new TreeSet<>();


        for (JobPosting job : jobs) {

            if (job.location() != null && !job.location().isBlank()) {

                jobLocations.add(
                        job.location()
                );
            }


            if (job.offices() != null) {

                for (JobPosting.Office office : job.offices()) {

                    if (office.name() != null && !office.name().isBlank()) {

                        officeNames.add(
                                office.name()
                        );
                    }


                    if (office.location() != null && !office.location().isBlank()) {

                        officeLocations.add(
                                office.location()
                        );
                    }
                }
            }
        }


        Map<String, List<String>> result =
                new LinkedHashMap<>();

        result.put(
                "jobLocations",
                new ArrayList<>(jobLocations)
        );

        result.put(
                "officeNames",
                new ArrayList<>(officeNames)
        );

        result.put(
                "officeLocations",
                new ArrayList<>(officeLocations)
        );

        return result;
    }
}