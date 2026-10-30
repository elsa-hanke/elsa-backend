import { createHash } from 'node:crypto'
import { dbClient, withDb } from './db-client'
import { ensureYliopistoErikoisalaLink } from './kayttaja-utils'
import { fetchUserIdsByEmailOrLogin } from './user-utils'
import { E2E_ERIKOISTUVA_EMAIL, VASTUUHENKILO_EMAIL } from '../../support/commands/credentials'

const hash = (bytes: Buffer) => createHash('sha256').update(bytes).digest('hex')

export const asyncArchiveTasks = {
  async 'archive:read'({
    key,
    sourceIds = {}
  }: {
    key: string
    sourceIds?: Record<string, number>
  }) {
    if (!/^(koejakso-sopimus|koejakso-arvio|valmistuminen):[A-Z_]+:\d+$/.test(key)) {
      throw new Error('Invalid archive business key')
    }
    return withDb(dbClient, async (client) => {
      const jobs = await client.query('SELECT * FROM arkistointi_job WHERE job_key = $1', [key])
      if (!jobs.rows.length) return null
      const job = jobs.rows[0]
      const metadata = JSON.parse(job.payload)
      const identities = metadata.documents as { snapshotDocumentId: number; sourceDocumentId: number }[]
      const schedule = await client.query(
        `SELECT task_instance, picked, execution_time, execution_time <= CURRENT_TIMESTAMP AS due,
        last_success, last_failure
        FROM scheduled_tasks WHERE task_name = 'arkistointi-job' AND task_instance = $1`,
        [job.id]
      )
      const manifest = await client.query(
        `SELECT r.asiakirja_id, r.asiakirjatyyppi, r.jarjestys, r.sha256,
        r.sisaltotyyppi, a.opintooikeus_id, a.nimi, a.archive_snapshot, d.data
        FROM arkistointi_job_asiakirja r JOIN asiakirja a ON a.id = r.asiakirja_id
        JOIN asiakirja_data d ON d.id = a.asiakirja_data_id
        WHERE r.job_id = $1 ORDER BY r.jarjestys`,
        [job.id]
      )
      const documents = []
      for (const row of manifest.rows) {
        const type = row.asiakirjatyyppi as string
        const sourceId = sourceIds[type] ?? identities.find(
          (identity) => identity.snapshotDocumentId === Number(row.asiakirja_id)
        )?.sourceDocumentId
        const source = await client.query(
          `SELECT d.data FROM asiakirja a
          JOIN asiakirja_data d ON d.id = a.asiakirja_data_id
          WHERE NOT a.archive_snapshot AND a.opintooikeus_id = $1 AND a.nimi = $2
          AND a.id = $3`,
          [row.opintooikeus_id, row.nimi, sourceId]
        )
        documents.push({
          id: Number(row.asiakirja_id),
          type,
          order: row.jarjestys,
          contentType: row.sisaltotyyppi,
          size: row.data.length,
          sha256: hash(row.data),
          storedSha256: row.sha256,
          snapshot: row.archive_snapshot,
          sourceMatches: source.rows.some((s) => Buffer.isBuffer(s.data) && s.data.equals(row.data))
        })
      }
      return {
        count: jobs.rows.length,
        id: job.id,
        university: job.university,
        caseType: job.case_type,
        status: job.status,
        expectedDocumentCount: job.expected_document_count,
        externalId: job.external_id,
        completedAt: job.completed_at,
        metadata,
        schedule: schedule.rows.map((row) => ({
          jobId: row.task_instance,
          picked: row.picked,
          executionTime: row.execution_time,
          due: row.due,
          lastSuccess: row.last_success,
          lastFailure: row.last_failure
        })),
        documents
      }
    })
  },

  async 'archive:prepareEhl'({
    email,
    approverEmail
  }: {
    email: string
    approverEmail: string
  }): Promise<{ specialtyId: number }> {
    if (email !== E2E_ERIKOISTUVA_EMAIL || approverEmail !== VASTUUHENKILO_EMAIL) {
      throw new Error('Only the disposable E2E identities may be changed')
    }
    return withDb(dbClient, async (client) => {
      await client.query('BEGIN')
      try {
        const specialty = await client.query(
          "SELECT id, nimi FROM erikoisala WHERE tyyppi = 'HAMMASLAAKETIEDE' ORDER BY id LIMIT 1"
        )
        if (specialty.rows.length !== 1) throw new Error('Missing EHL specialty fixture')
        const specialtyId = Number(specialty.rows[0].id)
        // Clone the active E2E study right's guide, retaining its complete requirements.
        const guide = await client.query(
          `INSERT INTO opintoopas
          SELECT (jsonb_populate_record(NULL::opintoopas, to_jsonb(p) || jsonb_build_object(
            'id', nextval(pg_get_serial_sequence('opintoopas', 'id')), 'erikoisala_id', $2::bigint,
            'nimi', $3::text))).*
          FROM opintoopas p JOIN opintooikeus o ON o.opintoopas_id = p.id
          JOIN erikoistuva_laakari el ON el.id = o.erikoistuva_laakari_id
          JOIN kayttaja k ON k.id = el.kayttaja_id JOIN jhi_user u ON u.id = k.user_id
          WHERE u.email = $1 AND o.kaytossa RETURNING id`,
          [email, specialtyId, `E2E ${specialty.rows[0].nimi}`]
        )
        if (guide.rows.length !== 1) throw new Error('Missing active E2E study guide fixture')
        const right = await client.query(
          `UPDATE opintooikeus o SET erikoisala_id = $3, opintoopas_id = $2
          FROM erikoistuva_laakari el JOIN kayttaja k ON k.id = el.kayttaja_id
          JOIN jhi_user u ON u.id = k.user_id
          WHERE u.email = $1 AND o.erikoistuva_laakari_id = el.id AND o.kaytossa RETURNING o.id, o.yliopisto_id`,
          [email, guide.rows[0].id, specialtyId]
        )
        if (right.rows.length !== 1) throw new Error('Expected one active E2E study right')
        const approver = await fetchUserIdsByEmailOrLogin(client, approverEmail)
        if (!approver) throw new Error('Missing E2E approver')
        const link = await ensureYliopistoErikoisalaLink(
          client,
          approver.kayttajaId,
          right.rows[0].yliopisto_id,
          specialtyId
        )
        await client.query(
          `INSERT INTO rel_kayttaja_yliopisto_erikoisala__tehtavatyyppi
          (kayttaja_yliopisto_erikoisala_id, vastuuhenkilon_tehtavatyyppi_id)
          SELECT $1, id FROM vastuuhenkilon_tehtavatyyppi
          WHERE nimi IN ('VALMISTUMISPYYNNON_OSAAMISEN_ARVIOINTI', 'VALMISTUMISPYYNNON_HYVAKSYNTA')
          ON CONFLICT DO NOTHING`,
          [link]
        )
        await client.query('COMMIT')
        return { specialtyId }
      } catch (error) {
        await client.query('ROLLBACK')
        throw error
      }
    })
  }
}
