package org.example.ATS_source.model;

import java.time.OffsetDateTime;

public record JobPosting(

        String externalId,

        AtsProvider provider,

        String sourceBoard,

        String title,

        String companyName,

        String location,

        String language,

        String absoluteUrl,

        OffsetDateTime updatedAt

) {
}