import { dbClient, withDb } from './db-client'
import { E2E_ERIKOISTUVA_EMAIL } from '../../support/commands/credentials'

const YEK_ROLE = 'ROLE_YEK_KOULUTETTAVA'

function assertLocalTestDatabase(): void {
  if (process.env.CYPRESS_YEK_DOCUMENTS_E2E_CONFIRMED !== 'yes') {
    throw new Error('Set CYPRESS_YEK_DOCUMENTS_E2E_CONFIRMED=yes only after verifying that both the app and DB use the disposable E2E database, not a replica.')
  }
  const host = process.env.CYPRESS_DB_HOST ?? 'localhost'
  const port = Number(process.env.CYPRESS_DB_PORT ?? '5432')
  const database = process.env.CYPRESS_DB_NAME ?? 'elsaBackend'
  if (!['localhost', '127.0.0.1'].includes(host) || port !== 5432 || database !== 'elsaBackend') {
    throw new Error('YEK document tests require the disposable local elsaBackend database on port 5432. Never use a replica.')
  }
}

export const yekDocumentTasks = {
  'db:assertLocalYekDocumentDatabase'(): null {
    assertLocalTestDatabase()
    return null
  },

  async 'db:prepareYekOnlyDocumentUser'(): Promise<null> {
    assertLocalTestDatabase()
    return withDb(dbClient, async (client) => {
      await client.query('BEGIN')
      try {
        // Only the shared, disposable E2E identity may be modified by this task.
        const result = await client.query<{ user_id: string; erikoistuva_id: number }>(
          `SELECT u.id AS user_id, el.id AS erikoistuva_id
           FROM jhi_user u
           JOIN kayttaja k ON k.user_id = u.id
           JOIN erikoistuva_laakari el ON el.kayttaja_id = k.id
           WHERE u.email = $1
             AND EXISTS (SELECT 1 FROM opintooikeus o
                         WHERE o.erikoistuva_laakari_id = el.id
                           AND o.erikoisala_id = 61 AND o.kaytossa = true)
             AND NOT EXISTS (SELECT 1 FROM opintooikeus o
                             WHERE o.erikoistuva_laakari_id = el.id AND o.erikoisala_id <> 61)`,
          [E2E_ERIKOISTUVA_EMAIL]
        )
        if (result.rows.length !== 1) {
          throw new Error('Convert the E2E user\'s only study right to YEK before preparing document tests.')
        }
        const { user_id, erikoistuva_id } = result.rows[0]
        await client.query(
          `INSERT INTO jhi_user_authority (user_id, authority_name) VALUES ($1, $2)
           ON CONFLICT DO NOTHING`,
          [user_id, YEK_ROLE]
        )
        await client.query('UPDATE jhi_user SET active_authority = $2 WHERE id = $1', [user_id, YEK_ROLE])
        await client.query(
          'DELETE FROM jhi_user_authority WHERE user_id = $1 AND authority_name <> $2',
          [user_id, YEK_ROLE]
        )
        // Keep unrelated licensing fields out of the attachment-editing scenario.
        await client.query(
          `UPDATE erikoistuva_laakari
           SET laillistamispaiva = '2019-01-01',
               laakarikoulutussuoritettusuomitaibelgia = true,
               laakarikoulutussuoritettumuukuinsuomitaibelgia = false
           WHERE id = $1`,
          [erikoistuva_id]
        )
        await client.query('COMMIT')
        return null
      } catch (error) {
        await client.query('ROLLBACK')
        throw error
      }
    })
  },
}

