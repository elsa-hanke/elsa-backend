import type { Client } from 'pg'

export async function ensureYliopistoLink(client: Client, kayttajaId: number): Promise<any> {
  await client.query(
    `INSERT INTO rel_kayttaja__yliopisto (kayttaja_id, yliopisto_id)
     VALUES ($1, 1) ON CONFLICT DO NOTHING`,
    [kayttajaId]
  )
}

export async function ensureYliopistoErikoisalaLink(client: Client, kayttajaId: number): Promise<number> {
  const existing = await client.query(
    `SELECT id FROM kayttaja_yliopisto_erikoisala
     WHERE kayttaja_id = $1 AND yliopisto_id = 1 AND erikoisala_id = 46`,
    [kayttajaId]
  )
  if (existing.rows.length > 0) return existing.rows[0].id

  return (
    await client.query(
      `INSERT INTO kayttaja_yliopisto_erikoisala (kayttaja_id, yliopisto_id, erikoisala_id)
       VALUES ($1, 1, 46) RETURNING id`,
      [kayttajaId]
    )
  ).rows[0].id
}
