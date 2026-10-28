package org.example.ATS_source.search;

import lombok.RequiredArgsConstructor;
import org.example.ATS_source.model.JobPosting;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class JobFilter {

    private final TitleFilter titleFilter;
    private final LocationFilter locationFilter;

    /**
     * Matches based on filters :
     * - Title,
     * - Location,
     * - Language,
     * - Last updated
     * @param job
     * @param request
     * @return
     */
    public boolean matches(JobPosting job, JobSearchRequest request) {

        if (!titleFilter.matches(job, request)) {
            return false;
        }

        if (!locationFilter.matches(job, request)) {
            return false;
        }

        //Language filter
        if (request.language() != null && !request.language().isBlank()) {

            if (job.language() == null || !job.language().equalsIgnoreCase(request.language())) {
                return false;
            }
        }

        //Last updated filter
        if (request.lastUpdated() != null) {

            if (job.updatedAt() == null || job.updatedAt().isBefore(request.lastUpdated())) {
                return false;
            }
        }

        return true;
    }
}