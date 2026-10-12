CREATE TABLE job_posting (
                             id BIGINT PRIMARY KEY,
                             title VARCHAR(255) NOT NULL,
                             company_name VARCHAR(255),
                             location VARCHAR(255),
                             language VARCHAR(50),
                             absolut_url VARCHAR(255),
                             updated_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_job_posting_company ON job_posting (company_name);