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

export async function withDb<T>(
  createClient: () => ReturnType<typeof import('./db-client').dbClient>,
  fn: (client: Client) => Promise<T>
): Promise<T> {
  const client = createClient()
  await client.connect()
  try {
    return await fn(client as unknown as Client)
  } finally {
    await client.end()
  }
}

