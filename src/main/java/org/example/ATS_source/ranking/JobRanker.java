package org.example.ATS_source.ranking;

import lombok.RequiredArgsConstructor;
import org.example.ATS_source.model.JobPosting;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;

@Component
@RequiredArgsConstructor
public class JobRanker {

    private final ExperienceRequirementAnalyzer experienceRequirementAnalyzer;


    public List<RankedJob> rankJobs(List<JobPosting> jobs, JobRole role, Integer minYears, Integer maxYears) {

        Map<String, JobScoringProfiles.ScoredKeyword> scoringProfile = getScoringProfile(role);

        List<RankedJob> rankedJobs =
                Collections.synchronizedList(
                        new ArrayList<>()
                );


        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {

            for (JobPosting job : jobs) {

                executor.submit(() -> {

                    ExperienceRequirement experience =
                            experienceRequirementAnalyzer.analyze(
                                    job.description()
                            );

                    if (!matchesExperienceRange(experience, minYears, maxYears)) {
                        return;
                    }

                    int score = calculateScore(job, scoringProfile);

                    rankedJobs.add(
                            new RankedJob(
                                    job,
                                    score,
                                    experience
                            )
                    );
                });
            }
        }

        rankedJobs.sort(Comparator.comparingInt(RankedJob::score).reversed());

        return rankedJobs;
    }


    private int calculateScore(JobPosting job, Map<String, JobScoringProfiles.ScoredKeyword> scoringProfile) {

        String description = normalize(job.description());

        return calculateTextScore(
                description,
                scoringProfile
        );
    }


    private int calculateTextScore(String text, Map<String, JobScoringProfiles.ScoredKeyword> scoringProfile) {

        int score = 0;
        String remainingText = text;

        for (Map.Entry<String, JobScoringProfiles.ScoredKeyword> entry : scoringProfile.entrySet()) {

            JobScoringProfiles.ScoredKeyword scoredKeyword = entry.getValue();

            Matcher matcher = scoredKeyword.pattern().matcher(remainingText);


            if (matcher.find()) {

                score += scoredKeyword.points();
                remainingText = matcher.replaceAll(" ");
            }
        }

        return score;
    }


    private Map<String, JobScoringProfiles.ScoredKeyword> getScoringProfile(JobRole role) {

        return switch (role) {

            case BACKEND -> JobScoringProfiles.BACKEND_ROLE;

            default -> throw new IllegalArgumentException("Scoring profile not implemented for: " + role);
        };
    }


    private String normalize(String text) {

        if (text == null) {
            return "";
        }


        return text
                .replaceAll(
                        "<[^>]*>",
                        " "
                )
                .replaceAll(
                        "&nbsp;",
                        " "
                )
                .replaceAll(
                        "\\s+",
                        " "
                )
                .toLowerCase(
                        Locale.ROOT
                )
                .trim();
    }


    private boolean matchesExperienceRange(ExperienceRequirement experience, Integer requestedMin, Integer requestedMax
    ) {

        if (requestedMin == null && requestedMax == null) {
            return true;
        }

        if (experience.minYears() == null) {
            return true;
        }

        int jobMin = experience.minYears();

        if (requestedMax != null && jobMin > requestedMax) {
            return false;
        }

        if (requestedMin != null && experience.maxYears() != null && experience.maxYears() < requestedMin) {
            return false;
        }


        return true;
    }
}