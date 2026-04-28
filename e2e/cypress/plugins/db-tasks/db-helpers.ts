import type { Client } from 'pg'

// ─── Common ID lookups ────────────────────────────────────────────────────────

export async function getOpintooikeusId(client: Client, email: string): Promise<number | null> {
  const result = await client.query(
    `SELECT o.id
     FROM opintooikeus o
     JOIN erikoistuva_laakari el ON el.id = o.erikoistuva_laakari_id
     JOIN kayttaja k             ON k.id  = el.kayttaja_id
     JOIN jhi_user u             ON u.id  = k.user_id
     WHERE u.email = $1 AND o.kaytossa = true
     LIMIT 1`,
    [email]
  )
  return result.rows[0]?.id ?? null
}

export async function getKayttajaId(client: Client, email: string): Promise<number | null> {
  const result = await client.query(
    `SELECT k.id
     FROM kayttaja k
     JOIN jhi_user u ON u.id = k.user_id
     WHERE u.email = $1`,
    [email]
  )
  return result.rows[0]?.id ?? null
}

// ─── Shared koejakso deletion (used by both cleanupKoejakso and cleanupErikoistuva) ──

/**
 * Deletes all koejakso form rows for a given opintooikeus_id, in FK-safe order.
 * Safe to call when no rows exist.
 */
export async function deleteKoejaksoRowsByOpintooikeusId(
  client: Client,
  oid: number
): Promise<void> {
  // vastuuhenkilon_arvio asiakirjat must precede the arvio itself
  await client.query(
    `DELETE FROM asiakirja
     WHERE koejakson_vastuuhenkilon_arvio_id IN (
       SELECT id FROM koejakson_vastuuhenkilon_arvio WHERE opintooikeus_id = $1
     )`,
    [oid]
  )
  await client.query(`DELETE FROM koejakson_vastuuhenkilon_arvio   WHERE opintooikeus_id = $1`, [oid])
  await client.query(`DELETE FROM koejakson_loppukeskustelu        WHERE opintooikeus_id = $1`, [oid])
  // valiarviointi join table before the valiarviointi itself
  await client.query(
    `DELETE FROM koejakson_valiarviointi_kehittamistoimenpidekategoriat
     WHERE valiarviointi_id IN (
       SELECT id FROM koejakson_valiarviointi WHERE opintooikeus_id = $1
     )`,
    [oid]
  )
  await client.query(`DELETE FROM koejakson_valiarviointi          WHERE opintooikeus_id = $1`, [oid])
  await client.query(`DELETE FROM koejakson_kehittamistoimenpiteet WHERE opintooikeus_id = $1`, [oid])
  await client.query(`DELETE FROM koejakson_aloituskeskustelu      WHERE opintooikeus_id = $1`, [oid])
  // koulutussopimus join tables before the sopimus itself
  await client.query(
    `DELETE FROM koulutussopimuksen_kouluttaja
     WHERE koulutussopimus_id IN (
       SELECT id FROM koejakson_koulutussopimus WHERE opintooikeus_id = $1
     )`,
    [oid]
  )
  await client.query(
    `DELETE FROM koulutussopimuksen_koulutuspaikka
     WHERE koulutussopimus_id IN (
       SELECT id FROM koejakson_koulutussopimus WHERE opintooikeus_id = $1
     )`,
    [oid]
  )
  await client.query(`DELETE FROM koejakson_koulutussopimus        WHERE opintooikeus_id = $1`, [oid])
}

