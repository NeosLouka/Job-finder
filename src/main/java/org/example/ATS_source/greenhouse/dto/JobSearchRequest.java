package org.example.ATS_source.greenhouse.dto;


import java.time.OffsetDateTime;
import java.util.List;


public record JobSearchRequest(
        List<String> titles,
        List<String> locations,
        String language,
        OffsetDateTime lastUpdated
) {

}
