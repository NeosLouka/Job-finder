package org.example;

import org.example.ATS_source.TokenRepository;
import org.example.ATS_source.greenhouse.GreenhouseTools;

import java.nio.file.Path;


public class Main {


    public static void main(String[] args) {

        TokenRepository tokenRepository = new TokenRepository(Path.of("data/greenhouse_tokens.txt"));

        GreenhouseTools greenhouseTools = new GreenhouseTools(tokenRepository);

        try {
            greenhouseTools.findTokens();
        } catch (Exception e) {
            System.err.println(
                    "Error: " + e.getMessage()
            );
            e.printStackTrace();
        }
    }
}
