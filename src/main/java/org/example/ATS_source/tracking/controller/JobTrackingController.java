package org.example.ATS_source.tracking.controller;

import lombok.RequiredArgsConstructor;
import org.example.ATS_source.model.JobPosting;
import org.example.ATS_source.tracking.model.TrackedJob;
import org.example.ATS_source.tracking.repository.JobTrackingRepository;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/tracking")
@RequiredArgsConstructor
public class JobTrackingController {

    private final JobTrackingRepository jobTrackingRepository;


    @PostMapping("/viewed")
    public TrackedJob markViewed(@RequestBody JobPosting job) {

        return jobTrackingRepository.markViewed(job);
    }


    @PostMapping("/applied")
    public TrackedJob markApplied(@RequestBody JobPosting job) {

        return jobTrackingRepository.markApplied(job);
    }
}