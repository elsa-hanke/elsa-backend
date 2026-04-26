import type { Client } from 'pg'
import { dbClient, withDb } from './db-client'
import {
  deleteKoejaksoRowsByOpintooikeusId,
} from './db-helpers'

// ─── ID lookup ────────────────────────────────────────────────────────────────

interface UserIds {
  user_id: number
  kayttaja_id: number | null
  el_id: number | null
}

async function fetchUserIds(client: Client, email: string): Promise<UserIds | null> {
  const result = await client.query(
    `SELECT u.id AS user_id, k.id AS kayttaja_id, el.id AS el_id
     FROM jhi_user u
     LEFT JOIN kayttaja k ON k.user_id = u.id
     LEFT JOIN erikoistuva_laakari el ON el.kayttaja_id = k.id
     WHERE u.email = $1`,
    [email]
  )
  return result.rows.length === 0 ? null : result.rows[0]
}

// ─── Valmistumispyynto cleanup ────────────────────────────────────────────────

async function deleteValmistumispyyntoRows(client: Client, el_id: number): Promise<void> {
  await client.query(
    `DELETE FROM valmistumispyynnon_tarkistus
     WHERE valmistumispyynto_id IN (
       SELECT id FROM valmistumispyynto
       WHERE opintooikeus_id IN (SELECT id FROM opintooikeus WHERE erikoistuva_laakari_id = $1)
     )`,
    [el_id]
  )
  await client.query(
    `DELETE FROM valmistumispyynto
     WHERE opintooikeus_id IN (SELECT id FROM opintooikeus WHERE erikoistuva_laakari_id = $1)`,
    [el_id]
  )
}

// ─── Koulutussuunnitelma cleanup ──────────────────────────────────────────────

async function deleteKoulutussuunnitelmaRows(client: Client, el_id: number): Promise<void> {
  await client.query(
    `DELETE FROM koulutusjakso
     WHERE koulutussuunnitelma_id IN (
       SELECT id FROM koulutussuunnitelma
       WHERE opintooikeus_id IN (SELECT id FROM opintooikeus WHERE erikoistuva_laakari_id = $1)
     )`,
    [el_id]
  )
  await client.query(
    `DELETE FROM koulutussuunnitelma
     WHERE opintooikeus_id IN (SELECT id FROM opintooikeus WHERE erikoistuva_laakari_id = $1)`,
    [el_id]
  )
}

// ─── Tyoskentelyjakso cleanup ─────────────────────────────────────────────────

async function fetchTyoskentelypaikkaIds(client: Client, el_id: number): Promise<number[]> {
  const result = await client.query(
    `SELECT tyoskentelypaikka_id FROM tyoskentelyjakso
     WHERE opintooikeus_id IN (SELECT id FROM opintooikeus WHERE erikoistuva_laakari_id = $1)`,
    [el_id]
  )
  return result.rows.map((r: { tyoskentelypaikka_id: number }) => r.tyoskentelypaikka_id)
}

async function deleteTyoskentelyjaksoRows(client: Client, el_id: number): Promise<void> {
  await client.query(
    `DELETE FROM suoritusarvioinnin_arvioitava_kokonaisuus
     WHERE suoritusarviointi_id IN (
       SELECT id FROM suoritusarviointi
       WHERE tyoskentelyjakso_id IN (
         SELECT id FROM tyoskentelyjakso
         WHERE opintooikeus_id IN (SELECT id FROM opintooikeus WHERE erikoistuva_laakari_id = $1)
       )
     )`,
    [el_id]
  )
  await client.query(
    `DELETE FROM suoritusarvioinnin_kommentti
     WHERE suoritusarviointi_id IN (
       SELECT id FROM suoritusarviointi
       WHERE tyoskentelyjakso_id IN (
         SELECT id FROM tyoskentelyjakso
         WHERE opintooikeus_id IN (SELECT id FROM opintooikeus WHERE erikoistuva_laakari_id = $1)
       )
     )`,
    [el_id]
  )
  await client.query(
    `DELETE FROM suoritusarviointi
     WHERE tyoskentelyjakso_id IN (
       SELECT id FROM tyoskentelyjakso
       WHERE opintooikeus_id IN (SELECT id FROM opintooikeus WHERE erikoistuva_laakari_id = $1)
     )`,
    [el_id]
  )
  await client.query(
    `DELETE FROM keskeytysaika
     WHERE tyoskentelyjakso_id IN (
       SELECT id FROM tyoskentelyjakso
       WHERE opintooikeus_id IN (SELECT id FROM opintooikeus WHERE erikoistuva_laakari_id = $1)
     )`,
    [el_id]
  )
  await client.query(
    `DELETE FROM asiakirja
     WHERE tyoskentelyjakso_id IN (
       SELECT id FROM tyoskentelyjakso
       WHERE opintooikeus_id IN (SELECT id FROM opintooikeus WHERE erikoistuva_laakari_id = $1)
     )`,
    [el_id]
  )
  await client.query(
    `DELETE FROM tyoskentelyjakso
     WHERE opintooikeus_id IN (SELECT id FROM opintooikeus WHERE erikoistuva_laakari_id = $1)`,
    [el_id]
  )
}

