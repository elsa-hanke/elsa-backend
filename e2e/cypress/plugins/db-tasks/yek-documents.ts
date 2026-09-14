import { dbClient, withDb } from './db-client'
import { E2E_ERIKOISTUVA_EMAIL } from '../../support/commands/credentials'

const YEK_ROLE = 'ROLE_YEK_KOULUTETTAVA'

export const yekDocumentTasks = {
  async 'db:prepareYekOnlyDocumentUser'(): Promise<null> {
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

