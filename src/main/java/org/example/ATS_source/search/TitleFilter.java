package org.example.ATS_source.search;

import org.example.ATS_source.model.JobPosting;
import org.springframework.stereotype.Component;

@Component
public class TitleFilter {

    public boolean matches(JobPosting job, JobSearchRequest request) {

        if (job.title() == null || job.title().isBlank()) {
            return false;
        }

        String title = job.title().toLowerCase();

        if (request.excludedTitleTerms() != null && request.excludedTitleTerms().stream().anyMatch(term -> title.contains(term.toLowerCase()))
        ) {
            return false;
        }


        if (request.titles() != null && !request.titles().isEmpty()) {

            return request.titles()
                    .stream()
                    .anyMatch(term ->
                            title.contains(
                                    term.toLowerCase()
                            )
                    );
        }


        return true;
    }
}