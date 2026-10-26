package org.example.ATS_source.tracking.model;

import java.time.OffsetDateTime;

public record TrackedJob(
        String externalId,
        String provider,
        String sourceBoard,

        String title,
        String companyName,
        String absoluteUrl,

        JobStatus status,

        OffsetDateTime viewedAt,
        OffsetDateTime appliedAt
) {
}
