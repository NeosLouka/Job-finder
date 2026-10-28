package org.example.ATS_source.ashby.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record AshbyJobsResponse(
        String apiVersion,
        List<AshbyJobDto> jobs
) {
}