async function deleteTyoskentelypaikkaRows(client: Client, paikkaIds: number[]): Promise<void> {
  if (paikkaIds.length === 0) return
  await client.query(`DELETE FROM tyoskentelypaikka WHERE id = ANY($1::bigint[])`, [paikkaIds])
}

// ─── Opintooikeus cleanup ─────────────────────────────────────────────────────

async function deleteOpintooikeusRows(client: Client, el_id: number): Promise<void> {
  await client.query(
    `DELETE FROM asiakirja
     WHERE opintooikeus_id IN (SELECT id FROM opintooikeus WHERE erikoistuva_laakari_id = $1)`,
    [el_id]
  )
  await client.query(`DELETE FROM opintooikeus_herate WHERE erikoistuva_laakari_id = $1`, [el_id])
  await client.query(
    `DELETE FROM kouluttajavaltuutus
     WHERE valtuuttaja_opintooikeus_id IN (
       SELECT id FROM opintooikeus WHERE erikoistuva_laakari_id = $1
     )`,
    [el_id]
  )
  await client.query(`DELETE FROM opintooikeus WHERE erikoistuva_laakari_id = $1`, [el_id])
}

// ─── Top-level orchestration ──────────────────────────────────────────────────

async function deleteErikoistuvaLaakari(client: Client, ids: UserIds): Promise<void> {
  const { el_id, kayttaja_id, user_id } = ids

  if (el_id) {
    const paikkaIds = await fetchTyoskentelypaikkaIds(client, el_id)

    // Reuse the shared koejakso cleanup — same FK order as db:cleanupKoejakso
    const opintooikeusRows = await client.query(
      `SELECT id FROM opintooikeus WHERE erikoistuva_laakari_id = $1`,
      [el_id]
    )
    for (const row of opintooikeusRows.rows as { id: number }[]) {
      await deleteKoejaksoRowsByOpintooikeusId(client, row.id)
    }

    await deleteValmistumispyyntoRows(client, el_id)
    await deleteKoulutussuunnitelmaRows(client, el_id)
    await deleteTyoskentelyjaksoRows(client, el_id)
    await deleteTyoskentelypaikkaRows(client, paikkaIds)
    await deleteOpintooikeusRows(client, el_id)

    await client.query(`DELETE FROM erikoistuva_laakari WHERE id = $1`, [el_id])
  }

  if (kayttaja_id) {
    await client.query(`DELETE FROM kayttaja WHERE id = $1`, [kayttaja_id])
  }

  await client.query(`DELETE FROM jhi_user_authority WHERE user_id = $1`, [user_id])
  await client.query(`DELETE FROM jhi_user WHERE id = $1`, [user_id])
}

// ─── Exported task ────────────────────────────────────────────────────────────

/**
 * Tasks for cleaning up an erikoistuva lääkäri (resident physician) test user
 * and all of their associated data.
 *
 * db:cleanupErikoistuva – Removes the user and every related row in FK-dependency
 *                         order. Safe to call when the user does not exist.
 *                         Call before first-login tests so the "Aloita palvelun
 *                         käyttö" screen is always shown on the next login.
 */
export const erikoistuvaLaakariTasks = {
  async 'db:cleanupErikoistuva'({ email }: { email: string }): Promise<null> {
    return withDb(dbClient, async (client: any) => {
      const ids = await fetchUserIds(client, email)
      if (!ids) return null
      await deleteErikoistuvaLaakari(client, ids)
      return null
    })
  },
}
