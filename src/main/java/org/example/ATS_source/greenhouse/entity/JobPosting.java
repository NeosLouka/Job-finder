package org.example.ATS_source.greenhouse.entity;


import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;

@Entity
@Table(name = "job_posting")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class JobPosting {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private Long id;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "company_name")
    private String companyName;

    @Column(name = "location")
    private String location;

    @Column(name = "language")
    private String language;

    @Column(name = "absolut_url")
    private String absolutUrl;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;


}