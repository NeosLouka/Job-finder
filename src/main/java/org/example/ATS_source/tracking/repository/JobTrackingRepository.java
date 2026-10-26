package org.example.ATS_source.tracking.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.example.ATS_source.model.JobPosting;
import org.example.ATS_source.tracking.model.JobStatus;
import org.example.ATS_source.tracking.model.TrackedJob;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;

@Component
@RequiredArgsConstructor
public class JobTrackingRepository {

    private final ObjectMapper objectMapper;
    private final Path root = Path.of("data","tracked");


    public JobStatus getStatus(JobPosting job) {

        Path file = getJobFile(job);

        if (!Files.exists(file)) {
            return JobStatus.NEW;
        }

        try {

            TrackedJob trackedJob = objectMapper.readValue(file.toFile(), TrackedJob.class);

            return trackedJob.status();

        } catch (IOException e) {

            throw new RuntimeException("Failed to read tracked job", e);
        }
    }


    public TrackedJob markViewed(JobPosting job) {

        TrackedJob existing = read(job);

        /*
         * If already APPLIED,
         * don't downgrade it back to VIEWED.
         */
        if (existing != null && existing.status() == JobStatus.APPLIED) {

            return existing;
        }


        OffsetDateTime viewedAt = OffsetDateTime.now();

        if (existing != null && existing.viewedAt() != null) {

            viewedAt = existing.viewedAt();
        }


        TrackedJob trackedJob =
                new TrackedJob(
                        job.externalId(),

                        job.provider().name(),

                        job.sourceBoard(),

                        job.title(),

                        job.companyName(),

                        job.absoluteUrl(),

                        JobStatus.VIEWED,

                        viewedAt,

                        existing == null ? null : existing.appliedAt()
                );

        save(job, trackedJob);

        return trackedJob;
    }


    public TrackedJob markApplied(JobPosting job) {

        TrackedJob existing = read(job);

        OffsetDateTime viewedAt = existing == null ? null : existing.viewedAt();


        TrackedJob trackedJob =
                new TrackedJob(

                        job.externalId(),

                        job.provider().name(),

                        job.sourceBoard(),

                        job.title(),

                        job.companyName(),

                        job.absoluteUrl(),

                        JobStatus.APPLIED,

                        viewedAt,

                        OffsetDateTime.now()
                );


        save(job, trackedJob);


        return trackedJob;
    }


    private TrackedJob read(JobPosting job) {

        Path file = getJobFile(job);

        if (!Files.exists(file)) {
            return null;
        }

        try {

            return objectMapper.readValue(
                    file.toFile(),
                    TrackedJob.class
            );

        } catch (IOException e) {

            throw new RuntimeException("Failed to read tracked job", e);
        }
    }


    private void save(JobPosting job, TrackedJob trackedJob) {

        try {
            Path companyFolder = getCompanyFolder(job);

            Files.createDirectories(companyFolder);

            objectMapper
                    .writerWithDefaultPrettyPrinter()
                    .writeValue(
                            getJobFile(job)
                                    .toFile(),
                            trackedJob
                    );

        } catch (IOException e) {

            throw new RuntimeException("Failed to save tracked job", e);
        }
    }


    private Path getCompanyFolder(JobPosting job) {

        return root.resolve(sanitize(job.companyName()));
    }


    private Path getJobFile(JobPosting job
    ) {
        String fileName =
                sanitize(
                        job.provider().name()
                )
                        + "_"
                        + sanitize(
                        job.sourceBoard()
                )
                        + "_"
                        + sanitize(
                        job.externalId()
                )
                        + ".json";


        return getCompanyFolder(job).resolve(fileName);
    }


    private String sanitize(String value) {

        if (value == null) {
            return "unknown";
        }


        return value.replaceAll(
                "[\\\\/:*?\"<>|]",
                "_"
        );
    }
}