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

-- updated_at 자동 업데이트를 위한 트리거 함수 생성
CREATE OR REPLACE FUNCTION update_updated_at_column()
    RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- temp_applicant 테이블에 updated_at 자동 업데이트 트리거 추가
CREATE TRIGGER update_temp_applicants_updated_at
    BEFORE UPDATE ON temp_applicants
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();