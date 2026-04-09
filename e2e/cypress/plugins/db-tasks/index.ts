import { kouluttajaTasks } from './kouluttaja'
import { erikoistuvaLaakariTasks } from './erikoistuva'
import { tyoskentelyjaksotTasks } from './tyoskentelyjakso'
import { pingTask } from './ping'

export { dbClient } from './db-client'

export function registerDbTasks(on: Cypress.PluginEvents): void {
  on('task', {
    ...kouluttajaTasks,
    ...erikoistuvaLaakariTasks,
    ...tyoskentelyjaksotTasks,
    ...pingTask,
  })
}

