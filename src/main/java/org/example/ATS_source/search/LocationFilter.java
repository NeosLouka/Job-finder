package org.example.ATS_source.search;

import org.example.ATS_source.model.JobPosting;
import org.springframework.stereotype.Component;

@Component
public class LocationFilter {

    private final EuropeanCountries europeanCountries;

    public LocationFilter(EuropeanCountries europeanCountries) {
        this.europeanCountries = europeanCountries;
    }



    public boolean matches(JobPosting job, JobSearchRequest request) {

        if (request.locations() != null && !request.locations().isEmpty()) {

            boolean matchesLocation = request.locations()
                    .stream()
                            .anyMatch(location ->
                                    matchesLocation(
                                            job,
                                            location
                                    )
                            );

            if (!matchesLocation) {
                return false;
            }
        }


        if (
                request.europeOnly() && !europeanCountries.isEuropean(job)
        ) {
            return false;
        }


        return true;
    }


    private boolean matchesLocation(JobPosting job, String requestedLocation) {

        String target = requestedLocation.toLowerCase();


        if (job.location() != null && job.location().toLowerCase().contains(target)) {
            return true;
        }


        if (job.offices() != null) {

            for (JobPosting.Office office : job.offices()) {

                if (office.location() != null && office.location().toLowerCase().contains(target)) {
                    return true;
                }

                if (office.name() != null && office.name().toLowerCase().contains(target)) {
                    return true;
                }
            }
        }


        return false;
    }
}