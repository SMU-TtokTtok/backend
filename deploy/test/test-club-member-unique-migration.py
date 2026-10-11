"""Validate #432 SQL only against the dedicated disposable PostgreSQL container."""
from pathlib import Path
import subprocess
import uuid

ROOT = Path(__file__).resolve().parents[2]
PENDING = ROOT / "deploy/migrations/pending"
MIGRATION = (PENDING / "add_club_member_unique_constraint.sql").read_text(encoding="utf-8")
PREFLIGHT = (PENDING / "check_club_member_duplicates.sql").read_text(encoding="utf-8")


def query(sql):
    return subprocess.run([
        "docker", "exec", "-i", "ttokttok-426-repro-pg", "psql",
        "-U", "postgres", "-d", "ttokttok426", "-qAt",
        "-v", "ON_ERROR_STOP=1", "-v", "VERBOSITY=verbose",
    ], input=sql, capture_output=True, text=True, encoding="utf-8")


def check(name, body, expected_error=False):
    schema = "migration432_" + uuid.uuid4().hex
    setup = f"""
        BEGIN;
        CREATE SCHEMA {schema};
        SET LOCAL search_path TO {schema};
        CREATE TABLE club_members (id text PRIMARY KEY, club_id text, email text);
    """
    result = query(setup + body + "\nROLLBACK;")
    if expected_error:
        assert result.returncode != 0 and "23505" in result.stderr, result.stderr
        assert "uk_club_members_club_email" in result.stderr, result.stderr
    else:
        assert result.returncode == 0, result.stderr
    cleanup = query(f"SELECT to_regnamespace('{schema}') IS NULL;")
    assert cleanup.returncode == 0 and cleanup.stdout.strip() == "t", "Test schema did not roll back"
    print(name + ": PASS (test schema rolled back)")
    return result.stdout.splitlines()


check("constraint name, duplicates and independent keys", """
    INSERT INTO club_members VALUES ('a', 'club-a', 'x@sangmyung.kr');
""" + MIGRATION + """
    DO $$
    DECLARE violated_constraint text;
    BEGIN
        BEGIN
            INSERT INTO club_members VALUES ('duplicate', 'club-a', 'x@sangmyung.kr');
            RAISE EXCEPTION 'Duplicate unexpectedly persisted';
        EXCEPTION WHEN unique_violation THEN
            GET STACKED DIAGNOSTICS violated_constraint = CONSTRAINT_NAME;
            IF violated_constraint <> 'uk_club_members_club_email' THEN
                RAISE EXCEPTION 'Wrong constraint: %', violated_constraint;
            END IF;
        END;
    END $$;
    INSERT INTO club_members VALUES ('b', 'club-b', 'x@sangmyung.kr'), ('c', 'club-a', 'y@sangmyung.kr');
    DO $$ BEGIN
        IF (SELECT count(*) FROM club_members) <> 3 THEN
            RAISE EXCEPTION 'Original or independent members were lost';
        END IF;
    END $$;
""")

check("existing duplicates reject migration atomically", """
    INSERT INTO club_members VALUES ('a', 'club-a', 'x@sangmyung.kr'), ('b', 'club-a', 'x@sangmyung.kr');
""" + MIGRATION, expected_error=True)

rows = check("read-only preflight detects duplicates, NULL and case variants", """
    INSERT INTO club_members VALUES ('a', 'club-a', 'x@sangmyung.kr'), ('b', 'club-a', 'x@sangmyung.kr'),
        ('c', NULL, 'y@sangmyung.kr'), ('d', 'club-b', 'Z@sangmyung.kr'), ('e', 'club-b', 'z@sangmyung.kr');
""" + PREFLIGHT)
assert rows[:3] == ["club-a|x@sangmyung.kr|2", "1", "club-b|z@sangmyung.kr|2"], rows
