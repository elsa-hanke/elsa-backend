import { dbClient } from './db-client'

/**
 * Tasks for seeding and cleaning up the kouluttaja (trainer) test user.
 *
 * db:seedKouluttaja  – Creates a ROLE_KOULUTTAJA user linked to yliopisto 1 /
 *                      erikoisala 46 (Työterveyshuolto).  Idempotent.
 * db:cleanupKouluttaja – Removes all rows created by seedKouluttaja.  Safe to
 *                        call even if the user does not exist.
 */
export const kouluttajaTasks = {
  async 'db:seedKouluttaja'({ email, etunimi, sukunimi }: {
    email: string
    etunimi: string
    sukunimi: string
  }): Promise<{ kayttajaId: number } | null> {
    const client = dbClient()
    await client.connect()
    try {
      // Varmistetaan, että ROLE_KOULUTTAJA löytyy jhi_authority-taulusta.
      // JPA:n @ManyToMany liittyy jhi_authority-tauluun authority_name-sarakkeen kautta,
      // joten ilman tätä riviä JPQL-query ei löydä kouluttajaa (left join tuottaa NULL:n).
      await client.query(
        `INSERT INTO jhi_authority (name) VALUES ('ROLE_KOULUTTAJA') ON CONFLICT DO NOTHING`
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

        // Varmistetaan, että kayttaja_yliopisto_erikoisala-rivi on olemassa.
        // Jos se puuttuu (esim. aiempi ajonkertainen luonti epäonnistui osittain),
        // lisätään se tässä.
        const ye = await client.query(
          `SELECT id FROM kayttaja_yliopisto_erikoisala
           WHERE kayttaja_id = $1 AND yliopisto_id = 1 AND erikoisala_id = 46`,
          [kayttajaId]
        )
        if (ye.rows.length === 0) {
          await client.query(
            `INSERT INTO kayttaja_yliopisto_erikoisala (kayttaja_id, yliopisto_id, erikoisala_id)
             VALUES ($1, 1, 46)`,
            [kayttajaId]
          )
        }

        // Varmistetaan myös, että jhi_user_authority-rivi on olemassa
        const userId = (await client.query(
          `SELECT u.id FROM jhi_user u WHERE u.email = $1`,
          [email]
        )).rows[0].id
        await client.query(
          `INSERT INTO jhi_user_authority (user_id, authority_name)
           VALUES ($1,'ROLE_KOULUTTAJA') ON CONFLICT DO NOTHING`,
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
         VALUES ($1,$2,$3,$4,$5,true,'fi','system','system','ROLE_KOULUTTAJA')`,
        [userId, email, etunimi, sukunimi, email]
      )
      await client.query(
        `INSERT INTO jhi_user_authority (user_id, authority_name)
         VALUES ($1,'ROLE_KOULUTTAJA') ON CONFLICT DO NOTHING`,
        [userId]
      )

      // IDENTITY-sarake generoi id:n automaattisesti – sekvenssi etenee oikein
      // eikä synny duplicate-key-törmäystä ensimmäisellä ajolla.
      const kayttajaId: number = (
        await client.query(
          `INSERT INTO kayttaja (nimike, user_id, tila)
           VALUES ($1,$2,'AKTIIVINEN')
           RETURNING id`,
          [nimi, userId]
        )
      ).rows[0].id

      // Linkitetään yliopistoon 1 + erikoisalaan 46 (Työterveyshuolto) – vastaa
      // createWithoutOpintotietodata-metodin luomaa erikoistuvan tietuetta.
      await client.query(
        `INSERT INTO kayttaja_yliopisto_erikoisala
           (kayttaja_id, yliopisto_id, erikoisala_id)
         VALUES ($1,1,46)`,
        [kayttajaId]
      )

      return { kayttajaId }
    } finally {
      await client.end()
    }
  },

  /**
   * Poistaa esialustetun kouluttajan tiedot. Turvallinen kutsua, vaikka käyttäjää
   * ei olisikaan.
   */
  async 'db:cleanupKouluttaja'({ email }: { email: string }): Promise<null> {
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
        await client.query(
          `DELETE FROM kouluttajavaltuutus WHERE valtuutettu_id = $1`,
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

