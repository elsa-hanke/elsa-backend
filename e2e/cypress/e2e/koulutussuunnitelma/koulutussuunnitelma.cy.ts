/**
 * Use case 5 – Koulutusjakson lisääminen koulutussuunnitelmaan
 *
 * Covers:
 *  - Adding a training period (koulutusjakso) as a resident physician
 *  - Filling in the period name and optional learning objectives
 *  - Verifying the new period appears in the training plan view
 *
 * Prerequisites (dev profile):
 *  On first login createWithoutOpintotietodata creates all required records.
 *  No extra seeding needed.
 */
import {E2E_ERIKOISTUVA_EMAIL} from "../../support/commands";

describe('Koulutussuunnitelma', () => {
  before(() => {
    Cypress.session.clearAllSavedSessions()
    cy.task('db:cleanupErikoistuva', { email: E2E_ERIKOISTUVA_EMAIL })
    cy.loginAsErikoistuva()
  })

  context('Koulutusjakson lisääminen (case 5)', () => {
    it('navigates to the training plan', () => {
      cy.visit('/koulutussuunnitelma')
      cy.contains('h1', 'Koulutussuunnitelma').should('be.visible')
    })

    it('opens the new training period form', () => {
      cy.visit('/koulutussuunnitelma/koulutusjaksot/uusi')
      cy.contains('h1', 'Lisää koulutusjakso').should('be.visible')
    })

    it('fills in and submits the training period form', () => {
      cy.visit('/koulutussuunnitelma/koulutusjaksot/uusi')

      // Wait for the form to finish loading (spinner disappears)
      cy.get('.lisaa-koulutusjakso').should('be.visible')
      cy.get('[role="status"]', { timeout: 10000 }).should('not.exist')

      // --- Period name (Koulutusjakson nimi) – required ----------------------
      cy.contains('label', 'Koulutusjakson nimi')
        .parent()
        .find('input[type="text"]')
        .first()
        .clear()
        .type('E2E Testi Koulutusjakso')

      // --- Other objectives (Muut osaamistavoitteet) – optional textarea -----
      cy.contains('label', 'Muut osaamistavoitteet')
        .parent()
        .find('textarea')
        .type('Testataan e2e-automaatiolla.')

      // --- Submit -------------------------------------------------------------
      cy.contains('button', 'Tallenna koulutusjakso').click()

      // After saving we should be redirected to the koulutusjakso detail page
      cy.url().should('match', /\/koulutussuunnitelma\/koulutusjaksot\/\d+$/)
    })

    it('shows the newly created training period in the training plan', () => {
      cy.visit('/koulutussuunnitelma')
      cy.contains('E2E Testi Koulutusjakso').should('be.visible')
    })
  })
})

