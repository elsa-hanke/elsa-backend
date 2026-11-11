import type { Client } from 'pg'
import { dbClient, withDb } from './db-client'
import { MERGE_ADMIN_EMAIL, VIRKAILIJA_EMAIL } from '../../support/commands/credentials'
import {
  cleanupUser,
  createUserWithRole,
  createVerificationToken,
  ensureUserAuthority,
  fetchUserIdsByEmailOrLogin
} from './user-utils'

const ACTOR = 'e2e-merge-search-actor@test.elsa'
const OWN = 'e2e-merge-search-own@test.elsa'
const FOREIGN = 'e2e-merge-search-foreign@test.elsa'
const CLERK = 'ROLE_OPINTOHALLINNON_VIRKAILIJA'
const ADMIN = 'ROLE_TEKNINEN_PAAKAYTTAJA'

type ReusedActor = {
  userId: string
  id: number
  activeAuthority: string | null
  addedRoles: string[]
  addedUniversity: number | null
}
// This journal belongs only to the E2E fixture. Saving it in the same transaction
// as the temporary changes lets a new Node process recover an interrupted run.
async function ensureRestoreJournal(client: Client): Promise<void> {
  await client.query(`CREATE TABLE IF NOT EXISTS e2e_account_merge_search_restore (
    id integer PRIMARY KEY CHECK (id = 1),
    actor jsonb NOT NULL
  )`)
}

