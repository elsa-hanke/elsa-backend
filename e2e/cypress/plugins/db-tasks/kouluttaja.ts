import { randomUUID } from 'crypto'
import type { Client } from 'pg'
import { dbClient, withDb } from './db-client'

// ─── Helpers ──────────────────────────────────────────────────────────────────

async function fetchKouluttajaIds(
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

async function ensureYliopistoErikoisalaLink(client: Client, kayttajaId: number): Promise<void> {
  await client.query(
    `INSERT INTO kayttaja_yliopisto_erikoisala (kayttaja_id, yliopisto_id, erikoisala_id)
     SELECT $1, 1, 46
     WHERE NOT EXISTS (
       SELECT 1 FROM kayttaja_yliopisto_erikoisala
       WHERE kayttaja_id = $1 AND yliopisto_id = 1 AND erikoisala_id = 46
     )`,
    [kayttajaId]
  )
}

async function ensureUserAuthority(client: Client, userId: string): Promise<void> {
  await client.query(
    `INSERT INTO jhi_user_authority (user_id, authority_name)
     VALUES ($1, 'ROLE_KOULUTTAJA') ON CONFLICT DO NOTHING`,
    [userId]
  )
}

async function createKouluttajaUser(
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
     VALUES ($1,$2,$3,$4,$5,true,'fi','system','system','ROLE_KOULUTTAJA')`,
    [userId, email, etunimi, sukunimi, email]
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

  await ensureYliopistoErikoisalaLink(client, kayttajaId)

  return kayttajaId
}

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
  }): Promise<{ kayttajaId: number } | null> {
    return withDb(dbClient, async (client: any) => {
      // JPA's @ManyToMany joins on jhi_authority.name, so this row must exist
      // or JPQL queries won't find the kouluttaja (the left join produces NULL).
      await client.query(
        `INSERT INTO jhi_authority (name) VALUES ('ROLE_KOULUTTAJA') ON CONFLICT DO NOTHING`
      )

      const existing = await fetchKouluttajaIds(client, email)

      if (existing) {
        // User already exists — ensure both the yliopisto link and authority row
        // are present in case a previous partial run left them missing.
        await ensureYliopistoErikoisalaLink(client, existing.kayttajaId)
        await ensureUserAuthority(client, existing.userId)
        return { kayttajaId: existing.kayttajaId }
      }

      const kayttajaId = await createKouluttajaUser(client, email, etunimi, sukunimi)
      return { kayttajaId }
    })
  },

  /**
   * Poistaa esialustetun kouluttajan tiedot. Turvallinen kutsua, vaikka käyttäjää
   * ei olisikaan.
   */
  async 'db:cleanupKouluttaja'({ email }: { email: string }): Promise<null> {
    return withDb(dbClient, async (client: any) => {
      const ids = await fetchKouluttajaIds(client, email)
      if (!ids) return null
      const { userId, kayttajaId } = ids

      if (kayttajaId) {
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
        await client.query(`DELETE FROM kayttaja WHERE id = $1`, [kayttajaId])
      }

      await client.query(`DELETE FROM jhi_user_authority WHERE user_id = $1`, [userId])
      await client.query(`DELETE FROM jhi_user WHERE id = $1`, [userId])
      return null
    })
  },
}
