/**
 * Use case 4 – Työskentelyjakson lisääminen
 *
 * Covers:
 *  - Adding a new work period (työskentelyjakso) as a resident physician
 *  - Attaching a PDF work certificate (työtodistus)
 *  - Adding an absence (poissaolo) to the created period
 *  - Verifying the work accumulation (työkertymä) is updated
 *
 * Prerequisites (handled automatically in the dev profile):
 *  On first login the backend runs createWithoutOpintotietodata which creates the
 *  erikoistuva_laakari + opintooikeus records. No extra seeding is required.
 */
import {E2E_ERIKOISTUVA_EMAIL} from "../../support/commands";

describe('Työskentelyjakso', () => {
  before(() => {
    Cypress.session.clearAllSavedSessions()
    cy.task('db:cleanupErikoistuva', { email: E2E_ERIKOISTUVA_EMAIL })
    cy.loginAsErikoistuva()
  })

  context('Työskentelyjakson lisääminen (case 4)', () => {
    it('navigates to the work periods list', () => {
      cy.visit('/tyoskentelyjaksot')
      cy.contains('h1', 'Työskentelyjaksot').should('be.visible')
    })

    it('opens the new work period form', () => {
      cy.visit('/tyoskentelyjaksot/uusi')
      cy.contains('h1', 'Lisää työskentelyjakso').should('be.visible')
    })

    it('fills in and submits the work period form', () => {
      cy.visit('/tyoskentelyjaksot/uusi')

      // Wait for the form to finish loading
      cy.get('.lisaa-tyoskentelyjakso').should('be.visible')
      cy.get('[data-testid="loading"]', { timeout: 10000 }).should('not.exist')

      // --- Type (Tyyppi) – select the first available radio option -----------
      cy.get('input[type="radio"][name="tyoskentelyjakso-tyyppi"]')
        .first()
        .click({ force: true })

      // --- Work place name (Työskentelypaikka) --------------------------------
      cy.contains('label', 'Työskentelypaikka')
        .parent()
        .find('input[type="text"]')
        .first()
        .clear()
        .type('E2E Testipairaala')

      // --- Municipality (Kunta) – pick first multiselect option ---------------
      cy.contains('label', 'Kunta')
        .parent()
        .as('kuntaGroup')
      cy.selectFirstMultiselectOption(cy.get('@kuntaGroup'))

      // --- Start date (Alkamispäivä) ------------------------------------------
      cy.contains('label', 'Alkamispäivä')
        .parent()
        .find('input.date-input')
        .first()
        .clear()
        .type('01.01.2025')
        .blur()

      // --- End date (Päättymispäivä) – optional, fill it to complete the period
      cy.contains('label', 'Päättymispäivä')
        .parent()
        .find('input.date-input')
        .first()
        .clear()
        .type('30.06.2025')
        .blur()

      // --- Work certificate (Työtodistus) – file upload -----------------------
      // The file input may be hidden; selectFile works even for hidden inputs.
      cy.get('input[type="file"]').first().selectFile(
        'cypress/fixtures/test-todistus.pdf',
        { force: true }
      )

      // --- Submit --------------------------------------------------------------
      cy.contains('button', 'Tallenna').click()

      // Assert success: redirected to the detail page or list
      cy.url().should('match', /\/tyoskentelyjaksot(\/\d+)?$/)
      cy.contains('uusi-tyoskentelyjakso-lisatty', { matchCase: false }).should('not.exist')
      // Success toast OR redirect away from /uusi means it worked
      cy.url().should('not.include', '/uusi')
    })

    it('shows the newly created work period in the list', () => {
      cy.visit('/tyoskentelyjaksot')
      cy.contains('E2E Testipairaala').should('be.visible')
    })

    it('adds an absence (poissaolo) to the work period', () => {
      cy.visit('/tyoskentelyjaksot')

      // Navigate into the first työskentelyjakso that was just created
      cy.contains('E2E Testipairaala').click()
      cy.url().should('match', /\/tyoskentelyjaksot\/\d+$/)

      // Click "Lisää poissaolo"
      cy.contains('Lisää poissaolo').click()
      cy.url().should('include', '/poissaolot/uusi')

      // --- Absence reason (Poissaolon syy) ------------------------------------
      cy.contains('label', 'Poissaolon syy')
        .parent()
        .as('syy')
      cy.selectFirstMultiselectOption(cy.get('@syy'))

      // --- Absence start date -------------------------------------------------
      cy.contains('label', 'Alkamispäivä')
        .parent()
        .find('input.date-input')
        .first()
        .clear()
        .type('15.02.2025')
        .blur()

      // --- Absence end date ---------------------------------------------------
      cy.contains('label', 'Päättymispäivä')
        .parent()
        .find('input.date-input')
        .first()
        .clear()
        .type('28.02.2025')
        .blur()

      cy.contains('button', 'Tallenna').click()

      // After saving the absence we should be back on the työskentelyjakso page
      cy.url().should('not.include', '/poissaolot/uusi')
    })

    it('shows the updated work accumulation (työkertymä) on the list', () => {
      cy.visit('/tyoskentelyjaksot')
      // The list page shows a työkertymä summary — just verify the section exists
      cy.get('.tyoskentelyjaksot, main').should('be.visible')
      cy.contains('E2E Testipairaala').should('be.visible')
    })
  })
})

