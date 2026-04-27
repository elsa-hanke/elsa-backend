import { kouluttajaTasks } from './kouluttaja'
import { erikoistuvaLaakariTasks } from './erikoistuva'
import { tyoskentelyjaksotTasks } from './tyoskentelyjakso'
import { pingTask } from './ping'
import { koejaksoTasks } from './koejakso'
import { vastuuhenkiloTasks } from './vastuuhenkilo'
import { virkailijaTasks } from './virkailija'

export { dbClient } from './db-client'

export function registerDbTasks(on: Cypress.PluginEvents): void {
  on('task', {
    ...kouluttajaTasks,
    ...erikoistuvaLaakariTasks,
    ...tyoskentelyjaksotTasks,
    ...pingTask,
    ...koejaksoTasks,
    ...vastuuhenkiloTasks,
    ...virkailijaTasks
  })
}

