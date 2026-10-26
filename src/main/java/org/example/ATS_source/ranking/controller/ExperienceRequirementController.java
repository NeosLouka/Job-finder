package org.example.ATS_source.ranking.controller;

import lombok.RequiredArgsConstructor;
import org.example.ATS_source.ranking.ExperienceRequirement;
import org.example.ATS_source.ranking.ExperienceRequirementAnalyzer;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/experience")
@RequiredArgsConstructor
public class ExperienceRequirementController {

    private final ExperienceRequirementAnalyzer
            experienceRequirementAnalyzer;


    @PostMapping("/analyze")
    public ExperienceRequirement analyze(
            @RequestBody String description
    ) {

        return experienceRequirementAnalyzer
                .analyze(description);
    }
}