package org.example.ATS_source.explored.controller;

import lombok.RequiredArgsConstructor;
import org.example.ATS_source.explored.repository.ExploredJobsRepository;
import org.example.ATS_source.model.JobPosting;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/explored")
@RequiredArgsConstructor
public class ExploredJobsController {

    private final ExploredJobsRepository exploredJobsRepository;

    @PostMapping
    public void markExplored(
            @RequestBody JobPosting job
    ) {
        exploredJobsRepository.markExplored(job);
    }
}