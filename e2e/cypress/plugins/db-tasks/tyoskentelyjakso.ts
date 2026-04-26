import type { Client } from 'pg'
import { dbClient, withDb } from './db-client'
import { getOpintooikeusId } from './db-helpers'

// ─── Helpers ──────────────────────────────────────────────────────────────────

async function findExistingTyoskentelyjakso(
  client: Client,
  opintooikeusId: number
): Promise<number | null> {
  const result = await client.query(
    `SELECT id FROM tyoskentelyjakso WHERE opintooikeus_id = $1 LIMIT 1`,
    [opintooikeusId]
  )
  return result.rows[0]?.id ?? null
}

async function createTyoskentelyjakso(
  client: Client,
  opintooikeusId: number
): Promise<number> {
  const kuntaResult = await client.query(`SELECT id FROM kunta LIMIT 1`)
  const kuntaId: number = kuntaResult.rows[0].id

  const tyoskentelypaikkaId: number = (
    await client.query(
      `INSERT INTO tyoskentelypaikka (nimi, tyyppi, kunta_id)
       VALUES ('E2E Testisairaala', 'YLIOPISTOLLINEN_SAIRAALA', $1)
       RETURNING id`,
      [kuntaId]
    )
  ).rows[0].id

  return (
    await client.query(
      `INSERT INTO tyoskentelyjakso
         (alkamispaiva, paattymispaiva, osaaikaprosentti, kaytannon_koulutus,
          hyvaksytty_aiempaan_erikoisalaan, liitetty_koejaksoon,
          liitetty_terveyskeskuskoulutusjaksoon,
          tyoskentelypaikka_id, opintooikeus_id)
       VALUES ('2020-01-01', NULL, 100, 'OMAN_ERIKOISALAN_KOULUTUS',
               false, false, false, $1, $2)
       RETURNING id`,
      [tyoskentelypaikkaId, opintooikeusId]
    )
  ).rows[0].id
}

// ─── Exported task ────────────────────────────────────────────────────────────

/**
 * Tasks for seeding a työskentelyjakso (work period) for e2e test users.
 *
 * db:seedTyoskentelyjakso – Creates a työskentelyjakso for the user's active
 *                           opinto-oikeus so the arviointipyyntö form has
 *                           selectable options. Idempotent.
 */
export const tyoskentelyjaksotTasks = {
  async 'db:seedTyoskentelyjakso'({
                                    email,
                                  }: {
    email: string
  }): Promise<{ tyoskentelyjaksoId: number } | null> {
    return withDb(dbClient, async (client: any) => {
      const opintooikeusId = await getOpintooikeusId(client, email)
      if (!opintooikeusId) return null

      const existingId = await findExistingTyoskentelyjakso(client, opintooikeusId)
      if (existingId) return { tyoskentelyjaksoId: existingId }

      const tyoskentelyjaksoId = await createTyoskentelyjakso(client, opintooikeusId)
      return { tyoskentelyjaksoId }
    })
  },
}
