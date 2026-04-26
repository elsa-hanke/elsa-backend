import { randomUUID } from 'crypto'
import type { Client } from 'pg'
import { dbClient, withDb } from './db-client'

const ROLE = 'ROLE_OPINTOHALLINNON_VIRKAILIJA'

// ─── Helpers ──────────────────────────────────────────────────────────────────

async function fetchVirkalijaIds(
  client: Client,
  email: string
): Promise<{ userId: string; kayttajaId: number } | null> {
  const result = await client.query(
    `SELECT u.id AS user_id, k.id AS kayttaja_id
     FROM jhi_user u
     LEFT JOIN kayttaja k ON k.user_id = u.id
     WHERE u.email = $1`,
    [email]
  )
  if (result.rows.length === 0) return null
  return { userId: result.rows[0].user_id, kayttajaId: result.rows[0].kayttaja_id }
}

async function ensureYliopistoLink(client: Client, kayttajaId: number): Promise<void> {
  await client.query(
    `INSERT INTO rel_kayttaja__yliopisto (kayttaja_id, yliopisto_id)
     VALUES ($1, 1) ON CONFLICT DO NOTHING`,
    [kayttajaId]
  )
}

async function ensureUserAuthority(client: Client, userId: string): Promise<void> {
  await client.query(
    `INSERT INTO jhi_user_authority (user_id, authority_name)
     VALUES ($1, $2) ON CONFLICT DO NOTHING`,
    [userId, ROLE]
  )
}

async function createVirkalijaUser(
  client: Client,
  email: string,
  etunimi: string,
  sukunimi: string
): Promise<number> {
  const userId = randomUUID()
  const nimi = `${etunimi} ${sukunimi}`

  await client.query(
    `INSERT INTO jhi_user
       (id, login, first_name, last_name, email, activated, lang_key,
        created_by, last_modified_by, active_authority)
     VALUES ($1,$2,$3,$4,$5,true,'fi','system','system',$6)`,
    [userId, email, etunimi, sukunimi, email, ROLE]
  )
  await ensureUserAuthority(client, userId)

  const kayttajaId: number = (
    await client.query(
      `INSERT INTO kayttaja (nimike, user_id, tila)
       VALUES ($1,$2,'AKTIIVINEN')
       RETURNING id`,
      [nimi, userId]
    )
  ).rows[0].id

  await ensureYliopistoLink(client, kayttajaId)

  return kayttajaId
}

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
  }): Promise<{ kayttajaId: number } | null> {
    return withDb(dbClient, async (client: any) => {
      // Virkailijan auktorisointi edellyttää, että rooli löytyy jhi_authority-taulusta
      await client.query(
        `INSERT INTO jhi_authority (name) VALUES ($1) ON CONFLICT DO NOTHING`,
        [ROLE]
      )

      const existing = await fetchVirkalijaIds(client, email)

      if (existing) {
        // User already exists — ensure both the yliopisto link and authority row
        // are present in case a previous partial run left them missing.
        await ensureYliopistoLink(client, existing.kayttajaId)
        await ensureUserAuthority(client, existing.userId)
        return { kayttajaId: existing.kayttajaId }
      }

      const kayttajaId = await createVirkalijaUser(client, email, etunimi, sukunimi)
      return { kayttajaId }
    })
  },

  async 'db:cleanupVirkailija'({ email }: { email: string }): Promise<null> {
    return withDb(dbClient, async (client: any) => {
      const ids = await fetchVirkalijaIds(client, email)
      if (!ids) return null
      const { userId, kayttajaId } = ids

      if (kayttajaId) {
        await client.query(
          `DELETE FROM rel_kayttaja__yliopisto WHERE kayttaja_id = $1`,
          [kayttajaId]
        )
        await client.query(`DELETE FROM kayttaja WHERE id = $1`, [kayttajaId])
      }

      await client.query(`DELETE FROM jhi_user_authority WHERE user_id = $1`, [userId])
      await client.query(`DELETE FROM jhi_user WHERE id = $1`, [userId])
      return null
    })
  },
}
