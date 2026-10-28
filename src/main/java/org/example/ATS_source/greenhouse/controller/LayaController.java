package org.example.ATS_source.greenhouse.controller;

import lombok.RequiredArgsConstructor;
import org.example.ATS_source.search.JobSearchRequest;
import org.example.ATS_source.llm.LayaService;
import org.example.ATS_source.model.JobPosting;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/laya")
@RequiredArgsConstructor
public class LayaController {

    private final LayaService layaService;


    /*
     * Evaluate matching jobs from all known Greenhouse boards
     */
    @PostMapping("/jobs/search")
    public List<JobPosting> evaluateAll(
            @RequestBody JobSearchRequest request,
            @RequestParam(defaultValue = "65") double matchPercentage

            ) {
        return layaService.evaluateAll(request, matchPercentage);
    }

    /*
     * Evaluate matching jobs from one Greenhouse board
     */
    @PostMapping("/boards/{boardToken}/search")
    public List<JobPosting> evaluateBoard(
            @PathVariable String boardToken,
            @RequestBody JobSearchRequest request,
            @RequestParam(defaultValue = "65") double matchPercentage

    ) {
        return layaService.evaluate(boardToken, request, matchPercentage);
    }
}
