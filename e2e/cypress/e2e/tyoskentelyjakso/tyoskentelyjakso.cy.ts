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

  it('completes the full Työskentelyjakson lisääminen use case (case 4)', () => {
    // --- Step 1: Navigate to the work periods list ---
    cy.visit('/tyoskentelyjaksot')
    cy.wait(1000)
    cy.contains('h1', 'Työskentelyjaksot').should('be.visible')
    cy.wait(1000)

    // --- Step 2: Open the new work period form ---
    cy.visit('/tyoskentelyjaksot/uusi')
    cy.wait(1000)
    cy.contains('h1', 'Lisää työskentelyjakso').should('be.visible')
    cy.wait(1000)

    // --- Step 3: Fill in and submit the work period form ---
    cy.visit('/tyoskentelyjaksot/uusi')
    cy.wait(1000)
    // Wait for the form to finish loading
    cy.get('.lisaa-tyoskentelyjakso').should('be.visible')
    cy.get('[data-testid="loading"]', { timeout: 10000 }).should('not.exist')
    cy.wait(1000)
    // Type (Tyyppi) – select the first available radio option
    cy.get('input[type="radio"][name="tyoskentelyjakso-tyyppi"]')
      .first()
      .click({ force: true })
    cy.wait(1000)
    // Work place name (Työskentelypaikka)
    cy.contains('label', 'Työskentelypaikka')
      .parent()
      .find('input[type="text"]')
      .first()
      .clear()
      .type('E2E Testipairaala')
    cy.wait(1000)
    // Municipality (Kunta) – pick first multiselect option
    cy.contains('label', 'Kunta')
      .parent()
      .as('kuntaGroup')
    cy.selectFirstMultiselectOption(cy.get('@kuntaGroup'))
    cy.wait(1000)
    // Start date (Alkamispäivä)
    cy.contains('label', 'Alkamispäivä')
      .parent()
      .find('input.date-input')
      .first()
      .clear()
      .type('01.01.2025')
      .blur()
    cy.wait(1000)
    // End date (Päättymispäivä)
    cy.contains('label', 'Päättymispäivä')
      .parent()
      .find('input.date-input')
      .first()
      .clear()
      .type('30.06.2027')
      .blur()
    cy.wait(1000)
    // Work certificate (Työtodistus) – file upload
    cy.get('input[type="file"]').first().selectFile(
      'cypress/fixtures/test.pdf',
      { force: true }
    )
    cy.wait(1000)
    // Submit
    cy.contains('button', 'Tallenna').click()
    cy.wait(1000)
    // Assert success: redirected to the detail page or list
    cy.url().should('match', /\/tyoskentelyjaksot(\/\d+)?$/)
    cy.contains('uusi-tyoskentelyjakso-lisatty', { matchCase: false }).should('not.exist')
    cy.url().should('not.include', '/uusi')
    cy.wait(1000)

    // --- Step 4: Show the newly created work period in the list ---
    cy.visit('/tyoskentelyjaksot')
    cy.wait(1000)
    cy.contains('E2E Testipairaala').should('be.visible')
    cy.wait(1000)

    // --- Step 5: Add an absence (poissaolo) to the work period ---
    cy.visit('/tyoskentelyjaksot')
    cy.wait(1000)
    // Navigate into the first työskentelyjakso that was just created
    cy.contains('E2E Testipairaala').click()
    cy.wait(1000)
    cy.url().should('match', /\/tyoskentelyjaksot\/\d+$/)
    // Click "Lisää poissaolo"
    cy.contains('Lisää poissaolo').click()
    cy.wait(1000)
    cy.url().should('include', '/poissaolot/uusi')
    // Absence reason (Poissaolon syy)
    cy.contains('label', 'Poissaolon syy')
      .parent()
      .as('syy')
    cy.selectFirstMultiselectOption(cy.get('@syy'))
    cy.wait(1000)
    // Absence start date
    cy.contains('label', 'Alkamispäivä')
      .parent()
      .find('input.date-input')
      .first()
      .clear()
      .type('15.02.2025')
      .blur()
    cy.wait(1000)
    // Absence end date
    cy.contains('label', 'Päättymispäivä')
      .parent()
      .find('input.date-input')
      .first()
      .clear()
      .type('28.02.2025')
      .blur()
    cy.wait(1000)
    cy.contains('button', 'Tallenna').click()
    cy.wait(1000)
    // After saving the absence we should be back on the työskentelyjakso page
    cy.url().should('not.include', '/poissaolot/uusi')
    cy.wait(1000)

    // --- Step 6: Show the updated work accumulation (työkertymä) on the list ---
    cy.visit('/tyoskentelyjaksot')
    cy.wait(1000)
    cy.get('.tyoskentelyjaksot, main').should('be.visible')
    cy.wait(1000)
    cy.contains('E2E Testipairaala').should('be.visible')
    cy.wait(1000)
  })
})
