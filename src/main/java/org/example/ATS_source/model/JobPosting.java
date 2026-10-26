package org.example.ATS_source.model;

import java.time.OffsetDateTime;
import java.util.List;

public record JobPosting(

        String externalId,

        AtsProvider provider,

        String sourceBoard,

        String title,

        String companyName,

        String location,

        String language,

        String absoluteUrl,

        OffsetDateTime updatedAt,

        String description,

        List<Office> offices

) {

    public record Office(
            String name,
            String location
    ) {
    }
}