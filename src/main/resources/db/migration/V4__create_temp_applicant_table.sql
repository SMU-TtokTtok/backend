-- Create temp_applicants table
CREATE TABLE temp_applicants (
    id VARCHAR(36) PRIMARY KEY,
    form_id VARCHAR(36) NOT NULL,
    user_email VARCHAR(255),
    name VARCHAR(255),
    age INTEGER,
    major VARCHAR(255),
    email VARCHAR(255),
    phone VARCHAR(50),
    student_status VARCHAR(50),
    grade VARCHAR(50),
    gender VARCHAR(50),
    answers JSONB,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);