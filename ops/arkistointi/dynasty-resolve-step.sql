-- psql -v job_id=UUID -v step_key=CASE -v remote_id=415 -v action=confirm -f dynasty-resolve-step.sql
-- action=discard is allowed only after confirming in Dynasty that the object was NOT created.
-- Stop/drain the old worker before discarding a step: it may still return a late response.
BEGIN;
SELECT task_name FROM scheduled_tasks WHERE task_instance=:'job_id' FOR UPDATE;
SELECT id FROM arkistointi_job WHERE id=:'job_id'::uuid FOR UPDATE;
UPDATE arkistointi_dynasty_step s SET status='VALMIS',remote_id=NULLIF(:'remote_id',''),completed_at=CURRENT_TIMESTAMP
WHERE s.job_id=:'job_id'::uuid AND s.step_key=:'step_key' AND s.status='ALOITETTU' AND :'action'='confirm'
AND (NULLIF(:'remote_id','') IS NOT NULL OR s.step_key='ARCHIVE_CASE')
AND EXISTS (SELECT 1 FROM arkistointi_job j WHERE j.id=s.job_id AND j.status='VAATII_TOIMENPITEITA');
DELETE FROM arkistointi_dynasty_step s WHERE s.job_id=:'job_id'::uuid AND s.step_key=:'step_key' AND s.status='ALOITETTU'
AND :'action'='discard' AND EXISTS (SELECT 1 FROM arkistointi_job j WHERE j.id=s.job_id AND j.status='VAATII_TOIMENPITEITA');
COMMIT;
-- Inspect the affected row count and then use the existing controlled retry-job.sql.
