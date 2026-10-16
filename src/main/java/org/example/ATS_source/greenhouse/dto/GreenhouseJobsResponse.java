package org.example.ATS_source.greenhouse.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.example.ATS_source.model.AtsProvider;
import org.example.ATS_source.model.JobPosting;

import java.time.OffsetDateTime;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GreenhouseJobsResponse(

        List<JobDto> jobs

) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record JobDto(

            @JsonProperty("id")
            Long id,

            @JsonProperty("title")
            String title,

            @JsonProperty("company_name")
            String companyName,

            @JsonProperty("location")
            LocationDto location,

            @JsonProperty("language")
            String language,

            @JsonProperty("updated_at")
            OffsetDateTime updatedAt,

            @JsonProperty("absolute_url")
            String absoluteUrl

    ) {

        public JobPosting toJobPosting(String boardToken) {

            return new JobPosting(
                    id != null ? id.toString() : null,
                    AtsProvider.GREENHOUSE,
                    boardToken,
                    title,
                    companyName,
                    location != null ? location.name() : null,
                    language,
                    absoluteUrl,
                    updatedAt
            );
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record LocationDto(
            String name
    ) {
    }
}