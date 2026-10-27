package org.example.ATS_source.greenhouse.service;

import org.example.ATS_source.model.JobPosting;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

@Component
public class EuropeanCountries {

    /*
     * Full country names.
     */
    private static final Set<String> EUROPEAN_COUNTRY_NAMES = Set.of(
            "albania",
            "andorra",
            "austria",
            "belgium",
            "bosnia and herzegovina",
            "bulgaria",
            "croatia",
            "cyprus",
            "czech republic",
            "czechia",
            "denmark",
            "estonia",
            "finland",
            "france",
            "germany",
            "deutschland",
            "greece",
            "hungary",
            "iceland",
            "ireland",
            "italy",
            "kosovo",
            "latvia",
            "liechtenstein",
            "lithuania",
            "luxembourg",
            "malta",
            "moldova",
            "monaco",
            "montenegro",
            "netherlands",
            "north macedonia",
            "norway",
            "poland",
            "portugal",
            "romania",
            "san marino",
            "serbia",
            "slovakia",
            "slovenia",
            "spain",
            "sweden",
            "switzerland",
            "ukraine",
            "united kingdom",
            "england",
            "scotland",
            "wales",
            "northern ireland"
    );


    /*
     * ISO-3 codes.
     *
     * These are very useful because Greenhouse actually
     * returns locations such as:
     *
     * IRL-Dublin
     * DNK
     * NLD
     * DEU
     */
    private static final Set<String> EUROPEAN_ISO3_CODES = Set.of(
            "ALB", "AND", "AUT", "BEL", "BIH",
            "BGR", "HRV", "CYP", "CZE", "DNK",
            "EST", "FIN", "FRA", "DEU", "GRC",
            "HUN", "ISL", "IRL", "ITA", "XKX",
            "LVA", "LIE", "LTU", "LUX", "MLT",
            "MDA", "MCO", "MNE", "NLD", "MKD",
            "NOR", "POL", "PRT", "ROU", "SMR",
            "SRB", "SVK", "SVN", "ESP", "SWE",
            "CHE", "UKR", "GBR"
    );


    /*
     * Safe ISO-2 codes.
     *
     * Important:
     * AL, DE, MD, ME and MT are NOT here because
     * they collide with US state abbreviations.
     *
     * Examples:
     *
     * AL = Albania / Alabama
     * DE = Germany / Delaware
     * MD = Moldova / Maryland
     * ME = Montenegro / Maine
     * MT = Malta / Montana
     */
    private static final Set<String> SAFE_EUROPEAN_ISO2_CODES = Set.of(
            "AD", "AT", "BE", "BA", "BG",
            "HR", "CY", "CZ", "DK", "EE",
            "FI", "FR", "GR", "HU", "IS",
            "IE", "IT", "XK", "LV", "LI",
            "LT", "LU", "MC", "NL", "MK",
            "NO", "PL", "PT", "RO", "SM",
            "RS", "SK", "SI", "ES", "SE",
            "CH", "UA", "GB", "UK"
    );


    /*
     * These codes are accepted only when they appear
     * at the beginning in formats such as:
     *
     * DE-Berlin
     * DE - HQ - Berlin
     *
     * This prevents:
     *
     * Wilmington, DE
     *
     * from being interpreted as Germany.
     */
    private static final Set<String> PREFIX_ONLY_CODES = Set.of(
            "AL",
            "DE",
            "MD",
            "ME",
            "MT"
    );


    private static final List<Pattern> COUNTRY_NAME_PATTERNS =
            buildCountryNamePatterns();

    private static final List<Pattern> ISO3_PATTERNS =
            buildCodePatterns(EUROPEAN_ISO3_CODES);

    private static final List<Pattern> SAFE_ISO2_PATTERNS =
            buildCodePatterns(SAFE_EUROPEAN_ISO2_CODES);

    private static final List<Pattern> PREFIX_ONLY_PATTERNS =
            buildPrefixPatterns();


    /*
     * Matches:
     *
     * Europe
     * Remote Europe
     * Remote - Europe
     * European
     *
     * Does NOT match EMEA.
     */
    private static final Pattern EUROPE_PATTERN =
            Pattern.compile(
                    "(?i)(?<![a-z])europe(?:an)?(?![a-z])"
            );


