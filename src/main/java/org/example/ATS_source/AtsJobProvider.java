package org.example.ATS_source;

import org.example.ATS_source.search.JobSearchRequest;
import org.example.ATS_source.model.AtsProvider;
import org.example.ATS_source.model.JobPosting;

import java.util.List;

public interface AtsJobProvider {

    AtsProvider provider();

    List<JobPosting> searchJobs(
            JobSearchRequest request
    );
}
