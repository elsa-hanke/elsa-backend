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
  opintooikeusId: number,
  liitettyKoejaksoon = false
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
  const alkamispaiva = liitettyKoejaksoon ? '2025-01-01' : '2020-01-01'
  const paattymispaiva = liitettyKoejaksoon ? '2025-08-31' : null

  return (
    await client.query(
      `INSERT INTO tyoskentelyjakso
         (alkamispaiva, paattymispaiva, osaaikaprosentti, kaytannon_koulutus,
          hyvaksytty_aiempaan_erikoisalaan, liitetty_koejaksoon,
          liitetty_terveyskeskuskoulutusjaksoon,
          tyoskentelypaikka_id, opintooikeus_id)
       VALUES ($3, $4, 100, 'OMAN_ERIKOISALAN_KOULUTUS',
               false, $5, false, $1, $2)
       RETURNING id`,
      [tyoskentelypaikkaId, opintooikeusId, alkamispaiva, paattymispaiva, liitettyKoejaksoon]
    )
  ).rows[0].id
}

async function ensureKoejaksoAttachment(
  client: Client,
  opintooikeusId: number,
  tyoskentelyjaksoId: number
): Promise<void> {
  await client.query(
    `UPDATE tyoskentelyjakso
     SET liitetty_koejaksoon = true,
         alkamispaiva = '2025-01-01',
         paattymispaiva = '2025-08-31',
         osaaikaprosentti = 100
     WHERE id = $1`,
    [tyoskentelyjaksoId]
  )

  await client.query(
    `INSERT INTO asiakirja_data (data)
     SELECT decode('255044462d312e370a25454f460a', 'hex')
     WHERE NOT EXISTS (
       SELECT 1 FROM asiakirja WHERE tyoskentelyjakso_id = $1
     )`,
    [tyoskentelyjaksoId]
  )

  await client.query(
    `INSERT INTO asiakirja (opintooikeus_id, tyoskentelyjakso_id, nimi, tyyppi, lisattypvm, asiakirja_data_id)
     SELECT $1, $2, 'e2e-koejakso-tyotodistus.pdf', 'application/pdf', NOW(), currval(pg_get_serial_sequence('asiakirja_data', 'id'))
     WHERE NOT EXISTS (
       SELECT 1 FROM asiakirja WHERE tyoskentelyjakso_id = $2
     )`,
    [opintooikeusId, tyoskentelyjaksoId]
  )
}

// ─── Exported task ────────────────────────────────────────────────────────────

/**
 * Tasks for seeding a työskentelyjakso (work period) for e2e test users.
 *
 * db:seedTyoskentelyjakso – Creates a työskentelyjakso for the user's active
 *                           opinto-oikeus so the arviointipyyntö form has
 *                           selectable options. Idempotent.
 * db:ensureKoejaksoTyoskentelyjakso – Creates or updates a työskentelyjakso
 *                           that satisfies the vastuuhenkilön arvio prechecks.
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

  async 'db:ensureKoejaksoTyoskentelyjakso'({
                                              email,
                                            }: {
    email: string
  }): Promise<{ tyoskentelyjaksoId: number } | null> {
    return withDb(dbClient, async (client: any) => {
      const opintooikeusId = await getOpintooikeusId(client, email)
      if (!opintooikeusId) return null

      const existingId = await findExistingTyoskentelyjakso(client, opintooikeusId)
      const tyoskentelyjaksoId =
        existingId ?? (await createTyoskentelyjakso(client, opintooikeusId, true))

      await ensureKoejaksoAttachment(client, opintooikeusId, tyoskentelyjaksoId)
      return { tyoskentelyjaksoId }
    })
  },
}
