package org.example.ATS_source;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.HashSet;
import java.util.Set;

public class TokenRepository {

    private final Path file;

    public TokenRepository(Path file) {
        this.file = file;
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
