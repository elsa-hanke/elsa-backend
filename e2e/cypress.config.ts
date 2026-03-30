import { defineConfig } from 'cypress'
import { Client } from 'pg'

function dbClient() {
  return new Client({
    host: process.env.CYPRESS_DB_HOST ?? 'localhost',
    port: parseInt(process.env.CYPRESS_DB_PORT ?? '5432', 10),
    user: process.env.CYPRESS_DB_USER ?? 'elsaBackend',
    password: process.env.CYPRESS_DB_PASSWORD ?? '',
    database: process.env.CYPRESS_DB_NAME ?? 'elsaBackend',
  })
}

export default defineConfig({
  e2e: {
    baseUrl: 'http://localhost:9060',
    specPattern: 'cypress/e2e/**/*.cy.ts',
    supportFile: 'cypress/support/e2e.ts',
    setupNodeEvents(on) {
      on('task', {
        /**
         * Pre-seeds a kouluttaja user so they appear in the erikoistuva's dropdown
         * without triggering an email invitation. Uses the same yliopisto (1) and
         * erikoisala (46 = Työterveyshuolto) that createWithoutOpintotietodata assigns.
         */
        async 'db:seedKouluttaja'({ email, etunimi, sukunimi }: {
          email: string
          etunimi: string
          sukunimi: string
        }): Promise<{ kayttajaId: number } | null> {
          const client = dbClient()
          await client.connect()
          try {
            // Idempotent: skip if user already exists
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

            const kayttajaId: number = (
              await client.query(`SELECT nextval('hibernate_sequence') AS id`)
            ).rows[0].id

            await client.query(
              `INSERT INTO kayttaja (id, nimi, user_id, tila)
               VALUES ($1,$2,$3,'AKTIIVINEN')`,
              [kayttajaId, nimi, userId]
            )

            // Link to yliopisto 1 + erikoisala 46 (Työterveyshuolto) — matches the
            // erikoistuva created by createWithoutOpintotietodata in dev profile.
            const kyeId: number = (
              await client.query(`SELECT nextval('hibernate_sequence') AS id`)
            ).rows[0].id
            await client.query(
              `INSERT INTO kayttaja_yliopisto_erikoisala
                 (id, kayttaja_id, yliopisto_id, erikoisala_id)
               VALUES ($1,$2,1,46)`,
              [kyeId, kayttajaId]
            )

            return { kayttajaId }
          } finally {
            await client.end()
          }
        },

        /**
         * Removes seeded kouluttaja data. Safe to call even if the user doesn't exist.
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
         * Removes an erikoistuva lääkäri user and all their related data.
         * Safe to call even if the user doesn't exist.
         * Use this before first-login tests so the "Aloita palvelun käyttö"
         * screen is always presented on the next login.
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
              // Delete records that reference opintooikeus rows belonging to this user
              await client.query(
                `DELETE FROM koulutussuunnitelma
                 WHERE opintooikeus_id IN (
                   SELECT id FROM opintooikeus WHERE erikoistuva_laakari_id = $1
                 )`,
                [el_id]
              )
              // Delete dependent keskeytysaika rows first (FK to tyoskentelyjakso)
              await client.query(
                `DELETE FROM keskeytysaika WHERE tyoskentelyjakso_id IN (
                   SELECT id FROM tyoskentelyjakso WHERE opintooikeus_id IN (
                     SELECT id FROM opintooikeus WHERE erikoistuva_laakari_id = $1
                   )
                 )`,
                [el_id]
              )
              // Delete dependent asiakirja rows first (FK to tyoskentelyjakso)
              await client.query(
                `DELETE FROM asiakirja WHERE tyoskentelyjakso_id IN (
                   SELECT id FROM tyoskentelyjakso WHERE opintooikeus_id IN (
                     SELECT id FROM opintooikeus WHERE erikoistuva_laakari_id = $1
                   )
                 )`,
                [el_id]
              )
              // Delete dependent tyoskentelyjakso rows first (FK to opintooikeus)
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

        /** Simple connectivity check — returns true when the DB is reachable. */
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
    },
    viewportWidth: 1280,
    viewportHeight: 800,
    defaultCommandTimeout: 30000,
    video: true,
    screenshotOnRunFailure: true,
    experimentalModifyObstructiveThirdPartyCode: true,
  },
})
