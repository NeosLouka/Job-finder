package org.example.ATS_source.greenhouse.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.example.ATS_source.greenhouse.boardTokens.GreenhouseTools;
import org.example.ATS_source.greenhouse.dto.CompanySearchResponse;
import org.example.ATS_source.greenhouse.dto.GreenhouseJobsResponse;
import org.example.ATS_source.greenhouse.dto.JobSearchRequest;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestBody;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class GreenhouseJobService {

    private final GreenhouseTools greenhouseTools;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public GreenhouseJobService(ObjectMapper objectMapper , GreenhouseTools greenhouseTools) {
        this.greenhouseTools = greenhouseTools;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    public List<CompanySearchResponse> searchAllBoardTokens(JobSearchRequest request){

        List<String> tokens = new ArrayList<>();

        try {
            tokens = greenhouseTools.retrieveExistingTokens();
        } catch (IOException e) {
            throw new RuntimeException("Failed to retrieve existing Tokens!");
        }

        CompanySearchResponse response = new CompanySearchResponse(new ArrayList<>());
        for(String token : tokens){
            response.roles().add();
        }

    }


    public CompanySearchResponse findRequestRelatedJobs(String boardToken , JobSearchRequest request){

        CompanySearchResponse jobListBuffer = new CompanySearchResponse(new ArrayList<>());

        if(boardToken == null || boardToken.isBlank())
            throw new IllegalArgumentException("boardToken cannot be null or blank.");

        try {
            GreenhouseJobsResponse response = fetchCompanyJobs(boardToken);

            for(GreenhouseJobsResponse.JobDto job : response.jobs()){
                if(filterJob(job, request.titles(), request.locations(), request.language(), request.lastUpdated())){
                    jobListBuffer.roles().add(new CompanySearchResponse.RoleDetails(
                            job.title(),
                            job.companyName(),
                            job.location(),
                            job.language(),
                            job.absoluteUrl(),
                            job.updatedAt()
                    ));
                }

            }

        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch or filter jobs for token: " + boardToken, e);
        }

        return jobListBuffer;
    }

    private GreenhouseJobsResponse fetchCompanyJobs(String boardToken) throws IOException, InterruptedException {

        String url = "https://boards-api.greenhouse.io/v1/boards/" + boardToken + "/jobs";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Accept", "application/json")
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if(response.statusCode() == 200){
            return objectMapper.readValue(response.body(), GreenhouseJobsResponse.class);
        }

        throw new IllegalStateException("Failed to fetch jobs for token " + boardToken + ": HTTP " + response.statusCode());

    }

    private boolean filterJob(GreenhouseJobsResponse.JobDto jobDto, List<String> titles, List<String> locations, String language, OffsetDateTime lastUpdated){

        if(titles != null || !titles.isEmpty()){
            boolean titleMatch = titles.stream().anyMatch(l -> jobDto.title() != null &&
                        jobDto.title().toLowerCase().contains(l.toLowerCase())
            );
            if(!titleMatch)
                return false;
        }

        if(locations != null && !locations.isEmpty()){
            boolean locationMatch = locations.stream().anyMatch(l -> jobDto.location() != null &&
                    jobDto.location().name().toLowerCase().contains(l.toLowerCase())
            );
            if(!locationMatch)
                return false;
        }

        if (language != null && !language.isBlank()) {
            if (jobDto.language() == null || !jobDto.language().equalsIgnoreCase(language)) {
                return false;
            }
        }

        if (lastUpdated != null) {
            if (jobDto.updatedAt() == null || jobDto.updatedAt().isBefore(lastUpdated)) {
                return false;
            }
        }
        return true;
    }



}

















