import { defineConfig } from 'cypress'
import { mkdirSync, writeFileSync } from 'node:fs'
import path from 'node:path'
import { registerDbTasks } from './cypress/plugins/db-tasks'

export default defineConfig({
  e2e: {
    baseUrl: 'http://localhost:8080',
    specPattern: 'cypress/e2e/**/*.cy.ts',
    supportFile: 'cypress/support/e2e.ts',
    setupNodeEvents(on) {
      registerDbTasks(on)
      // Persist each completed spec so interrupted shards retain partial results.
      const resultsDirectory = process.env.E2E_RESULTS_DIR
      if (resultsDirectory) {
        mkdirSync(resultsDirectory, { recursive: true })
        on('after:spec', (spec, results) => {
          if (!results) return
          writeFileSync(path.join(resultsDirectory, `${encodeURIComponent(spec.relative)}.json`), JSON.stringify({
            spec: spec.relative.replace(/\\/g, '/'),
            stats: results.stats,
          }, null, 2))
        })
        on('after:run', (results) => {
          if ('runs' in results) {
            writeFileSync(path.join(resultsDirectory, 'completed.json'), JSON.stringify({ completed: true }))
          }
        })
      }
    },
    viewportWidth: 1280,
    viewportHeight: 800,
    defaultCommandTimeout: 30000,
    video: false,
    screenshotOnRunFailure: true,
    experimentalModifyObstructiveThirdPartyCode: true,
  },
})
