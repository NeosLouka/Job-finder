package org.example.ATS_source.greenhouse.controller;


import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.ATS_source.greenhouse.dto.CompanySearchResponse;
import org.example.ATS_source.greenhouse.dto.JobSearchRequest;
import org.example.ATS_source.greenhouse.service.GreenhouseJobService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/greenhouse/search")
@RequiredArgsConstructor
public class GreenhouseJobSearch {

    private final GreenhouseJobService greenhouseService;

    @PostMapping
    public ResponseEntity<CompanySearchResponse> search(
            @RequestParam("boardToken") String boardToken,
            @Valid @RequestBody JobSearchRequest request
    ){
        CompanySearchResponse response = greenhouseService.findRequestRelatedJobs(boardToken, request);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(response);

    }
}
