package org.example.ATS_source.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Repository
public class TokenRepository {

    private final Path file;

    public TokenRepository(@Value("${app.tokens.file-path:data/greenhouse_tokens.txt}") String filePath) {
        this.file = Path.of(filePath);
    }

    public Set<String> loadTokens() throws IOException {

        return new HashSet<>(
                Files.readAllLines(file)
        );
    }

    public void saveToken(String token)
            throws IOException {

        Files.writeString(
                file,
                token + System.lineSeparator(),
                StandardOpenOption.CREATE,
                StandardOpenOption.APPEND
        );
    }

}
