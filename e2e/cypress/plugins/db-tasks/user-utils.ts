import { randomUUID } from 'crypto'
import type { Client } from 'pg'

export async function fetchUserIdsByEmailOrLogin(
  client: Client,
  emailOrLogin: string
): Promise<{ userId: string; kayttajaId: number } | null> {
  const result = await client.query(
    `SELECT u.id AS user_id, k.id AS kayttaja_id
     FROM jhi_user u
     LEFT JOIN kayttaja k ON k.user_id = u.id
     WHERE u.email = $1 OR u.login = $1`,
    [emailOrLogin]
  )
  if (result.rows.length === 0) return null
  return { userId: result.rows[0].user_id, kayttajaId: result.rows[0].kayttaja_id }
}

export async function ensureUserAuthority(
  client: Client,
  userId: string,
  role: string
): Promise<void> {
  await client.query(
    `INSERT INTO jhi_user_authority (user_id, authority_name)
     VALUES ($1, $2) ON CONFLICT DO NOTHING`,
    [userId, role]
  )
}

export async function createVerificationToken(
  client: Client,
  userId: string
): Promise<string> {
  const token = randomUUID()
  await client.query(`DELETE FROM verification_token WHERE user_id = $1`, [userId])
  await client.query(
    `INSERT INTO verification_token (id, user_id) VALUES ($1, $2)`,
    [token, userId]
  )
  return token
}

export async function createUserWithRole(
  client: Client,
  {
    email,
    etunimi,
    sukunimi,
    role,
    linkFn,
  }: {
    email: string
    etunimi: string
    sukunimi: string
    role: string
    linkFn: (client: Client, kayttajaId: number) => Promise<number>
  }
): Promise<number> {
  const userId = randomUUID()
  const nimi = `${etunimi} ${sukunimi}`

  await client.query(
    `INSERT INTO jhi_user
       (id, login, first_name, last_name, email, activated, lang_key,
        created_by, last_modified_by, active_authority)
     VALUES ($1,$2,$3,$4,$5,true,'fi','system','system',$6)`,
    [userId, email, etunimi, sukunimi, email, role]
  )
  await ensureUserAuthority(client, userId, role)

  const kayttajaId: number = (
    await client.query(
      `INSERT INTO kayttaja (nimike, user_id, tila)
       VALUES ($1,$2,'AKTIIVINEN')
       RETURNING id`,
      [nimi, userId]
    )
  ).rows[0].id

  await linkFn(client, kayttajaId)

  return kayttajaId
}

export async function cleanupUser(
  client: Client,
  userId: string,
  kayttajaId: number | null,
  linkCleanupFn?: (client: Client, kayttajaId: number) => Promise<void>
): Promise<void> {
  if (kayttajaId) {
    if (linkCleanupFn) {
      await linkCleanupFn(client, kayttajaId)
    }
    await client.query(`DELETE FROM kayttaja WHERE id = $1`, [kayttajaId])
  }
  await client.query(`DELETE FROM verification_token WHERE user_id = $1`, [userId])
  await client.query(`DELETE FROM jhi_user_authority WHERE user_id = $1`, [userId])
  await client.query(`DELETE FROM jhi_user WHERE id = $1`, [userId])
}

