import { dbClient } from './db-client'

/**
 * Tehtävät opintohallinnon virkailijan testikäyttäjän siementämiseen ja siivoamiseen.
 *
 * db:seedVirkailija  – Luo ROLE_OPINTOHALLINNON_VIRKAILIJA-käyttäjän, joka on linkitetty
 *                      yliopistoon 1.  Idempotentti.
 * db:cleanupVirkailija – Poistaa kaikki seedVirkailija:n luomat rivit.
 */
export const virkailijaTasks = {
  async 'db:seedVirkailija'({ email, etunimi, sukunimi }: {
    email: string
    etunimi: string
    sukunimi: string
  }): Promise<{ kayttajaId: number } | null> {
    const client = dbClient()
    await client.connect()
    try {
      // Varmistetaan, että ROLE_OPINTOHALLINNON_VIRKAILIJA on jhi_authority-taulussa
      await client.query(
        `INSERT INTO jhi_authority (name)
         VALUES ('ROLE_OPINTOHALLINNON_VIRKAILIJA') ON CONFLICT DO NOTHING`
      )

      // Tarkistetaan, onko käyttäjä jo olemassa
      const existing = await client.query(
        `SELECT k.id FROM kayttaja k
         JOIN jhi_user u ON u.id = k.user_id
         WHERE u.email = $1`,
        [email]
      )

      if (existing.rows.length > 0) {
        const kayttajaId: number = existing.rows[0].id
        // Varmistetaan yliopisto-linkitys
        const userId = (await client.query(
          `SELECT u.id FROM jhi_user u WHERE u.email = $1`, [email]
        )).rows[0].id
        await client.query(
          `INSERT INTO rel_kayttaja__yliopisto (kayttaja_id, yliopisto_id)
           VALUES ($1, 1) ON CONFLICT DO NOTHING`,
          [kayttajaId]
        )
        await client.query(
          `INSERT INTO jhi_user_authority (user_id, authority_name)
           VALUES ($1,'ROLE_OPINTOHALLINNON_VIRKAILIJA') ON CONFLICT DO NOTHING`,
          [userId]
        )
        return { kayttajaId }
      }

      const userId = require('crypto').randomUUID()
      const nimi = `${etunimi} ${sukunimi}`

      await client.query(
        `INSERT INTO jhi_user
           (id, login, first_name, last_name, email, activated, lang_key,
            created_by, last_modified_by, active_authority)
         VALUES ($1,$2,$3,$4,$5,true,'fi','system','system','ROLE_OPINTOHALLINNON_VIRKAILIJA')`,
        [userId, email, etunimi, sukunimi, email]
      )
      await client.query(
        `INSERT INTO jhi_user_authority (user_id, authority_name)
         VALUES ($1,'ROLE_OPINTOHALLINNON_VIRKAILIJA') ON CONFLICT DO NOTHING`,
        [userId]
      )

      const kayttajaId: number = (
        await client.query(
          `INSERT INTO kayttaja (nimike, user_id, tila)
           VALUES ($1,$2,'AKTIIVINEN')
           RETURNING id`,
          [nimi, userId]
        )
      ).rows[0].id

      // Linkitetään yliopistoon 1 – virkailijan auktorisointi tarkistaa rel_kayttaja__yliopisto-taulun
      await client.query(
        `INSERT INTO rel_kayttaja__yliopisto (kayttaja_id, yliopisto_id)
         VALUES ($1, 1) ON CONFLICT DO NOTHING`,
        [kayttajaId]
      )

      return { kayttajaId }
    } finally {
      await client.end()
    }
  },

  async 'db:cleanupVirkailija'({ email }: { email: string }): Promise<null> {
    const client = dbClient()
    await client.connect()
    try {
      const result = await client.query(
        `SELECT u.id AS user_id, k.id AS kayttaja_id
         FROM jhi_user u
         LEFT JOIN kayttaja k ON k.user_id = u.id
         WHERE u.email = $1`,
        [email]
      )
      if (result.rows.length === 0) return null

      const { user_id, kayttaja_id } = result.rows[0]

      if (kayttaja_id) {
        await client.query(
          `DELETE FROM rel_kayttaja__yliopisto WHERE kayttaja_id = $1`,
          [kayttaja_id]
        )
        await client.query(`DELETE FROM kayttaja WHERE id = $1`, [kayttaja_id])
      }
      await client.query(
        `DELETE FROM jhi_user_authority WHERE user_id = $1`,
        [user_id]
      )
      await client.query(`DELETE FROM jhi_user WHERE id = $1`, [user_id])
      return null
    } finally {
      await client.end()
    }
  },
}

