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

const ROLE = 'ROLE_VASTUUHENKILO'
const TEHTAVATYYPPIT = [
  'KOEJAKSOSOPIMUSTEN_JA_KOEJAKSOJEN_HYVAKSYMINEN',
  'VALMISTUMISPYYNNON_OSAAMISEN_ARVIOINTI',
  'VALMISTUMISPYYNNON_HYVAKSYNTA',
  'YEK_VALMISTUMINEN',
] as const

// Helper for tehtavatyyppi seeding (remains local)
async function seedTehtavatyyppit(client: Client, yeId: number): Promise<void> {
  const types = await client.query(
    `SELECT id FROM vastuuhenkilon_tehtavatyyppi WHERE nimi = ANY($1)`,
    [TEHTAVATYYPPIT]
  )
  for (const row of types.rows as { id: number }[]) {
    await client.query(
      `INSERT INTO rel_kayttaja_yliopisto_erikoisala__tehtavatyyppi
         (kayttaja_yliopisto_erikoisala_id, vastuuhenkilon_tehtavatyyppi_id)
       VALUES ($1, $2) ON CONFLICT DO NOTHING`,
      [yeId, row.id]
    )
  }
}

// ─── Exported tasks ───────────────────────────────────────────────────────────

/**
 * Tehtävät vastuuhenkilön testikäyttäjän siementämiseen ja siivoamiseen.
 *
 * db:seedVastuuhenkilo    – Luo ROLE_VASTUUHENKILO-käyttäjän, joka on linkitetty
 *                           yliopistoon 1 ja erikoisalaan 46. Idempotentti.
 * db:cleanupVastuuhenkilo – Poistaa kaikki seedVastuuhenkilo:n luomat rivit.
 */
export const vastuuhenkiloTasks = {
  async 'db:seedVastuuhenkilo'({
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
        const yeId = await ensureYliopistoErikoisalaLink(client, existing.kayttajaId)
        await seedTehtavatyyppit(client, yeId)
        await ensureUserAuthority(client, existing.userId, ROLE)
        return { kayttajaId: existing.kayttajaId }
      }

      // Custom linkFn for vastuuhenkilo: ensure link and seed tehtavatyyppit
      const linkFn = async (client: Client, kayttajaId: number) => {
        const yeId = await ensureYliopistoErikoisalaLink(client, kayttajaId)
        await seedTehtavatyyppit(client, yeId)
        return yeId
      }

      const kayttajaId = await createUserWithRole(client, {
        email,
        etunimi,
        sukunimi,
        role: ROLE,
        linkFn,
      })
      const created = await fetchUserIdsByEmailOrLogin(client, email)
      const token = await createVerificationToken(client, created!.userId)
      return { kayttajaId, token }
    })
  },

  async 'db:cleanupVastuuhenkilo'({ email }: { email: string }): Promise<null> {
    return withDb(dbClient, async (client: Client) => {
      const ids = await fetchUserIdsByEmailOrLogin(client, email)
      if (!ids) return null
      const { userId, kayttajaId } = ids
      // Clean up tehtavatyyppi, kayttaja_yliopisto_erikoisala, kayttaja
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
        }
      )
      return null
    })
  },
}
