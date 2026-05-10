import type { Client } from 'pg'
import { dbClient, withDb } from './db-client'
import {
  fetchUserIdsByEmailOrLogin,
  ensureUserAuthority,
  createVerificationToken,
  createUserWithRole,
  cleanupUser,
} from './user-utils'
import { ensureYliopistoErikoisalaLink } from './kayttaja-utils'

const ROLE = 'ROLE_KOULUTTAJA'

// ─── Exported tasks ───────────────────────────────────────────────────────────

/**
 * Tasks for seeding and cleaning up the kouluttaja (trainer) test user.
 *
 * db:seedKouluttaja    – Creates a ROLE_KOULUTTAJA user linked to yliopisto 1 /
 *                        erikoisala 46 (Työterveyshuolto). Idempotent.
 * db:cleanupKouluttaja – Removes all rows created by seedKouluttaja. Safe to
 *                        call even if the user does not exist.
 */
export const kouluttajaTasks = {
  async 'db:seedKouluttaja'({
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
        await ensureYliopistoErikoisalaLink(client, existing.kayttajaId)
        await ensureUserAuthority(client, existing.userId, ROLE)
        return { kayttajaId: existing.kayttajaId }
      }

      const kayttajaId = await createUserWithRole(client, {
        email,
        etunimi,
        sukunimi,
        role: ROLE,
        linkFn: ensureYliopistoErikoisalaLink,
      })
      const created = await fetchUserIdsByEmailOrLogin(client, email)
      const token = await createVerificationToken(client, created!.userId)
      return { kayttajaId, token }
    })
  },

  async 'db:cleanupKouluttaja'({ email }: { email: string }): Promise<null> {
    return withDb(dbClient, async (client: Client) => {
      const ids = await fetchUserIdsByEmailOrLogin(client, email)
      if (!ids) return null
      const { userId, kayttajaId } = ids
      // Clean up tehtavatyyppi, kayttaja_yliopisto_erikoisala, kouluttajavaltuutus, kayttaja
      await cleanupUser(
        client,
        userId,
        kayttajaId,
        async (client, kayttajaId) => {
          await client.query(
            `DELETE FROM rel_kayttaja_yliopisto_erikoisala__tehtavatyyppi
             WHERE kayttaja_yliopisto_erikoisala_id IN (
               SELECT id FROM kayttaja_yliopisto_erikoisala WHERE kayttaja_id = $1
             )`,
            [kayttajaId]
          )
          await client.query(
            `DELETE FROM kayttaja_yliopisto_erikoisala WHERE kayttaja_id = $1`,
            [kayttajaId]
          )
          await client.query(
            `DELETE FROM kouluttajavaltuutus WHERE valtuutettu_id = $1`,
            [kayttajaId]
          )
        }
      )
      return null
    })
  },
}
