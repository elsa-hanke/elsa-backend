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

