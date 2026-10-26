package org.example.candidate;

import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

@Component
public class CandidateProfileReader {

    private final Path profilePath = Path.of("data/candidate_profile.txt");

    public String readProfile(){
        try {
            if(!Files.exists(profilePath)){
                throw new IllegalStateException("Candidate profile does not exist!");
            }

            return Files.readString(profilePath, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read candidate profile.", e);
        }
    }
}
