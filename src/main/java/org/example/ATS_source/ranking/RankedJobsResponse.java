package org.example.ATS_source.ranking;

import java.util.List;

public record RankedJobsResponse<T>(
        int count,
        int totalMatchingJobs,
        int limit,
        boolean reachedLimit,
        List<T> jobs
) {
}