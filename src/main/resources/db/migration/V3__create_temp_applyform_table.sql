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
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create temp_applyform_grades table for grades collection
CREATE TABLE temp_applyform_grades (
    temp_applyform_id VARCHAR(36) NOT NULL,
    grades VARCHAR(50) NOT NULL,
    FOREIGN KEY (temp_applyform_id) REFERENCES temp_applyforms(id) ON DELETE CASCADE
);

-- Create indexes for better query performance
CREATE INDEX idx_temp_applyforms_club_id ON temp_applyforms(club_id);
CREATE INDEX idx_temp_applyforms_apply_dates ON temp_applyforms(apply_start_date, apply_end_date);
CREATE INDEX idx_temp_applyforms_interview_dates ON temp_applyforms(interview_start_date, interview_end_date);
CREATE INDEX idx_temp_applyforms_has_interview ON temp_applyforms(has_interview);
CREATE INDEX idx_temp_applyform_grades_temp_applyform_id ON temp_applyform_grades(temp_applyform_id);
