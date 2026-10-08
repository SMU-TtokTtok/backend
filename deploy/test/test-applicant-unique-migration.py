"""Validate #426 SQL only against the dedicated disposable PostgreSQL container."""
from pathlib import Path
import subprocess
import uuid

ROOT = Path(__file__).resolve().parents[2]
PENDING = ROOT / "deploy/migrations/pending"
MIGRATION = (PENDING / "V28__add_applicant_user_form_unique_constraint.sql").read_text(encoding="utf-8")
PREFLIGHT = (PENDING / "check_applicant_duplicates.sql").read_text(encoding="utf-8")


def query(sql):
    return subprocess.run([
        "docker", "exec", "-i", "ttokttok-426-repro-pg", "psql",
        "-U", "postgres", "-d", "ttokttok426", "-qAt",
        "-v", "ON_ERROR_STOP=1", "-v", "VERBOSITY=verbose",
    ], input=sql, capture_output=True, text=True, encoding="utf-8")


def check(name, body, expected_error=False):
    schema = "migration426_" + uuid.uuid4().hex
    setup = f"""
        BEGIN;
        CREATE SCHEMA {schema};
        SET LOCAL search_path TO {schema};
        CREATE TABLE applicants (id text PRIMARY KEY, user_email text, applyform_id text);
    """
    result = query(setup + body + "\nROLLBACK;")
    if expected_error:
        assert result.returncode != 0 and "23505" in result.stderr, result.stderr
        assert "uk_applicants_user_email_applyform" in result.stderr, result.stderr
    else:
        assert result.returncode == 0, result.stderr
    cleanup = query(f"SELECT to_regnamespace('{schema}') IS NULL;")
    assert cleanup.returncode == 0 and cleanup.stdout.strip() == "t", "Test schema did not roll back"
    print(name + ": PASS (test schema rolled back)")
    return result.stdout.splitlines()


check("constraint name, duplicates and independent keys", """
    INSERT INTO applicants VALUES ('a', 'user-a', 'form-a');
""" + MIGRATION + """
    DO $$
    DECLARE violated_constraint text;
    BEGIN
        BEGIN
            INSERT INTO applicants VALUES ('duplicate', 'user-a', 'form-a');
            RAISE EXCEPTION 'Duplicate unexpectedly persisted';
        EXCEPTION WHEN unique_violation THEN
            GET STACKED DIAGNOSTICS violated_constraint = CONSTRAINT_NAME;
            IF violated_constraint <> 'uk_applicants_user_email_applyform' THEN
                RAISE EXCEPTION 'Wrong constraint: %', violated_constraint;
            END IF;
        END;
    END $$;
    INSERT INTO applicants VALUES ('b', 'user-b', 'form-a'), ('c', 'user-a', 'form-b');
    DO $$ BEGIN
        IF (SELECT count(*) FROM applicants) <> 3 THEN
            RAISE EXCEPTION 'Original or independent applications were lost';
        END IF;
    END $$;
""")

check("existing duplicates reject migration atomically", """
    INSERT INTO applicants VALUES ('a', 'user-a', 'form-a'), ('b', 'user-a', 'form-a');
""" + MIGRATION, expected_error=True)

rows = check("read-only preflight detects duplicates and NULL", """
    INSERT INTO applicants VALUES ('a', 'user-a', 'form-a'), ('b', 'user-a', 'form-a'),
        ('c', NULL, 'form-b');
""" + PREFLIGHT)
assert rows[:2] == ["user-a|form-a|2", "1"], rows
