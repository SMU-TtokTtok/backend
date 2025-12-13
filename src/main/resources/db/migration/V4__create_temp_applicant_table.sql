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
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes for better query performance
CREATE INDEX idx_temp_applicants_form_id ON temp_applicants(form_id);
CREATE INDEX idx_temp_applicants_user_email ON temp_applicants(user_email);
CREATE INDEX idx_temp_applicants_email ON temp_applicants(email);
CREATE INDEX idx_temp_applicants_name ON temp_applicants(name);
CREATE INDEX idx_temp_applicants_user_form ON temp_applicants(user_email, form_id);
CREATE INDEX idx_temp_applicants_grade ON temp_applicants(grade);
CREATE INDEX idx_temp_applicants_student_status ON temp_applicants(student_status);
CREATE INDEX idx_temp_applicants_created_at ON temp_applicants(created_at);

