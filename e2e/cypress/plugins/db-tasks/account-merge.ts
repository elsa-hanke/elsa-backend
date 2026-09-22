import type { Client } from 'pg'
import { dbClient, withDb } from './db-client'
import { ensureYliopistoErikoisalaLink } from './kayttaja-utils'
import {
  cleanupUser,
  createUserWithRole,
  createVerificationToken,
  fetchUserIdsByEmailOrLogin
} from './user-utils'
import {
  MERGE_ADMIN_EMAIL,
  MERGE_RETAINED_EMAIL,
  MERGE_SOURCE_EMAIL
} from '../../support/commands/credentials'

const emails = [MERGE_SOURCE_EMAIL, MERGE_RETAINED_EMAIL, MERGE_ADMIN_EMAIL]

export const accountMergeTasks = {
  async 'db:seedAccountMergeUsers'({ ownerEmail }: { ownerEmail: string }) {
    return withDb(dbClient, async (client: Client) => {
      await client.query('BEGIN')
      try {
        for (const email of emails) {
          if (await fetchUserIdsByEmailOrLogin(client, email)) {
            throw new Error('Clean up the previous account-merge fixture before seeding it.')
          }
        }
        const create = async (email: string, role: string, etunimi: string, sukunimi: string) => {
          await client.query(
            'INSERT INTO jhi_authority (name) VALUES ($1) ON CONFLICT DO NOTHING',
            [role]
          )
          const id = Number(
            await createUserWithRole(client, {
              email,
              role,
              etunimi,
              sukunimi,
              linkFn: role === 'ROLE_KOULUTTAJA' ? ensureYliopistoErikoisalaLink : async () => 0
            })
          )
          if (!Number.isSafeInteger(id) || id <= 0) throw new Error('Invalid fixture account ID')
          const user = await fetchUserIdsByEmailOrLogin(client, email)
          const token = await createVerificationToken(client, user!.userId)
          return { id, token }
        }
        // Reuse the existing Suomi.fi test identities (Tessa Testilä and Daniel Siekkinen).
        // Never log in the source: token login would trigger a different, automatic merge path.
        const retained = await create(
          MERGE_RETAINED_EMAIL,
          'ROLE_ERIKOISTUVA_LAAKARI',
          'Tessa',
          'Testilä'
        )
        const source = await create(MERGE_SOURCE_EMAIL, 'ROLE_KOULUTTAJA', 'Tessa', 'Testilä')
        const admin = await create(
          MERGE_ADMIN_EMAIL,
          'ROLE_TEKNINEN_PAAKAYTTAJA',
          'Daniel',
          'Siekkinen'
        )
        await client.query("UPDATE kayttaja SET tila = 'KUTSUTTU' WHERE id = $1", [source.id])
        const resident = await client.query(
          `INSERT INTO erikoistuva_laakari (kayttaja_id, syntymaaika)
           VALUES ($1, '1980-02-01') RETURNING id`,
          [retained.id]
        )
        // Copy only study-right metadata, never the other trainee's forms or records.
        const studyRight = await client.query(
          `INSERT INTO opintooikeus
             (erikoistuva_laakari_id, opintooikeuden_myontamispaiva, opintooikeuden_paattymispaiva,
              opiskelijatunnus, osaamisen_arvioinnin_oppaan_pvm, yliopisto_id, erikoisala_id,
              opintoopas_id, asetus_id, kaytossa, yliopisto_opintooikeus_id, tila, muokkausaika,
              terveyskeskuskoulutusjakso_suoritettu, muokkausoikeudet_virkailijoilla, viimeinen_katselupaiva)
           SELECT $1, o.opintooikeuden_myontamispaiva, o.opintooikeuden_paattymispaiva,
                  'e2e-merge', o.osaamisen_arvioinnin_oppaan_pvm, o.yliopisto_id, o.erikoisala_id,
                  o.opintoopas_id, o.asetus_id, true, $2, o.tila, CURRENT_TIMESTAMP,
                  false, false, o.viimeinen_katselupaiva
           FROM opintooikeus o
           JOIN erikoistuva_laakari el ON el.id = o.erikoistuva_laakari_id
           JOIN kayttaja k ON k.id = el.kayttaja_id
           JOIN jhi_user u ON u.id = k.user_id
           WHERE u.email = $3 AND o.kaytossa = true RETURNING id`,
          [resident.rows[0].id, `e2e-merge-${retained.id}`, ownerEmail]
        )
        if (studyRight.rows.length !== 1)
          throw new Error('Expected one active study right for the form owner')
        await client.query(
          'UPDATE erikoistuva_laakari SET aktiivinen_opintooikeus = $1 WHERE id = $2',
          [studyRight.rows[0].id, resident.rows[0].id]
        )
        await client.query('COMMIT')
        return { retained, source, admin }
      } catch (error) {
        await client.query('ROLLBACK')
        throw error
      }
    })
  },

  async 'db:readAccountMergeUsers'() {
    return withDb(dbClient, async (client: Client) => {
      const result = await client.query(
        `SELECT k.id, u.email, array_agg(a.authority_name ORDER BY a.authority_name) AS roles
         FROM kayttaja k JOIN jhi_user u ON u.id = k.user_id
         JOIN jhi_user_authority a ON a.user_id = u.id
         WHERE u.login = ANY($1::text[]) GROUP BY k.id, u.email ORDER BY k.id`,
        [emails]
      )
      return result.rows.map((row) => ({ ...row, id: Number(row.id) }))
    })
  },

  // Call cleanupKoejakso for the form owner first. Only this fixture's accounts are removed.
  async 'db:cleanupAccountMergeUsers'() {
    return withDb(dbClient, async (client: Client) => {
      await client.query('BEGIN')
      try {
        for (const email of emails) {
          const user = await fetchUserIdsByEmailOrLogin(client, email)
          if (!user) continue
          await cleanupUser(client, user.userId, user.kayttajaId, async (db, id) => {
            await db.query('DELETE FROM kouluttajavaltuutus WHERE valtuutettu_id = $1', [id])
            await db.query(
              `DELETE FROM rel_kayttaja_yliopisto_erikoisala__tehtavatyyppi
               WHERE kayttaja_yliopisto_erikoisala_id IN
               (SELECT id FROM kayttaja_yliopisto_erikoisala WHERE kayttaja_id = $1)`,
              [id]
            )
            await db.query('DELETE FROM kayttaja_yliopisto_erikoisala WHERE kayttaja_id = $1', [id])
            await db.query(
              'UPDATE erikoistuva_laakari SET aktiivinen_opintooikeus = NULL WHERE kayttaja_id = $1',
              [id]
            )
            // Opening the retained trainee's page creates an empty training plan on read.
            await db.query(
              `DELETE FROM koulutussuunnitelma WHERE opintooikeus_id IN (
                 SELECT o.id FROM opintooikeus o
                 JOIN erikoistuva_laakari el ON el.id = o.erikoistuva_laakari_id
                 WHERE el.kayttaja_id = $1
               )`,
              [id]
            )
            await db.query(
              'DELETE FROM opintooikeus WHERE erikoistuva_laakari_id IN (SELECT id FROM erikoistuva_laakari WHERE kayttaja_id = $1)',
              [id]
            )
            await db.query('DELETE FROM erikoistuva_laakari WHERE kayttaja_id = $1', [id])
          })
        }
        await client.query('COMMIT')
        return null
      } catch (error) {
        await client.query('ROLLBACK')
        throw error
      }
    })
  }
}
