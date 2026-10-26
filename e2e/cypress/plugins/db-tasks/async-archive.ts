import { createHash } from 'node:crypto'
import { dbClient, withDb } from './db-client'
import { ensureYliopistoErikoisalaLink } from './kayttaja-utils'
import { fetchUserIdsByEmailOrLogin } from './user-utils'
import { E2E_ERIKOISTUVA_EMAIL, VASTUUHENKILO_EMAIL } from '../../support/commands/credentials'

const hash = (bytes: Buffer) => createHash('sha256').update(bytes).digest('hex')

export const asyncArchiveTasks = {
  async 'archive:assertFreshStack'(): Promise<null> {
    return withDb(dbClient, async (client) => {
      // The fixture schema exists only when the separate E2E jar and profile are active.
      const result = await client.query(`SELECT
        (SELECT count(*) FROM archive_e2e.receipt) AS receipts,
        (SELECT count(*) FROM arkistointi_job) AS jobs,
        (SELECT count(*) FROM asiakirja WHERE archive_snapshot) AS snapshots`)
      if (Object.values(result.rows[0]).some((count) => Number(count) !== 0)) {
        throw new Error(
          'Async archive specs require a fresh disposable stack per scenario; recreate it before retrying.'
        )
      }
      return null
    })
  },

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
      const receipt = await client.query('SELECT * FROM archive_e2e.receipt WHERE job_id = $1', [
        job.id
      ])
      const schedule = await client.query(
        "SELECT count(*) AS count FROM scheduled_tasks WHERE task_name = 'arkistointi-job' AND task_instance = $1",
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
        const source =
          type === 'LAILLISTAMISTODISTUS'
            ? await client.query(
                `SELECT d.data FROM opintooikeus o
              JOIN erikoistuva_laakari el ON el.id = o.erikoistuva_laakari_id
              JOIN asiakirja_data d ON d.id = el.laillistamistodistus_id WHERE o.id = $1`,
                [row.opintooikeus_id]
              )
            : await client.query(
                `SELECT d.data FROM asiakirja a
              JOIN asiakirja_data d ON d.id = a.asiakirja_data_id
              WHERE NOT a.archive_snapshot AND a.opintooikeus_id = $1 AND a.nimi = $2
              AND ($3::bigint IS NULL OR a.id = $3)`,
                [row.opintooikeus_id, row.nimi, sourceIds[type] ?? null]
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
        scheduled: Number(schedule.rows[0].count),
        documents,
        receipt: receipt.rows[0] ?? null
      }
    })
  },

  async 'archive:prepareEhl'({
    email,
    approverEmail
  }: {
    email: string
    approverEmail: string
  }): Promise<null> {
    if (email !== E2E_ERIKOISTUVA_EMAIL || approverEmail !== VASTUUHENKILO_EMAIL) {
      throw new Error('Only the disposable E2E identities may be changed')
    }
    return withDb(dbClient, async (client) => {
      await client.query('BEGIN')
      try {
        await client.query('SELECT 1 FROM archive_e2e.receipt LIMIT 1')
        // Seed a complete guide from the existing fixture, changing both its specialty and title.
        const guide = await client.query(`INSERT INTO opintoopas
          SELECT (jsonb_populate_record(NULL::opintoopas, to_jsonb(p) || jsonb_build_object(
            'id', nextval(pg_get_serial_sequence('opintoopas', 'id')), 'erikoisala_id', 54,
            'nimi', 'E2E Hampaiston oikomishoito'))).* FROM opintoopas p WHERE p.id = 15 RETURNING id`)
        if (guide.rows.length !== 1) throw new Error('Missing EHL guide fixture')
        const right = await client.query(
          `UPDATE opintooikeus o SET erikoisala_id = 54, opintoopas_id = $2
          FROM erikoistuva_laakari el JOIN kayttaja k ON k.id = el.kayttaja_id
          JOIN jhi_user u ON u.id = k.user_id
          WHERE u.email = $1 AND o.erikoistuva_laakari_id = el.id AND o.kaytossa RETURNING o.id, o.yliopisto_id`,
          [email, guide.rows[0].id]
        )
        if (right.rows.length !== 1) throw new Error('Expected one active E2E study right')
        const approver = await fetchUserIdsByEmailOrLogin(client, approverEmail)
        if (!approver) throw new Error('Missing E2E approver')
        const link = await ensureYliopistoErikoisalaLink(
          client,
          approver.kayttajaId,
          right.rows[0].yliopisto_id,
          54
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
        return null
      } catch (error) {
        await client.query('ROLLBACK')
        throw error
      }
    })
  }
}
