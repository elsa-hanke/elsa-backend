import { defineConfig } from 'cypress'

export default defineConfig({
  e2e: {
    baseUrl: 'http://localhost:8080',
    specPattern: 'cypress/e2e/**/*.cy.ts',
    supportFile: 'cypress/support/e2e.ts',
    setupNodeEvents(on, config) {
      // Disable SameSite enforcement so the SAML ACS cross-site POST
      // (testi.apro.tunnistus.fi → localhost:8080) includes the session cookie.
      // This is required on Linux CI where --network=host gives true host
      // networking and Chrome enforces SameSite=Lax strictly.
      on('before:browser:launch', (browser, launchOptions) => {
        if (browser.family === 'chromium') {
          launchOptions.args.push('--disable-features=SameSiteByDefaultCookies')
          launchOptions.args.push('--disable-features=CookiesWithoutSameSiteMustBeSecure')
        }
        return launchOptions
      })
    },
    viewportWidth: 1280,
    viewportHeight: 800,
    defaultCommandTimeout: 30000,
    video: true,
    screenshotOnRunFailure: true,
    experimentalModifyObstructiveThirdPartyCode: true,
  },
})

