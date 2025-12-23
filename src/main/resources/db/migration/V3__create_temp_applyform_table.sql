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

-- updated_at 자동 업데이트를 위한 트리거 함수 생성
CREATE OR REPLACE FUNCTION update_updated_at_column()
    RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- temp_applyform 테이블에 updated_at 자동 업데이트 트리거 추가
CREATE TRIGGER update_temp_applyforms_updated_at
    BEFORE UPDATE ON temp_applyforms
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();