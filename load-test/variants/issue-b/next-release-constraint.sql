-- Release N+1 only. Review existing duplicates/NULLs and deploy 409 handling first.
-- Not an automatically executed Flyway migration in release N.
ALTER TABLE applicants
    ADD CONSTRAINT uk_applicants_user_email_applyform
    UNIQUE (user_email, applyform_id);
