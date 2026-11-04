import { randomUUID } from 'node:crypto'
import { dbClient, withDb } from './db-client'

// Node-owned marker also allows cleanup if an API write succeeds but an assertion fails.
const toolName = `E2E-arviointityokalu-${randomUUID()}`

export const assessmentToolTasks = {
  'db:assessmentToolName'(): string {
    return toolName
  },

  async 'db:cleanupAssessmentTool'(): Promise<null> {
    return withDb(dbClient, async (client) => {
      // This spec creates no questions, attachments or assessment references.
      // All versions keep the same unique name.
      await client.query('DELETE FROM arviointityokalu WHERE nimi = $1', [toolName])
      return null
    })
  },
}
