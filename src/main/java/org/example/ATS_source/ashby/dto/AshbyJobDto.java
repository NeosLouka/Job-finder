package org.example.ATS_source.ashby.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record AshbyJobDto(

        String title,
        String location,

        List<SecondaryLocation> secondaryLocations,

        String department,
        String team,

        Boolean isListed,
        Boolean isRemote,

        String workplaceType,

        String descriptionHtml,
        String descriptionPlain,

        String publishedAt,

        String employmentType,

        Address address,

        String jobUrl,
        String applyUrl

) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record SecondaryLocation(
            String location,
            PostalAddress address
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Address(
            PostalAddress postalAddress
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PostalAddress(
            String addressLocality,
            String addressRegion,
            String addressCountry
    ) {
    }
}