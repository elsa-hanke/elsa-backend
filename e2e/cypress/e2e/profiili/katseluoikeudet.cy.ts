import {E2E_ERIKOISTUVA_EMAIL} from "../../support/commands";

export {}

/**
 * Use case 1 – Katseluoikeuden lisääminen kouluttajalle
 *
 * Covers:
 *  - Resident physician navigates to Oma profiili → Katseluoikeudet tab
 *  - A pre-seeded kouluttaja is visible in the dropdown
 *    (seeded via db:seedKouluttaja so no email invite is sent)
 *  - Resident physician grants view access (myönnä oikeus)
 *  - Kouluttaja card appears in the Katseluoikeudet list
 *
 * Note: The pre-seeded kouluttaja uses the same yliopisto (1) and
 * erikoisala (46 = Työterveyshuolto) assigned by createWithoutOpintotietodata.
 */

const KOULUTTAJA_EMAIL   = 'test-kouluttaja@test.elsa'
const KOULUTTAJA_ETUNIMI = 'Testi'
const KOULUTTAJA_SUKUNIMI = 'Kouluttaja'

describe('Katseluoikeudet', () => {
  before(() => {
    Cypress.session.clearAllSavedSessions()
    cy.task('db:cleanupErikoistuva', { email: E2E_ERIKOISTUVA_EMAIL })
    // Seed the kouluttaja before logging in as erikoistuva.
    // Idempotent — safe to call on repeated runs.
    cy.task('db:seedKouluttaja', {
      email: KOULUTTAJA_EMAIL,
      etunimi: KOULUTTAJA_ETUNIMI,
      sukunimi: KOULUTTAJA_SUKUNIMI,
    })

    cy.loginAsErikoistuva()
  })

  after(() => {
    // Clean up so the test is fully idempotent across CI runs.
    // Remove view-access grant first, then the seeded kouluttaja.
    cy.task('db:cleanupKouluttaja', { email: KOULUTTAJA_EMAIL })
  })

  context('Katseluoikeuden lisääminen kouluttajalle (case 1)', () => {
    it('navigates, selects, grants, and verifies katseluoikeus for kouluttaja', () => {
      // Navigates to the profile and opens the Katseluoikeudet tab
      cy.visit('/profiili#katseluoikeudet')
      cy.contains('h1', 'Oma profiili').should('be.visible')
      cy.contains('.nav-link', 'Katseluoikeudet').should('be.visible')

      // Shows the seeded kouluttaja in the dropdown
      cy.contains('.nav-link', 'Katseluoikeudet').click()
      cy.contains('Katseluoikeudet').should('be.visible')
      cy.contains('label', 'Kouluttaja')
        .parent()
        .as('kouluttajaGroup')
      cy.get('@kouluttajaGroup')
        .find('.multiselect')
        .click()
      cy.get('.multiselect__content .multiselect__option')
        .contains(`${KOULUTTAJA_ETUNIMI} ${KOULUTTAJA_SUKUNIMI}`)
        .should('be.visible')
        .click()

      // Grants view access to the kouluttaja
      cy.contains('button', 'Myönnä oikeus').click()
      cy.contains(`${KOULUTTAJA_ETUNIMI} ${KOULUTTAJA_SUKUNIMI}`).should('be.visible')
      cy.contains(KOULUTTAJA_EMAIL).should('be.visible')

      // Shows the kouluttaja card after page reload
      cy.visit('/profiili#katseluoikeudet')
      cy.contains('.nav-link', 'Katseluoikeudet').click()
      cy.contains(`${KOULUTTAJA_ETUNIMI} ${KOULUTTAJA_SUKUNIMI}`).should('be.visible')
    })
  })
})
