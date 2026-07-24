-- OAuth 지원: 비밀번호 NULL 허용 (소셜 전용 계정), 구글 식별자 컬럼 추가
ALTER TABLE users ALTER COLUMN password DROP NOT NULL;

ALTER TABLE users ADD COLUMN provider VARCHAR(20) NOT NULL DEFAULT 'LOCAL';
ALTER TABLE users ADD COLUMN provider_id VARCHAR(255);

-- 동일 구글 계정(sub)이 두 사용자 행에 연결되는 것을 방지 (NULL 값끼리는 충돌하지 않음)
ALTER TABLE users ADD CONSTRAINT uk_users_provider_provider_id UNIQUE (provider, provider_id);
