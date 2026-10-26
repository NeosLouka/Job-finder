package org.example.ATS_source.ranking;

import org.example.ATS_source.model.JobPosting;

public record RankedJob(
        JobPosting job,
        int score,
        ExperienceRequirement experience

) {
}