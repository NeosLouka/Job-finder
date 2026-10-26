package org.example.ATS_source.llm;

public record JobMatchResult(
        double probability,
        boolean match
) {
}