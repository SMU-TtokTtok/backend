-- Create temp_applyforms table
CREATE TABLE temp_applyforms (
    id VARCHAR(36) PRIMARY KEY,
    club_id VARCHAR(255) NOT NULL,
    title VARCHAR(100),
    sub_title TEXT,
    apply_start_date DATE NOT NULL,
    apply_end_date DATE NOT NULL,
    has_interview BOOLEAN NOT NULL,
    interview_start_date DATE,
    interview_end_date DATE,
    max_apply_count INTEGER NOT NULL,
    form_json JSONB,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- Create temp_applyform_grades table for grades collection
CREATE TABLE temp_applyform_grades (
    temp_applyform_id VARCHAR(36) NOT NULL,
    grades VARCHAR(50) NOT NULL,
    FOREIGN KEY (temp_applyform_id) REFERENCES temp_applyforms(id) ON DELETE CASCADE
);