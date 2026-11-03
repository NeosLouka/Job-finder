package org.example.ATS_source.search;

import org.example.ATS_source.ashby.service.AshbyJobService;
import org.example.ATS_source.greenhouse.service.GreenhouseJobService;
import org.example.ATS_source.ranking.JobRole;
import org.example.ATS_source.ranking.RankedJob;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class CombinedJobService {

    private final AshbyJobService ashbyJobService;
    private final GreenhouseJobService greenhouseJobService;

    public CombinedJobService(
            AshbyJobService ashbyJobService,
            GreenhouseJobService greenhouseJobService
    ) {
        this.ashbyJobService = ashbyJobService;
        this.greenhouseJobService = greenhouseJobService;
    }

    public List<RankedJob> filterAndRankAllJobs(
            JobSearchRequest request,
            JobRole role,
            Integer minYears,
            Integer maxYears
    ) {

        List<RankedJob> ashbyRankedJobs = ashbyJobService
                .filterAndRankAllJobs(
                        request,
                        role,
                        minYears,
                        maxYears
                );

        List<RankedJob> greenhouseRankedJobs = greenhouseJobService
                .filterAndRankAllJobs(
                        request,
                        role,
                        minYears,
                        maxYears
                );


        List<RankedJob> combinedRankedJobs = new ArrayList<>();

        combinedRankedJobs.addAll(ashbyRankedJobs);
        combinedRankedJobs.addAll(greenhouseRankedJobs);


        combinedRankedJobs.sort(
                Comparator
                        .comparingInt(RankedJob::score)
                        .reversed()
        );


        return combinedRankedJobs;
    }
}
