import { E2E_ERIKOISTUVA_EMAIL } from '../../support/commands'

describe('Authentication', () => {
  context('Suomi.fi login', () => {
    before(() => {
      // Wipe the cached Cypress session so the full login flow always runs.
      Cypress.session.clearAllSavedSessions()
      // Remove the user from the DB so the "Aloita palvelun käyttö" (first-login)
      // screen is always shown, making the test repeatable on every run.
      cy.task('db:cleanupErikoistuva', { email: E2E_ERIKOISTUVA_EMAIL })
    })

    it('logs in successfully via Suomi.fi test identity provider', () => {
      cy.loginAsErikoistuva()

      cy.visit('/etusivu')
      cy.url().should('not.include', '/kirjautuminen')

      cy.get('main[role="main"]').should('exist')
      cy.get('.etusivu').should('exist')

      cy.contains('Erikoistumisen edistyminen').should('be.visible')
    })
  })
})
