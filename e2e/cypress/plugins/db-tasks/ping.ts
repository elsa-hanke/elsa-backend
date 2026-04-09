import { dbClient } from './db-client'

export const pingTask = {
  async 'db:ping'(): Promise<boolean> {
    const client = dbClient()
    try {
      await client.connect()
      await client.query('SELECT 1')
      return true
    } catch {
      return false
    } finally {
      await client.end().catch(() => undefined)
    }
  },
}