    /*
     * Matches EU only as its own token.
     *
     * Examples:
     *
     * EU
     * Remote - EU
     * EU Headquarters
     *
     * It will not accidentally match normal words.
     */
    private static final Pattern EU_PATTERN =
            Pattern.compile(
                    "(?<![A-Za-z0-9])EU(?![A-Za-z0-9])"
            );


    public boolean isEuropeanLocation(JobPosting job) {

        if (job == null) {
            return false;
        }


        List<String> locations = new ArrayList<>();


        /*
         * First check the office information.
         *
         * It often contains more structured location data.
         */
        if (job.offices() != null) {

            for (JobPosting.Office office : job.offices()) {

                if (office.location() != null
                        && !office.location().isBlank()) {

                    locations.add(
                            office.location()
                    );
                }


                if (office.name() != null
                        && !office.name().isBlank()) {

                    locations.add(
                            office.name()
                    );
                }
            }
        }


        /*
         * Also check the normal job location.
         */
        if (job.location() != null
                && !job.location().isBlank()) {

            locations.add(
                    job.location()
            );
        }


        for (String location : locations) {

            if (isEuropeanLocationText(location)) {
                return true;
            }
        }


        return false;
    }


    private boolean isEuropeanLocationText(String location) {

        /*
         * Full country names.
         */
        for (Pattern pattern : COUNTRY_NAME_PATTERNS) {

            if (pattern.matcher(location).find()) {
                return true;
            }
        }


        /*
         * ISO-3:
         *
         * IRL-Dublin
         * Aarhus, DNK
         * Amsterdam, NLD
         */
        for (Pattern pattern : ISO3_PATTERNS) {

            if (pattern.matcher(location).find()) {
                return true;
            }
        }


        /*
         * Safe ISO-2:
         *
         * Dublin, IE
         * Barcelona, ES
         * Zürich, CH
         */
        for (Pattern pattern : SAFE_ISO2_PATTERNS) {

            if (pattern.matcher(location).find()) {
                return true;
            }
        }


        /*
         * Ambiguous ISO-2 codes are accepted only
         * as prefixes.
         *
         * DE-Berlin       -> true
         * Wilmington, DE  -> false
         */
        for (Pattern pattern : PREFIX_ONLY_PATTERNS) {

            if (pattern.matcher(location).find()) {
                return true;
            }
        }


        /*
         * Europe / European.
         */
        if (EUROPE_PATTERN.matcher(location).find()) {
            return true;
        }


        /*
         * EU as a standalone token.
         */
        return EU_PATTERN.matcher(location).find();
    }


    private static List<Pattern> buildCountryNamePatterns() {

        List<Pattern> patterns =
                new ArrayList<>();


        for (String country : EUROPEAN_COUNTRY_NAMES) {

            patterns.add(
                    Pattern.compile(
                            "(?i)(?<![\\p{L}\\p{N}])"
                                    + Pattern.quote(country)
                                    + "(?![\\p{L}\\p{N}])"
                    )
            );
        }


        return List.copyOf(patterns);
    }


    private static List<Pattern> buildCodePatterns(
            Set<String> codes
    ) {

        List<Pattern> patterns =
                new ArrayList<>();


        for (String code : codes) {

            /*
             * Deliberately CASE-SENSITIVE.
             *
             * Otherwise codes such as:
             *
             * AT
             * IT
             * IS
             * NO
             *
             * could match normal English words:
             *
             * "at"
             * "it"
             * "is"
             * "no"
             */
            patterns.add(
                    Pattern.compile(
                            "(?<![A-Za-z0-9])"
                                    + Pattern.quote(code)
                                    + "(?![A-Za-z0-9])"
                    )
            );
        }


        return List.copyOf(patterns);
    }


    private static List<Pattern> buildPrefixPatterns() {

        List<Pattern> patterns =
                new ArrayList<>();


        for (String code : PREFIX_ONLY_CODES) {

            patterns.add(
                    Pattern.compile(
                            "^\\s*"
                                    + Pattern.quote(code)
                                    + "\\s*[-_]"
                    )
            );
        }


        return List.copyOf(patterns);
    }
}
