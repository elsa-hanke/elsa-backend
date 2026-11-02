-- psql -v job_id=UUID -v step_key=CASE -v remote_id=415 -v action=confirm -f ops/arkistointi/dynasty-resolve-step.sql
-- Pass remote_id='' for ARCHIVE_CASE and for action=discard. All four variables are required.
-- Confirm IDs only after finding the corresponding object in Dynasty.
-- Discard only after confirming that nothing was created; stop/drain the old worker first.
-- Database fencing cannot cancel an HTTP request already in flight.
\set ON_ERROR_STOP on
BEGIN;
CREATE TEMP TABLE dynasty_resolution_input ON COMMIT DROP AS
SELECT :'job_id'::uuid AS job_id, :'step_key'::text AS step_key,
       :'remote_id'::text AS remote_id, :'action'::text AS action;

DO $$
DECLARE
    input record;
    job_status text;
    task_picked boolean;
    task_time timestamptz;
    step_status text;
    changed integer;
BEGIN
    SELECT * INTO STRICT input FROM pg_temp.dynasty_resolution_input;
    IF input.action NOT IN ('confirm', 'discard') THEN
        RAISE EXCEPTION 'Dynasty resolve: action must be confirm or discard';
    END IF;
    IF input.step_key NOT IN ('CASE', 'TASK', 'ARCHIVE_CASE')
       AND input.step_key !~ '^DOCUMENT:(0|[1-9][0-9]*)$' THEN
        RAISE EXCEPTION 'Dynasty resolve: invalid step_key';
    END IF;
    IF length(input.step_key) > 50 OR length(input.remote_id) > 255 THEN
        RAISE EXCEPTION 'Dynasty resolve: step_key or remote_id exceeds its storage limit';
    END IF;
    IF input.action = 'discard' OR input.step_key = 'ARCHIVE_CASE' THEN
        IF input.remote_id <> '' THEN
            RAISE EXCEPTION 'Dynasty resolve: remote_id must be empty for discard or ARCHIVE_CASE';
        END IF;
    ELSIF input.step_key = 'TASK' THEN
        IF input.remote_id !~* '^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$' THEN
            RAISE EXCEPTION 'Dynasty resolve: TASK requires a canonical UUID remote_id';
        END IF;
    ELSE
        IF input.remote_id !~ '^[0-9]+$' THEN
            RAISE EXCEPTION 'Dynasty resolve: CASE and DOCUMENT require a positive bigint remote_id';
        END IF;
        IF input.remote_id::numeric NOT BETWEEN 1 AND 9223372036854775807 THEN
            RAISE EXCEPTION 'Dynasty resolve: CASE and DOCUMENT require a positive bigint remote_id';
        END IF;
    END IF;

    -- Same lock order as workers: scheduler -> job -> step. Never recreate a missing task.
    SELECT picked, execution_time INTO task_picked, task_time
    FROM scheduled_tasks
    WHERE task_name = 'arkistointi-job' AND task_instance = input.job_id::text
    FOR UPDATE;
    IF NOT FOUND THEN
        RAISE EXCEPTION 'Dynasty resolve: archive scheduler task is missing';
    END IF;
    IF task_picked IS DISTINCT FROM false
       OR task_time IS DISTINCT FROM TIMESTAMPTZ '2500-01-01 12:00:00+00' THEN
        RAISE EXCEPTION 'Dynasty resolve: scheduler task must be parked and unpicked';
    END IF;
    SELECT status INTO job_status FROM arkistointi_job WHERE id = input.job_id FOR UPDATE;
    IF NOT FOUND OR job_status IS DISTINCT FROM 'VAATII_TOIMENPITEITA' THEN
        RAISE EXCEPTION 'Dynasty resolve: job must exist and require intervention';
    END IF;
    IF input.step_key LIKE 'DOCUMENT:%' AND NOT EXISTS (
        SELECT 1 FROM arkistointi_job_asiakirja
        WHERE job_id = input.job_id AND jarjestys::text = substring(input.step_key FROM 10)
    ) THEN
        RAISE EXCEPTION 'Dynasty resolve: document step is not in the job manifest';
    END IF;
    SELECT status INTO step_status FROM arkistointi_dynasty_step
    WHERE job_id = input.job_id AND step_key = input.step_key FOR UPDATE;
    IF NOT FOUND OR step_status IS DISTINCT FROM 'ALOITETTU' THEN
        RAISE EXCEPTION 'Dynasty resolve: step must exist and be ALOITETTU';
    END IF;

    IF input.action = 'confirm' THEN
        UPDATE arkistointi_dynasty_step SET status = 'VALMIS',
            remote_id = NULLIF(input.remote_id, ''), completed_at = CURRENT_TIMESTAMP
        WHERE job_id = input.job_id AND step_key = input.step_key AND status = 'ALOITETTU';
    ELSE
        DELETE FROM arkistointi_dynasty_step
        WHERE job_id = input.job_id AND step_key = input.step_key AND status = 'ALOITETTU';
    END IF;
    GET DIAGNOSTICS changed = ROW_COUNT;
    IF changed <> 1 THEN
        RAISE EXCEPTION 'Dynasty resolve: expected exactly one changed step, got %', changed;
    END IF;
END;
$$;
SELECT job_id, step_key, action, 'resolved' AS result FROM pg_temp.dynasty_resolution_input;
COMMIT;
-- Only after successful COMMIT, use retry-job.sql to resume this same job.
