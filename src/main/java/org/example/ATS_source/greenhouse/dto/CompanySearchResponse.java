package org.example.ATS_source.greenhouse.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.OffsetDateTime;
import java.util.List;

public record CompanySearchResponse(
        List<RoleDetails> roles
) {
    public record RoleDetails(
                @JsonProperty("title") String title,
                @JsonProperty("company_name") String companyName,
                @JsonProperty("location") GreenhouseJobsResponse.LocationDto location,
                @JsonProperty("language") String language,
                @JsonProperty("absolute_url") String absoluteUrl,
                @JsonProperty("updated_at") OffsetDateTime updatedAt
    ){}
}
