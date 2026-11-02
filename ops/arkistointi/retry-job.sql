-- psql -v ON_ERROR_STOP=1 -v job_id=<UUID> -f ops/arkistointi/retry-job.sql
-- Only parked tasks are eligible. Never recreate a missing scheduler row here.
WITH input AS MATERIALIZED (
    SELECT :'job_id'::uuid AS id
), locked_task AS MATERIALIZED (
    SELECT s.task_name, s.task_instance, s.version
    FROM scheduled_tasks s, input i
    WHERE s.task_name = 'arkistointi-job' AND s.task_instance = i.id::text
      AND s.picked = false AND s.execution_time = TIMESTAMPTZ '2500-01-01 12:00:00+00'
    FOR UPDATE OF s
), restarted_job AS (
    UPDATE arkistointi_job j
    SET status = 'ODOTTAA', completed_at = NULL, updated_at = now()
    FROM input i, locked_task s
    WHERE j.id = i.id AND j.status = 'VAATII_TOIMENPITEITA'
    RETURNING j.id
)
UPDATE scheduled_tasks s
SET execution_time = now(), picked = false, picked_by = NULL, last_heartbeat = NULL,
    consecutive_failures = 0, last_failure = NULL, last_success = NULL, version = s.version + 1
FROM locked_task old, restarted_job j
WHERE s.task_name = old.task_name AND s.task_instance = old.task_instance
  AND s.version = old.version AND s.task_instance = j.id::text
RETURNING s.task_instance, s.version, s.execution_time;
