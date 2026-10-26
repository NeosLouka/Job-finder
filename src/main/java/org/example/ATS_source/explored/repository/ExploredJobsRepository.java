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

    private final Path root = Path.of("data", "explored");
    private final ObjectMapper objectMapper;


    public void markExplored(JobPosting job) {

        if (isExplored(job)) {
            System.out.println(
                    "Job already explored: " + job.title()
            );

            return;
        }

        try {

            Path companyFolder = getCompanyFolder(job);

            Files.createDirectories(companyFolder);

            Path jobFile = getJobFile(job);

            ObjectNode json = objectMapper.createObjectNode();

            json.put(
                    "externalId",
                    job.externalId()
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



    public boolean isExplored(JobPosting job) {

        Path jobFile = getJobFile(job);

        return Files.exists(jobFile);
    }


    private Path getJobFile(JobPosting job) {

        return getCompanyFolder(job)
                .resolve(
                        job.externalId()
                                + ".json"
                );
    }

    private Path getCompanyFolder(JobPosting job) {

        String companyName = sanitizeFileName(job.companyName());

        return root.resolve(companyName);
    }



    private String sanitizeFileName(
            String name
    ) {

        return name.replaceAll(
                "[\\\\/:*?\"<>|]",
                "_"
        );
    }

}
