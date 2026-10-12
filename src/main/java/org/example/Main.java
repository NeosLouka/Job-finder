package org.example;


import org.example.ATS_source.greenhouse.service.GreenhouseJobService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableAsync;

import java.io.IOException;

@EnableAsync
@SpringBootApplication
public class Main{

    public static void main(String[] args) {
        SpringApplication.run(Main.class, args);
    }

//    @Bean
//    public CommandLineRunner testService(GreenhouseJobService jobService) {
//        return args -> {
//            System.out.println(">>> Testing GreenhouseJobService from Main <<<");
//
//            try {
//                // Call a method on your service here
//                var response = jobService.fetchJobs("stripe");
//                System.out.println("Successfully fetched jobs count: " + response.jobs().size());
//            } catch (Exception e) {
//                System.err.println("Test failed: " + e.getMessage());
//                e.printStackTrace();
//            }
//        };
//    }
}