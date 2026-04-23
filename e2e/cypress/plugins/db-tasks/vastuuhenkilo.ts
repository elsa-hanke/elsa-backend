import { dbClient } from './db-client'

/**
 * Tehtävät vastuuhenkilön testikäyttäjän siementämiseen ja siivoamiseen.
 *
 * db:seedVastuuhenkilo  – Luo ROLE_VASTUUHENKILO-käyttäjän, joka on linkitetty
 *                         yliopistoon 1 ja erikoisalaan 46.  Idempotentti.
 * db:cleanupVastuuhenkilo – Poistaa kaikki seedVastuuhenkilo:n luomat rivit.
 */
export const vastuuhenkiloTasks = {
  async 'db:seedVastuuhenkilo'({ email, etunimi, sukunimi }: {
    email: string
    etunimi: string
    sukunimi: string
  }): Promise<{ kayttajaId: number } | null> {
    const client = dbClient()
    await client.connect()
    try {
      // Varmistetaan, että ROLE_VASTUUHENKILO on jhi_authority-taulussa
      await client.query(
        `INSERT INTO jhi_authority (name) VALUES ('ROLE_VASTUUHENKILO') ON CONFLICT DO NOTHING`
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
        // Varmistetaan kayttaja_yliopisto_erikoisala-rivi
        const ye = await client.query(
          `SELECT id FROM kayttaja_yliopisto_erikoisala
           WHERE kayttaja_id = $1 AND yliopisto_id = 1 AND erikoisala_id = 46`,
          [kayttajaId]
        )
        if (ye.rows.length === 0) {
          const yeId: number = (await client.query(
            `INSERT INTO kayttaja_yliopisto_erikoisala (kayttaja_id, yliopisto_id, erikoisala_id)
             VALUES ($1, 1, 46) RETURNING id`,
            [kayttajaId]
          )).rows[0].id
          await seedTehtavatyyppit(client, yeId)
        } else {
          // Varmistetaan tehtävätyyppi-linkitykset
          await seedTehtavatyyppit(client, ye.rows[0].id)
        }
        return { kayttajaId }
      }

      const userId = require('crypto').randomUUID()
      const nimi = `${etunimi} ${sukunimi}`

      await client.query(
        `INSERT INTO jhi_user
           (id, login, first_name, last_name, email, activated, lang_key,
            created_by, last_modified_by, active_authority)
         VALUES ($1,$2,$3,$4,$5,true,'fi','system','system','ROLE_VASTUUHENKILO')`,
        [userId, email, etunimi, sukunimi, email]
      )
      await client.query(
        `INSERT INTO jhi_user_authority (user_id, authority_name)
         VALUES ($1,'ROLE_VASTUUHENKILO') ON CONFLICT DO NOTHING`,
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

      const yeId: number = (
        await client.query(
          `INSERT INTO kayttaja_yliopisto_erikoisala
             (kayttaja_id, yliopisto_id, erikoisala_id)
           VALUES ($1, 1, 46) RETURNING id`,
          [kayttajaId]
        )
      ).rows[0].id

      await seedTehtavatyyppit(client, yeId)

      return { kayttajaId }
    } finally {
      await client.end()
    }
  },

  async 'db:cleanupVastuuhenkilo'({ email }: { email: string }): Promise<null> {
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
          `DELETE FROM rel_kayttaja_yliopisto_erikoisala__tehtavatyyppi
           WHERE kayttaja_yliopisto_erikoisala_id IN
             (SELECT id FROM kayttaja_yliopisto_erikoisala WHERE kayttaja_id = $1)`,
          [kayttaja_id]
        )
        await client.query(
          `DELETE FROM kayttaja_yliopisto_erikoisala WHERE kayttaja_id = $1`,
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

/**
 * Lisää vastuuhenkilön tehtävätyyppi-linkitykset kayttaja_yliopisto_erikoisala-riville.
 * Idempotentti ON CONFLICT DO NOTHING -lausekkeen avulla.
 */
async function seedTehtavatyyppit(client: import('pg').Client, yeId: number): Promise<void> {
  // Haetaan tehtävätyyppi-id:t nimien perusteella
  const types = await client.query(
    `SELECT id, nimi FROM vastuuhenkilon_tehtavatyyppi
     WHERE nimi IN (
       'KOEJAKSOSOPIMUSTEN_JA_KOEJAKSOJEN_HYVAKSYMINEN',
       'VALMISTUMISPYYNNON_HYVAKSYNTA_LAAKETIEDE',
       'VALMISTUMISPYYNNON_HYVAKSYNTA_HAMMASLAAKETIEDE',
       'VALMISTUMISPYYNNON_HYVAKSYNTA_YEK'
     )`
  )

  for (const row of types.rows) {
    await client.query(
      `INSERT INTO rel_kayttaja_yliopisto_erikoisala__tehtavatyyppi
         (kayttaja_yliopisto_erikoisala_id, vastuuhenkilon_tehtavatyyppi_id)
       VALUES ($1, $2)
       ON CONFLICT DO NOTHING`,
      [yeId, row.id]
    )
  }
}

