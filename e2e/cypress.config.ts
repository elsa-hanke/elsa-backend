import { defineConfig } from 'cypress'

export default defineConfig({
  e2e: {
    baseUrl: 'http://localhost:8080',
    specPattern: 'cypress/e2e/**/*.cy.ts',
    supportFile: 'cypress/support/e2e.ts',
    setupNodeEvents(on, config) {
      // implement node event listeners here
    },
    viewportWidth: 1280,
    viewportHeight: 800,
    defaultCommandTimeout: 30000,
    video: false,
    screenshotOnRunFailure: true,
    experimentalModifyObstructiveThirdPartyCode: true,
  },
})

