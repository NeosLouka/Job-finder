package org.example.ATS_source.ranking;

import org.example.ATS_source.model.JobPosting;

public record RankedJobResult(
        JobPosting job,
        int score,
        ExperienceRequirement experience,
        boolean explored
) {
}