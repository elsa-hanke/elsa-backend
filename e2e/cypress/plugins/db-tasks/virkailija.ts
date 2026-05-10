import type { Client } from 'pg'
import { dbClient, withDb } from './db-client'
import {
  fetchUserIdsByEmailOrLogin,
  ensureUserAuthority,
  createVerificationToken,
  createUserWithRole,
  cleanupUser,
} from './user-utils'
import { ensureYliopistoLink } from './kayttaja-utils'

const ROLE = 'ROLE_OPINTOHALLINNON_VIRKAILIJA'

// ─── Exported tasks ───────────────────────────────────────────────────────────

/**
 * Tehtävät opintohallinnon virkailijan testikäyttäjän siementämiseen ja siivoamiseen.
 *
 * db:seedVirkailija    – Luo ROLE_OPINTOHALLINNON_VIRKAILIJA-käyttäjän, joka on linkitetty
 *                        yliopistoon 1. Idempotentti.
 * db:cleanupVirkailija – Poistaa kaikki seedVirkailija:n luomat rivit.
 */
export const virkailijaTasks = {
  async 'db:seedVirkailija'({
    email,
    etunimi,
    sukunimi,
  }: {
    email: string
    etunimi: string
    sukunimi: string
  }): Promise<{ kayttajaId: number; token?: string } | null> {
    return withDb(dbClient, async (client: Client) => {
      await client.query(
        `INSERT INTO jhi_authority (name) VALUES ($1) ON CONFLICT DO NOTHING`,
        [ROLE]
      )

      const existing = await fetchUserIdsByEmailOrLogin(client, email)
      if (existing) {
        await ensureYliopistoLink(client, existing.kayttajaId)
        await ensureUserAuthority(client, existing.userId, ROLE)
        return { kayttajaId: existing.kayttajaId }
      }

      const kayttajaId = await createUserWithRole(client, {
        email,
        etunimi,
        sukunimi,
        role: ROLE,
        linkFn: ensureYliopistoLink,
      })
      const created = await fetchUserIdsByEmailOrLogin(client, email)
      const token = await createVerificationToken(client, created!.userId)
      return { kayttajaId, token }
    })
  },

  async 'db:cleanupVirkailija'({ email }: { email: string }): Promise<null> {
    return withDb(dbClient, async (client: Client) => {
      const ids = await fetchUserIdsByEmailOrLogin(client, email)
      if (!ids) return null
      const { userId, kayttajaId } = ids
      // Clean up link and user
      await cleanupUser(
        client,
        userId,
        kayttajaId,
        async (client, kayttajaId) => {
          await client.query(
            `DELETE FROM rel_kayttaja__yliopisto WHERE kayttaja_id = $1`,
            [kayttajaId]
          )
        }
      )
      return null
    })
  },
}
