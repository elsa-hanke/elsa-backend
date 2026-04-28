import { randomUUID } from 'crypto'
import type { Client } from 'pg'
import { dbClient, withDb } from './db-client'

const ROLE = 'ROLE_VASTUUHENKILO'

const TEHTAVATYYPPIT = [
  'KOEJAKSOSOPIMUSTEN_JA_KOEJAKSOJEN_HYVAKSYMINEN',
  'VALMISTUMISPYYNNON_OSAAMISEN_ARVIOINTI',
  'VALMISTUMISPYYNNON_HYVAKSYNTA',
  'YEK_VALMISTUMINEN',
] as const

// ─── Helpers ──────────────────────────────────────────────────────────────────

async function fetchVastuuhenkiloIds(
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

/**
 * Ensures the kayttaja_yliopisto_erikoisala row exists and returns its id.
 * Returns the existing row's id if already present.
 */
async function ensureYliopistoErikoisalaLink(client: Client, kayttajaId: number): Promise<number> {
  const existing = await client.query(
    `SELECT id FROM kayttaja_yliopisto_erikoisala
     WHERE kayttaja_id = $1 AND yliopisto_id = 1 AND erikoisala_id = 46`,
    [kayttajaId]
  )
  if (existing.rows.length > 0) return existing.rows[0].id

  return (
    await client.query(
      `INSERT INTO kayttaja_yliopisto_erikoisala (kayttaja_id, yliopisto_id, erikoisala_id)
       VALUES ($1, 1, 46) RETURNING id`,
      [kayttajaId]
    )
  ).rows[0].id
}

/**
 * Lisää vastuuhenkilön tehtävätyyppi-linkitykset kayttaja_yliopisto_erikoisala-riville.
 * Idempotentti ON CONFLICT DO NOTHING -lausekkeen avulla.
 */
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

async function ensureUserAuthority(client: Client, userId: string): Promise<void> {
  await client.query(
    `INSERT INTO jhi_user_authority (user_id, authority_name)
     VALUES ($1, $2) ON CONFLICT DO NOTHING`,
    [userId, ROLE]
  )
}

async function createVastuuhenkiloUser(
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
       VALUES ($1,$2,'AKTIIVINEN') RETURNING id`,
      [nimi, userId]
    )
  ).rows[0].id

  const yeId = await ensureYliopistoErikoisalaLink(client, kayttajaId)
  await seedTehtavatyyppit(client, yeId)

  return kayttajaId
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
  }): Promise<{ kayttajaId: number } | null> {
    return withDb(dbClient, async (client: any) => {
      await client.query(
        `INSERT INTO jhi_authority (name) VALUES ($1) ON CONFLICT DO NOTHING`,
        [ROLE]
      )

      const existing = await fetchVastuuhenkiloIds(client, email)

      if (existing) {
        // User already exists — ensure yliopisto/erikoisala link and tehtavatyyppit
        // are present in case a previous partial run left them missing.
        const yeId = await ensureYliopistoErikoisalaLink(client, existing.kayttajaId)
        await seedTehtavatyyppit(client, yeId)
        await ensureUserAuthority(client, existing.userId)
        return { kayttajaId: existing.kayttajaId }
      }

      const kayttajaId = await createVastuuhenkiloUser(client, email, etunimi, sukunimi)
      return { kayttajaId }
    })
  },

  async 'db:cleanupVastuuhenkilo'({ email }: { email: string }): Promise<null> {
    return withDb(dbClient, async (client:any) => {
      const ids = await fetchVastuuhenkiloIds(client, email)
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
        await client.query(`DELETE FROM kayttaja WHERE id = $1`, [kayttajaId])
      }

      await client.query(`DELETE FROM jhi_user_authority WHERE user_id = $1`, [userId])
      await client.query(`DELETE FROM jhi_user WHERE id = $1`, [userId])
      return null
    })
  },
}
