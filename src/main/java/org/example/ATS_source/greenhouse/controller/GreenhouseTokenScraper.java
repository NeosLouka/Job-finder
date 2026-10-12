package org.example.ATS_source.greenhouse.controller;


import org.example.ATS_source.greenhouse.boardTokens.GreenhouseTools;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/v1/greenhouse/tokens")
public class GreenhouseTokenScraper {

    private final GreenhouseTools greenhouseTools;

    public GreenhouseTokenScraper(GreenhouseTools greenhouseTools) {
        this.greenhouseTools = greenhouseTools;
    }

    @GetMapping
    public ResponseEntity<List<String>> getTokens() {
        try {
            List<String> tokens = greenhouseTools.retrieveExistingTokens();
            return ResponseEntity.ok(tokens);
        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/search")
    public ResponseEntity<String> searchForTokens(){
        try {
            greenhouseTools.findTokens();

            return ResponseEntity
                    .status(HttpStatus.ACCEPTED)
                    .body("Token discovery job started in the background.");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Token discovery job started in the background." + e.getMessage());
        }
    }
}
