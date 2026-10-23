import { randomUUID } from 'node:crypto'
import { dbClient, withDb } from './db-client'
import { cleanupUser, createUserWithRole, createVerificationToken, fetchUserIdsByEmailOrLogin } from './user-utils'
import { ensureYliopistoErikoisalaLink } from './kayttaja-utils'

const users = {
  admin: { email: 'e2e-rich-text-admin@test.elsa', role: 'ROLE_TEKNINEN_PAAKAYTTAJA',
    etunimi: 'Daniel', sukunimi: 'Siekkinen' },
  trainer: { email: 'e2e-rich-text-trainer@test.elsa', role: 'ROLE_KOULUTTAJA',
    etunimi: 'Lassekalevi', sukunimi: 'Hummaamistes' }
}
type UserKind = keyof typeof users
function userConfig(kind: UserKind) {
  if (kind !== 'admin' && kind !== 'trainer') throw new Error('Unknown rich-text user fixture')
  return users[kind]
}
// Node-side ownership survives browser navigation and failed assertions.
const announcements = new Set<number>()
const marker = `E2E-rich-text-${randomUUID()}`
const detachedIdentities = new Map<UserKind, { id: string; hetu: Buffer }>()

export const richTextTasks = {
  async 'db:createLegacyRichTextAnnouncement'(html: string): Promise<number> {
    return withDb(dbClient, async (db) => {
      // INSERT, never UPDATE: existing Hibernate cache entries must not mask the fixture.
      const result = await db.query('INSERT INTO ilmoitus (teksti) VALUES ($1) RETURNING id', [html])
      const id = Number(result.rows[0].id)
      announcements.add(id)
      return id
    })
  },

  'db:richTextMarker'(): string {
    return marker
  },

  async 'db:readRichTextAnnouncement'(id: number): Promise<string> {
    return withDb(dbClient, async (db) => {
      const result = await db.query('SELECT teksti FROM ilmoitus WHERE id = $1', [id])
      if (result.rows.length !== 1) throw new Error('Expected one rich-text announcement')
      const html: string = result.rows[0].teksti
      // API-created rows are owned only if they contain this process's unguessable marker.
      if (!announcements.has(id) && !html.includes(marker)) throw new Error('Not a rich-text fixture')
      announcements.add(id)
      return html
    })
  },

  async 'db:cleanupRichTextAnnouncements'(): Promise<null> {
    return withDb(dbClient, async (db) => {
      // The marker also catches API writes whose HTTP response/assertion failed.
      await db.query('DELETE FROM ilmoitus WHERE id = ANY($1::bigint[]) OR position($2 in teksti) > 0', [
        [...announcements], marker
      ])
      announcements.clear()
      return null
    })
  },

  async 'db:seedRichTextUser'(kind: UserKind): Promise<{ token: string }> {
    const config = userConfig(kind)
    return withDb(dbClient, async (db) => {
      await db.query('BEGIN')
      try {
        // Specs run serially within a shard and each GHA shard owns its DB.
        // Temporarily detach the shared IdP identity to prevent automatic account
        // merging. Preserve its encrypted value only in Node memory, never logs.
        const linked = await db.query(
          `SELECT id, hetu FROM jhi_user WHERE first_name = $1 AND last_name = $2
           AND hetu IS NOT NULL`, [config.etunimi, config.sukunimi]
        )
        if (linked.rows.length > 1) throw new Error('Ambiguous rich-text test identity')
        const identity = linked.rows[0] as { id: string; hetu: Buffer } | undefined
        if (await fetchUserIdsByEmailOrLogin(db, config.email)) throw new Error('Clean the rich-text user before seeding')
        if (identity) await db.query('UPDATE jhi_user SET hetu = NULL WHERE id = $1', [identity.id])
        await db.query('INSERT INTO jhi_authority (name) VALUES ($1) ON CONFLICT DO NOTHING', [config.role])
        await createUserWithRole(db, {
          ...config, linkFn: kind === 'trainer' ? ensureYliopistoErikoisalaLink : async () => 0
        })
        const user = await fetchUserIdsByEmailOrLogin(db, config.email)
        const token = await createVerificationToken(db, user!.userId)
        await db.query('COMMIT')
        if (identity) detachedIdentities.set(kind, identity)
        return { token }
      } catch (error) {
        await db.query('ROLLBACK')
        throw error
      }
    })
  },

  async 'db:cleanupRichTextUser'(kind: UserKind): Promise<null> {
    const config = userConfig(kind)
    return withDb(dbClient, async (db) => {
      await db.query('BEGIN')
      try {
        const user = await fetchUserIdsByEmailOrLogin(db, config.email)
        if (user) await cleanupUser(db, user.userId, user.kayttajaId, async (client, id) => {
          await client.query('DELETE FROM kayttaja_yliopisto_erikoisala WHERE kayttaja_id = $1', [id])
        })
        const detachedIdentity = detachedIdentities.get(kind)
        if (detachedIdentity) {
          const restored = await db.query('UPDATE jhi_user SET hetu = $1 WHERE id = $2 AND hetu IS NULL', [
            detachedIdentity.hetu, detachedIdentity.id
          ])
          if (restored.rowCount !== 1) throw new Error('Cannot restore the shared test identity')
        }
        await db.query('COMMIT')
        detachedIdentities.delete(kind)
        return null
      } catch (error) {
        await db.query('ROLLBACK')
        throw error
      }
    })
  }
}
