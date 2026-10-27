package org.example.ATS_source.explored.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import org.example.ATS_source.model.JobPosting;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;

@Component
@RequiredArgsConstructor
public class ExploredJobsRepository {

    private final Path root =
            Path.of("data", "explored");

    private final ObjectMapper objectMapper;


    public void markExplored(JobPosting job) {

        if (isExplored(job)) {

            System.out.println(
                    "Job already explored: "
                            + job.title()
            );

            return;
        }


        try {

            Path companyFolder =
                    getCompanyFolder(job);

            Files.createDirectories(
                    companyFolder
            );


            Path jobFile =
                    getJobFile(job);


            ObjectNode json =
                    objectMapper.createObjectNode();


            json.put(
                    "externalId",
                    job.externalId()
            );

            json.put(
                    "provider",
                    job.provider().name()
            );

            json.put(
                    "companyName",
                    job.companyName()
            );

            json.put(
                    "title",
                    job.title()
            );

            json.put(
                    "absoluteUrl",
                    job.absoluteUrl()
            );

            json.put(
                    "openedAt",
                    OffsetDateTime
                            .now()
                            .toString()
            );


            objectMapper
                    .writerWithDefaultPrettyPrinter()
                    .writeValue(
                            jobFile.toFile(),
                            json
                    );

        } catch (IOException e) {

            throw new RuntimeException(
                    "Failed to save explored job",
                    e
            );
        }
    }


    public boolean isExplored(
            JobPosting job
    ) {

        return Files.exists(
                getJobFile(job)
        );
    }


    private Path getJobFile(
            JobPosting job
    ) {

        String externalId =
                sanitizeFileName(
                        job.externalId()
                );

        return getCompanyFolder(job)
                .resolve(
                        externalId + ".json"
                );
    }


    private Path getCompanyFolder(
            JobPosting job
    ) {

        String companyName =
                sanitizeFileName(
                        job.companyName()
                );

        return root
                .resolve(
                        job.provider().name()
                )
                .resolve(
                        companyName
                );
    }


    private String sanitizeFileName(String name) {

        if (name == null || name.isBlank()) {
            return "unknown";
        }

        String sanitized = name
                .strip()
                .replaceAll(
                        "[\\\\/:*?\"<>|]",
                        "_"
                );

        /*
         * Windows also doesn't allow
         * trailing spaces or periods.
         */
        sanitized = sanitized.replaceAll(
                "[. ]+$",
                ""
        );

        if (sanitized.isBlank()) {
            return "unknown";
        }

        return sanitized;
    }
}