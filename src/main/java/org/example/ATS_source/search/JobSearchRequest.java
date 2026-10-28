package org.example.ATS_source.search;


import java.time.OffsetDateTime;
import java.util.List;


public record JobSearchRequest(
        List<String> titles,
        List<String> excludedTitleTerms,
        List<String> locations,
        String language,
        OffsetDateTime lastUpdated,
        boolean europeOnly

) {


}