export const accountMergeSearchTasks = {
  async 'db:seedAccountMergeSearch'({ admin = false }: { admin?: boolean } = {}) {
    const activeRole = admin ? ADMIN : CLERK
    return withDb(dbClient, async (client: Client) => {
      await client.query('BEGIN')
      try {
        await ensureRestoreJournal(client)
        const pending = await client.query('SELECT id FROM e2e_account_merge_search_restore WHERE id = 1')
        if (pending.rows.length) {
          throw new Error('Restore the interrupted account-merge search fixture before seeding it.')
        }
        for (const email of [ACTOR, OWN, FOREIGN]) {
          if (await fetchUserIdsByEmailOrLogin(client, email)) {
            throw new Error('Clean up the account-merge search fixture before seeding it.')
          }
        }
        // Avoid invitation-merging a test identity already used by another spec.
        // Restore only the roles/university temporarily added by this fixture.
        const linkedActor = await client.query(
          `SELECT u.id AS user_id, k.id, u.active_authority FROM jhi_user u
           JOIN kayttaja k ON k.user_id = u.id
           WHERE u.email = ANY($1::text[]) AND u.login = u.email AND u.hetu IS NOT NULL`,
          [[VIRKAILIJA_EMAIL, MERGE_ADMIN_EMAIL]]
        )
        if (linkedActor.rows.length > 1) throw new Error('Multiple managed E2E accounts are linked to the clerk test identity')
        let reused: ReusedActor | null = null
        const universities = await client.query('SELECT id FROM yliopisto ORDER BY id LIMIT 2')
        if (universities.rows.length !== 2) throw new Error('Two universities are required')
        let [ownUniversity, otherUniversity] = universities.rows.map((row) => Number(row.id))
        const metadata = await client.query(
          `SELECT p.id AS guide, p.erikoisala_id AS specialty, a.id AS regulation
           FROM opintoopas p CROSS JOIN asetus a ORDER BY p.id, a.id LIMIT 1`
        )
        if (!metadata.rows.length) throw new Error('Study-guide and regulation reference data are required')
        const { guide, specialty, regulation } = metadata.rows[0]
        const create = async (email: string, role: string, firstName: string, lastName: string) => {
          const id = Number(await createUserWithRole(client, {
            email, role, etunimi: firstName, sukunimi: lastName, linkFn: async () => 0
          }))
          const user = await fetchUserIdsByEmailOrLogin(client, email)
          return { id, userId: user!.userId }
        }
        let token: string | null = null
        if (linkedActor.rows.length) {
          const existing = linkedActor.rows[0]
          const roles = await client.query('SELECT authority_name FROM jhi_user_authority WHERE user_id = $1',
            [existing.user_id])
          reused = {
            userId: existing.user_id, id: Number(existing.id), activeAuthority: existing.active_authority,
            addedRoles: [CLERK, ADMIN].filter((role) => !roles.rows.some((row) => row.authority_name === role)),
            addedUniversity: null
          }
          for (const role of reused.addedRoles) await ensureUserAuthority(client, reused.userId, role)
          await client.query('UPDATE jhi_user SET active_authority = $1 WHERE id = $2', [activeRole, reused.userId])
          const university = await client.query(
            'SELECT yliopisto_id FROM rel_kayttaja__yliopisto WHERE kayttaja_id = $1 ORDER BY yliopisto_id LIMIT 1',
            [reused.id]
          )
          if (university.rows.length) {
            ownUniversity = Number(university.rows[0].yliopisto_id)
            otherUniversity = universities.rows.map((row) => Number(row.id)).find((id) => id !== ownUniversity)!
          } else {
            reused.addedUniversity = ownUniversity
            await client.query('INSERT INTO rel_kayttaja__yliopisto (kayttaja_id, yliopisto_id) VALUES ($1, $2)',
              [reused.id, ownUniversity])
          }
        } else {
          const actor = await create(ACTOR, CLERK, 'Daniel', 'Siekkinen')
          await ensureUserAuthority(client, actor.userId, ADMIN)
          await client.query('INSERT INTO rel_kayttaja__yliopisto (kayttaja_id, yliopisto_id) VALUES ($1, $2)',
            [actor.id, ownUniversity])
          await client.query('UPDATE jhi_user SET active_authority = $1 WHERE id = $2', [activeRole, actor.userId])
          token = await createVerificationToken(client, actor.userId)
        }
        if (reused) {
          await client.query('INSERT INTO e2e_account_merge_search_restore (id, actor) VALUES (1, $1::jsonb)',
            [JSON.stringify(reused)])
        }
        const residents: number[] = []
        for (const [email, university, lastName] of [
          [OWN, ownUniversity, 'MergeSearchOwn'],
          [FOREIGN, otherUniversity, 'MergeSearchForeign']
        ] as const) {
          const resident = await create(email, 'ROLE_ERIKOISTUVA_LAAKARI', 'E2E', lastName)
          const trainee = await client.query(
            "INSERT INTO erikoistuva_laakari (kayttaja_id, syntymaaika) VALUES ($1, '1980-02-01') RETURNING id",
            [resident.id]
          )
          const right = await client.query(
            `INSERT INTO opintooikeus
               (erikoistuva_laakari_id, opintooikeuden_myontamispaiva, opintooikeuden_paattymispaiva,
                opiskelijatunnus, osaamisen_arvioinnin_oppaan_pvm, yliopisto_id, erikoisala_id,
                opintoopas_id, asetus_id, kaytossa, yliopisto_opintooikeus_id, tila, muokkausaika,
                terveyskeskuskoulutusjakso_suoritettu, muokkausoikeudet_virkailijoilla, viimeinen_katselupaiva)
             VALUES ($1, CURRENT_DATE, CURRENT_DATE + INTERVAL '2 years', $2, CURRENT_DATE,
                     $3, $4, $5, $6, true, $2, 'AKTIIVINEN', CURRENT_TIMESTAMP, false, false,
                     CURRENT_DATE + INTERVAL '3 years') RETURNING id`,
            [trainee.rows[0].id, `e2e-merge-search-${resident.id}`, university, specialty, guide, regulation]
          )
          await client.query('UPDATE erikoistuva_laakari SET aktiivinen_opintooikeus = $1 WHERE id = $2',
            [right.rows[0].id, trainee.rows[0].id])
          residents.push(resident.id)
        }
        await client.query('COMMIT')
        return { token, ownId: residents[0], foreignId: residents[1], ownEmail: OWN, foreignEmail: FOREIGN }
      } catch (error) {
        await client.query('ROLLBACK')
        throw error
      }
    })
  },

  async 'db:cleanupAccountMergeSearch'() {
    return withDb(dbClient, async (client: Client) => {
      await client.query('BEGIN')
      try {
        await ensureRestoreJournal(client)
        const journal = await client.query<{ actor: ReusedActor }>(
          'SELECT actor FROM e2e_account_merge_search_restore WHERE id = 1 FOR UPDATE'
        )
        const reusedActor = journal.rows[0]?.actor
        for (const email of [OWN, FOREIGN, ACTOR]) {
          const user = await fetchUserIdsByEmailOrLogin(client, email)
          if (!user) continue
          await cleanupUser(client, user.userId, user.kayttajaId, async (db, id) => {
            await db.query('DELETE FROM rel_kayttaja__yliopisto WHERE kayttaja_id = $1', [id])
            await db.query('UPDATE erikoistuva_laakari SET aktiivinen_opintooikeus = NULL WHERE kayttaja_id = $1', [id])
            await db.query(
              `DELETE FROM opintooikeus WHERE erikoistuva_laakari_id IN
               (SELECT id FROM erikoistuva_laakari WHERE kayttaja_id = $1)`, [id]
            )
            await db.query('DELETE FROM erikoistuva_laakari WHERE kayttaja_id = $1', [id])
          })
        }
        if (reusedActor) {
          await client.query('UPDATE jhi_user SET active_authority = $1 WHERE id = $2',
            [reusedActor.activeAuthority, reusedActor.userId])
          await client.query('DELETE FROM jhi_user_authority WHERE user_id = $1 AND authority_name = ANY($2::text[])',
            [reusedActor.userId, reusedActor.addedRoles])
          if (reusedActor.addedUniversity !== null) {
            await client.query('DELETE FROM rel_kayttaja__yliopisto WHERE kayttaja_id = $1 AND yliopisto_id = $2',
              [reusedActor.id, reusedActor.addedUniversity])
          }
        }
        await client.query('DELETE FROM e2e_account_merge_search_restore WHERE id = 1')
        await client.query('COMMIT')
        return null
      } catch (error) {
        await client.query('ROLLBACK')
        throw error
      }
    })
  }
}
