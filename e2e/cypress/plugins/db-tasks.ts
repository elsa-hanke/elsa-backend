import { Client } from 'pg'

export function dbClient(): Client {
  return new Client({
    host: process.env.CYPRESS_DB_HOST ?? 'localhost',
    port: parseInt(process.env.CYPRESS_DB_PORT ?? '5432', 10),
    user: process.env.CYPRESS_DB_USER ?? 'elsaBackend',
    password: process.env.CYPRESS_DB_PASSWORD ?? '',
    database: process.env.CYPRESS_DB_NAME ?? 'elsaBackend',
  })
}

export function registerDbTasks(on: Cypress.PluginEvents): void {
  on('task', {
    /**
     * Esialustetaan kouluttajakäyttäjä tietokantaan, jotta hän näkyy erikoistuvan
     * arvioijavalikossa ilman sähköpostikutsua. Käyttää samaa yliopistoa (1) ja
     * erikoisalaa (46 = Työterveyshuolto) kuin createWithoutOpintotietodata dev-profiilissa.
     */
    async 'db:seedKouluttaja'({ email, etunimi, sukunimi }: {
      email: string
      etunimi: string
      sukunimi: string
    }): Promise<{ kayttajaId: number } | null> {
      const client = dbClient()
      await client.connect()
      try {
        // Idempotentti: ohitetaan, jos käyttäjä on jo olemassa
        const existing = await client.query(
          `SELECT k.id FROM kayttaja k
           JOIN jhi_user u ON u.id = k.user_id
           WHERE u.email = $1`,
          [email]
        )
        if (existing.rows.length > 0) {
          return { kayttajaId: existing.rows[0].id }
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
           VALUES ($1,'ROLE_KOULUTTAJA')`,
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

    /**
     * Poistaa erikoistuvan lääkärin käyttäjän ja kaikki hänen tietonsa.
     * Turvallinen kutsua, vaikka käyttäjää ei olisikaan.
     * Kutsu ennen ensimmäisen kirjautumisen testejä, jotta "Aloita palvelun käyttö"
     * -näkymä esitetään aina seuraavalla kirjautumisella.
     */
    async 'db:cleanupErikoistuva'({ email }: { email: string }): Promise<null> {
      const client = dbClient()
      await client.connect()
      try {
        const result = await client.query(
          `SELECT u.id AS user_id, k.id AS kayttaja_id, el.id AS el_id
           FROM jhi_user u
           LEFT JOIN kayttaja k ON k.user_id = u.id
           LEFT JOIN erikoistuva_laakari el ON el.kayttaja_id = k.id
           WHERE u.email = $1`,
          [email]
        )
        if (result.rows.length === 0) return null

        const { user_id, kayttaja_id, el_id } = result.rows[0]

        if (el_id) {
          // Poistetaan koulutusjakso-rivit ennen koulutussuunnitelma-rivejä
          // (FK: koulutusjakso.koulutussuunnitelma_id → koulutussuunnitelma.id)
          await client.query(
            `DELETE FROM koulutusjakso
             WHERE koulutussuunnitelma_id IN (
               SELECT id FROM koulutussuunnitelma
               WHERE opintooikeus_id IN (
                 SELECT id FROM opintooikeus WHERE erikoistuva_laakari_id = $1
               )
             )`,
            [el_id]
          )
          await client.query(
            `DELETE FROM koulutussuunnitelma
             WHERE opintooikeus_id IN (
               SELECT id FROM opintooikeus WHERE erikoistuva_laakari_id = $1
             )`,
            [el_id]
          )
          // Poistetaan keskeytysaika-rivit ennen tyoskentelyjakso-rivejä (FK)
          await client.query(
            `DELETE FROM keskeytysaika WHERE tyoskentelyjakso_id IN (
               SELECT id FROM tyoskentelyjakso WHERE opintooikeus_id IN (
                 SELECT id FROM opintooikeus WHERE erikoistuva_laakari_id = $1
               )
             )`,
            [el_id]
          )
          // Poistetaan asiakirja-rivit ennen tyoskentelyjakso-rivejä (FK)
          await client.query(
            `DELETE FROM asiakirja WHERE tyoskentelyjakso_id IN (
               SELECT id FROM tyoskentelyjakso WHERE opintooikeus_id IN (
                 SELECT id FROM opintooikeus WHERE erikoistuva_laakari_id = $1
               )
             )`,
            [el_id]
          )
          // Poistetaan tyoskentelyjakso-rivit ennen opintooikeus-rivejä (FK)
          await client.query(
            `DELETE FROM tyoskentelyjakso WHERE opintooikeus_id IN (
               SELECT id FROM opintooikeus WHERE erikoistuva_laakari_id = $1
             )`,
            [el_id]
          )
          await client.query(
            `DELETE FROM opintooikeus_herate WHERE erikoistuva_laakari_id = $1`,
            [el_id]
          )
          await client.query(
            `DELETE FROM opintooikeus WHERE erikoistuva_laakari_id = $1`,
            [el_id]
          )
          await client.query(
            `DELETE FROM erikoistuva_laakari WHERE id = $1`,
            [el_id]
          )
        }

        if (kayttaja_id) {
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

    /** Yhteyden tarkistus – palauttaa true, kun tietokanta on tavoitettavissa. */
    async 'db:ping'(): Promise<boolean> {
      const client = dbClient()
      try {
        await client.connect()
        await client.query('SELECT 1')
        return true
      } catch {
        return false
      } finally {
        await client.end().catch(() => undefined)
      }
    },
  })
}

