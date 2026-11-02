-- psql -v job_id=<UUID> -f ops/arkistointi/retry-job.sql
-- For ARCHIVE_EXECUTION_LOST, stop/drain the old sender and reconcile remote receipt before retrying.
-- Only parked tasks are eligible. Never recreate a missing scheduler row here.
-- error_code/last_error retain the last failure until success or a later failure replaces it.
\set ON_ERROR_STOP on
BEGIN;
CREATE TEMP TABLE archive_retry_input ON COMMIT DROP AS
SELECT :'job_id'::uuid AS job_id;

DO $$
DECLARE
    input_id uuid;
    task_picked boolean;
    task_time timestamptz;
    task_version bigint;
    job_status text;
    changed integer;
BEGIN
    SELECT job_id INTO STRICT input_id FROM pg_temp.archive_retry_input;
    -- Same lock order as workers: scheduler -> job. Never insert a replacement task.
    SELECT picked, execution_time, version INTO task_picked, task_time, task_version
    FROM scheduled_tasks
    WHERE task_name = 'arkistointi-job' AND task_instance = input_id::text
    FOR UPDATE;
    IF NOT FOUND THEN
        RAISE EXCEPTION 'Archive retry: scheduler task is missing; no replacement was created';
    END IF;
    IF task_picked IS DISTINCT FROM false THEN
        RAISE EXCEPTION 'Archive retry: scheduler task is active (picked); retry refused';
    END IF;
    IF task_time IS DISTINCT FROM TIMESTAMPTZ '2500-01-01 12:00:00+00' THEN
        RAISE EXCEPTION 'Archive retry: scheduler task is not parked at Schedule.NEVER; retry refused';
    END IF;
    SELECT status INTO job_status FROM arkistointi_job WHERE id = input_id FOR UPDATE;
    IF NOT FOUND THEN
        RAISE EXCEPTION 'Archive retry: archive job is missing';
    END IF;
    IF job_status IS DISTINCT FROM 'VAATII_TOIMENPITEITA' THEN
        RAISE EXCEPTION 'Archive retry: job must require intervention (VAATII_TOIMENPITEITA), actual status: %', job_status;
    END IF;

    UPDATE arkistointi_job
    SET status = 'ODOTTAA', completed_at = NULL, updated_at = clock_timestamp()
    WHERE id = input_id AND status = 'VAATII_TOIMENPITEITA';
    GET DIAGNOSTICS changed = ROW_COUNT;
    IF changed <> 1 THEN
        RAISE EXCEPTION 'Archive retry: expected exactly one changed job, got %', changed;
    END IF;
    UPDATE scheduled_tasks
    SET execution_time = clock_timestamp(), picked = false, picked_by = NULL, last_heartbeat = NULL,
        consecutive_failures = 0, last_failure = NULL, last_success = NULL, version = version + 1
    WHERE task_name = 'arkistointi-job' AND task_instance = input_id::text AND version = task_version;
    GET DIAGNOSTICS changed = ROW_COUNT;
    IF changed <> 1 THEN
        RAISE EXCEPTION 'Archive retry: expected exactly one changed scheduler task, got %', changed;
    END IF;
END;
$$;
SELECT s.task_instance, s.version, s.execution_time, 'retry scheduled' AS result
FROM scheduled_tasks s, pg_temp.archive_retry_input i
WHERE s.task_name = 'arkistointi-job' AND s.task_instance = i.job_id::text;
COMMIT;
