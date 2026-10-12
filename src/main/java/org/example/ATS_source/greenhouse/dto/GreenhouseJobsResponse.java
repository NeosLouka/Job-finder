package org.example.ATS_source.greenhouse.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.example.ATS_source.greenhouse.entity.JobPosting;

import java.time.OffsetDateTime;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GreenhouseJobsResponse(
        List<JobDto> jobs
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record JobDto(
            @JsonProperty("id") Long id,
            @JsonProperty("title") String title,
            @JsonProperty("company_name") String companyName,
            @JsonProperty("location") LocationDto location,
            @JsonProperty("language") String language,
            @JsonProperty("updated_at") OffsetDateTime updatedAt,
            @JsonProperty("absolute_url") String absoluteUrl
    ) {
        public JobPosting toEntity() {
            JobPosting entity = new JobPosting();
            entity.setId(this.id);
            entity.setTitle(this.title);
            entity.setCompanyName(this.companyName);
            entity.setLocation(this.location != null ? this.location.name() : null);
            entity.setLanguage(this.language);
            entity.setAbsolutUrl(this.absoluteUrl);
            entity.setUpdatedAt(this.updatedAt);
            return entity;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record LocationDto(
            String name
    ) {}
}
