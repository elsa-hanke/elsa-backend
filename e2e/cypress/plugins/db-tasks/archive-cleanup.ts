import type { Client } from 'pg'
import { dbClient, withDb } from './db-client'

/** Read through the caller's connection, then use the existing isolated cleanup transaction. */
export async function cleanupArchiveForKayttaja(client: Client, kayttajaId: number): Promise<void> {
  const rights = await client.query<{ id: string | number }>(
    `SELECT o.id FROM opintooikeus o JOIN erikoistuva_laakari el ON el.id = o.erikoistuva_laakari_id
     WHERE el.kayttaja_id = $1`, [kayttajaId]
  )
  await cleanupArchiveForStudyRights(rights.rows.map((row) => Number(row.id)))
}

/** erikoistuvaId identifies erikoistuva_laakari, not kayttaja or jhi_user. */
export async function cleanupArchiveForErikoistuva(client: Client, erikoistuvaId: number): Promise<void> {
  const rights = await client.query<{ id: string | number }>(
    'SELECT id FROM opintooikeus WHERE erikoistuva_laakari_id = $1', [erikoistuvaId]
  )
  await cleanupArchiveForStudyRights(rights.rows.map((row) => Number(row.id)))
}

/** Remove only the selected E2E study rights' archive data from the disposable DB. */
export async function cleanupArchiveForStudyRights(studyRightIds: number[]): Promise<void> {
  if (!studyRightIds.length) return
  // Own connection/transaction: callers may already have an ordinary cleanup transaction.
  // SET LOCAL is reverted by COMMIT/ROLLBACK before that cleanup continues.
  await withDb(dbClient, async (client) => {
    await client.query('BEGIN')
    try {
      try {
        await client.query('SET LOCAL session_replication_role = replica')
      } catch (error) {
        if (error && typeof error === 'object' && 'code' in error && error.code === '42501') {
          throw new Error(
            'E2E archive cleanup requires permission to SET session_replication_role in the disposable PostgreSQL database. ' +
            'Check CYPRESS_DB_* and use the disposable Compose DB role, or a dedicated test role with SET permission ' +
            'on this parameter. See e2e/README.md. Do not grant this permission to a production application role.'
          )
        }
        throw error
      }
      // Replica mode disables FK checks and CASCADE too; delete every dependent row explicitly.
      await client.query(
        `WITH snapshots AS (
          SELECT id, asiakirja_data_id FROM asiakirja
          WHERE archive_snapshot AND opintooikeus_id = ANY($1::bigint[])
        ), jobs AS (
          SELECT DISTINCT job_id FROM arkistointi_job_asiakirja
          WHERE asiakirja_id IN (SELECT id FROM snapshots)
        ), deleted_tasks AS (
          DELETE FROM scheduled_tasks WHERE task_name = 'arkistointi-job'
          AND task_instance IN (SELECT job_id::text FROM jobs)
        ), deleted_manifest AS (
          DELETE FROM arkistointi_job_asiakirja WHERE job_id IN (SELECT job_id FROM jobs)
        ), deleted_jobs AS (
          DELETE FROM arkistointi_job WHERE id IN (SELECT job_id FROM jobs)
        ), deleted_snapshots AS (
          DELETE FROM asiakirja WHERE id IN (SELECT id FROM snapshots)
        )
        DELETE FROM asiakirja_data d WHERE d.id IN (SELECT asiakirja_data_id FROM snapshots)
        AND NOT EXISTS (
          SELECT 1 FROM asiakirja a WHERE a.asiakirja_data_id = d.id
          AND a.id NOT IN (SELECT id FROM snapshots)
        )`,
        [studyRightIds]
      )
      await client.query('COMMIT')
    } catch (error) {
      await client.query('ROLLBACK')
      throw error
    }
  })
}
