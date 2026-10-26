package org.example.ATS_source.ranking;

import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class ExperienceRequirementAnalyzer {

    /*
     * Examples:
     *
     * 1-3 years of experience
     * 1 – 3 years of professional experience
     * 2 to 4 years of experience
     */
    private static final Pattern RANGE_PATTERN =
            Pattern.compile(
                    "(\\d+)\\s*(?:-|–|to)\\s*(\\d+)\\s*years?",
                    Pattern.CASE_INSENSITIVE
            );


    /*
     * Examples:
     *
     * 3+ years of experience
     * 4+ years professional experience
     */
    private static final Pattern PLUS_PATTERN =
            Pattern.compile(
                    "(\\d+)\\s*\\+\\s*years?",
                    Pattern.CASE_INSENSITIVE
            );


    /*
     * Examples:
     *
     * at least 4 years of experience
     * minimum 5 years of experience
     */
    private static final Pattern MINIMUM_PATTERN =
            Pattern.compile(
                    "(?:at least|minimum|min\\.?)\\s*(\\d+)\\s*years?",
                    Pattern.CASE_INSENSITIVE
            );


    /*
     * Example:
     *
     * 3 years of professional experience
     */
    private static final Pattern EXACT_PATTERN =
            Pattern.compile(
                    "(\\d+)\\s*years?"
                            + "(?:\\s+of)?"
                            + "(?:\\s+\\w+){0,6}"
                            + "\\s+experience",
                    Pattern.CASE_INSENSITIVE
            );


    private static final Pattern NUMBER_WORD_PATTERN =
            Pattern.compile(
                    "\\b(one|two|three|four|five|six|seven|eight|nine|ten)\\b",
                    Pattern.CASE_INSENSITIVE
            );

    public ExperienceRequirement analyze(String description) {

        if (description == null || description.isBlank()) {

            return new ExperienceRequirement(
                    null,
                    null
            );
        }


        /*
         * Convert word numbers (e.g., "three") into digits (e.g., "3") first
         */
        String normalizedDescription = normalizeNumberWords(description);


        /*
         * First try a range.
         */
        Matcher rangeMatcher = RANGE_PATTERN.matcher(normalizedDescription);

        if (rangeMatcher.find()) {

            int min = Integer.parseInt(rangeMatcher.group(1));

            int max = Integer.parseInt(rangeMatcher.group(2));

            return new ExperienceRequirement(
                    min,
                    max
            );
        }


        /*
         * Then 3+, 4+, etc.
         */
        Matcher plusMatcher = PLUS_PATTERN.matcher(normalizedDescription);

        if (plusMatcher.find()) {

            int min = Integer.parseInt(plusMatcher.group(1));

            return new ExperienceRequirement(
                    min,
                    null
            );
        }


        /*
         * Then "at least 4 years".
         */
        Matcher minimumMatcher = MINIMUM_PATTERN.matcher(normalizedDescription);

        if (minimumMatcher.find()) {

            int min = Integer.parseInt(minimumMatcher.group(1));

            return new ExperienceRequirement(
                    min,
                    null
            );
        }


        /*
         * Finally simple requirements like:
         * "3 years of professional experience"
         */
        Matcher exactMatcher = EXACT_PATTERN.matcher(normalizedDescription);

        if (exactMatcher.find()) {

            int years = Integer.parseInt(exactMatcher.group(1));

            return new ExperienceRequirement(
                    years,
                    years
            );
        }


        /*
         * We couldn't confidently determine it.
         */
        return new ExperienceRequirement(
                null,
                null
        );
    }

    public boolean isSuitable(ExperienceRequirement experience, int maximumRequiredExperience) {

        /*
         * If we couldn't determine experience,
         * don't reject the job.
         * Laya can inspect it later.
         */
        if (experience.minYears() == null) {
            return true;
        }

        return experience.minYears() <= maximumRequiredExperience;
    }

    private String normalizeNumberWords(String text) {

        Matcher matcher = NUMBER_WORD_PATTERN.matcher(text);

        StringBuffer result = new StringBuffer();

        while (matcher.find()) {

            String replacement =
                    switch (matcher.group(1).toLowerCase(Locale.ROOT)) {

                        case "one" -> "1";
                        case "two" -> "2";
                        case "three" -> "3";
                        case "four" -> "4";
                        case "five" -> "5";
                        case "six" -> "6";
                        case "seven" -> "7";
                        case "eight" -> "8";
                        case "nine" -> "9";
                        case "ten" -> "10";

                        default -> matcher.group(1);
                    };


            matcher.appendReplacement(
                    result,
                    replacement
            );
        }


        matcher.appendTail(result);

        return result.toString();
    }

}