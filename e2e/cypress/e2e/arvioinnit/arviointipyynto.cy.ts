import {E2E_ERIKOISTUVA_EMAIL} from "../../support/commands";

export {}

/**
 * Use cases 2 & 3 – Arviointipyyntö (assessment request)
 *
 * Case 2 – Resident physician (erikoistuva):
 *  - Navigates to Osaaminen → Arvioinnit
 *  - Creates a new assessment request (arviointipyyntö) selecting the pre-seeded
 *    kouluttaja as the evaluator
 *  - Submits the request
 *  - Fills in self-assessment (itsearviointi) and saves
 *
 * Case 3 – Trainer (kouluttaja):
 *  - Sees the open assessment on the dashboard (Avoimet asiat)
 *  - Fills in the assessment using an arviointityokalu
 *  - Submits
 *
 * IMPORTANT – kouluttaja login in CI/CD:
 *  The kouluttaja is pre-seeded in the DB without a hashed SSN.  To log in as
 *  the kouluttaja in a clean environment the first login MUST go through the
 *  verification-token invite link (RelayState = token).  For now that test is
 *  marked pending; see the comment inside the describe block.
 *
 * Prerequisites (dev profile):
 *  createWithoutOpintotietodata ensures the erikoistuva has an opintooikeus.
 *  db:seedKouluttaja ensures the kouluttaja is available in the evaluator dropdown.
 */

const KOULUTTAJA_EMAIL    = 'test-kouluttaja@test.elsa'
const KOULUTTAJA_ETUNIMI  = 'Testi'
const KOULUTTAJA_SUKUNIMI = 'Kouluttaja'

describe.skip('Arviointipyyntö', () => {
  // ── Seed kouluttaja once for the whole suite ────────────────────────────────
  before(() => {
    Cypress.session.clearAllSavedSessions()
    cy.task('db:cleanupErikoistuva', { email: E2E_ERIKOISTUVA_EMAIL })
    cy.task('db:seedKouluttaja', {
      email: KOULUTTAJA_EMAIL,
      etunimi: KOULUTTAJA_ETUNIMI,
      sukunimi: KOULUTTAJA_SUKUNIMI,
    })
  })

  // ── Case 2 – Erikoistuva side ───────────────────────────────────────────────
  context('Case 2 – Arviointipyynnön tekeminen ja itsearviointi (erikoistuva)', () => {
    beforeEach(() => {
      cy.loginAsErikoistuva()
    })

    it('navigates to the Arvioinnit page', () => {
      cy.visit('/arvioinnit')
      cy.contains('h1', 'Arvioinnit').should('be.visible')
    })

    it('opens the new assessment request form', () => {
      cy.visit('/arvioinnit/arviointipyynto')
      cy.contains('h1', 'Pyydä arviointia').should('be.visible')
    })

    it('fills in and submits the assessment request', () => {
      cy.visit('/arvioinnit/arviointipyynto')

      // Wait for the form to finish loading
      cy.get('.arviointipyynto').should('be.visible')
      cy.get('[role="status"]', { timeout: 10000 }).should('not.exist')

      // --- Work period (Työskentelyjakso) – pick from multiselect ------------
      cy.contains('label', 'Työskentelyjakso')
        .parent()
        .as('jaksoGroup')
      cy.selectFirstMultiselectOption(cy.get('@jaksoGroup'))

      // --- Assessable entity (Arvioitava kokonaisuus) – pick first ----------
      cy.contains('label', 'Arvioitava kokonaisuus')
        .parent()
        .as('kokonaisuusGroup')
      cy.selectFirstMultiselectOption(cy.get('@kokonaisuusGroup'))

      // --- Evaluator (Arvioija) – select the seeded kouluttaja ---------------
      cy.contains('label', 'Arvioija')
        .parent()
        .find('.multiselect')
        .click()

      cy.get('.multiselect__content .multiselect__option')
        .contains(`${KOULUTTAJA_ETUNIMI} ${KOULUTTAJA_SUKUNIMI}`)
        .click()

      // --- Event description (optional) --------------------------------------
      cy.get('textarea').first().type('E2E testitapahtuma arviointipyyntöä varten.')

      // --- Submit -------------------------------------------------------------
      cy.contains('button', 'Lähetä').click()

      // After sending the request, the app redirects to the
      // "arviointipyynto-lahetetty" page which shows a link to itsearviointi.
      cy.url().should('include', 'arviointipyynto-lahetetty')
      cy.contains('Arviointipyyntö lähetetty').should('be.visible')
    })

    it('completes the self-assessment (itsearviointi)', () => {
      // After the redirect we should have a link to the itsearviointi page.
      // Navigate there directly by using the assessment that was just created.
      cy.visit('/arvioinnit')

      // Find the just-created assessment request and open it
      cy.contains('E2E testitapahtuma arviointipyyntöä varten.')
        .closest('tr, .list-item, [class*="row"]')
        .contains('Itsearviointi')
        .click({ force: true })

      cy.url().should('include', '/itsearviointi')
      cy.contains('h1', 'Itsearviointi').should('be.visible')

      // Fill in itsearviointi fields (at minimum a text area or radio inputs)
      cy.get('textarea').first().type('E2E itsearviointi vastaus.')

      cy.contains('button', 'Tallenna').click()

      // Should redirect back to arvioinnit list
      cy.url().should('include', '/arvioinnit')
    })
  })

  // ── Case 3 – Kouluttaja side ────────────────────────────────────────────────
  //
  // The kouluttaja login requires the verification-token invite flow:
  //   1. After db:seedKouluttaja the kouluttaja's jhi_user exists WITHOUT a hashed SSN.
  //   2. The kouluttaja must first log in via their invite link
  //      (cy.visit(`/login/saml2?RelayState={token}`) + cy.loginWithSuomifi(SSN_KOULUTTAJA))
  //      to bind their SSN to the pre-seeded account.
  //
  // This is intentionally marked pending because it requires:
  //   a) Access to the verification token from the DB, and
  //   b) A second Suomi.fi test SSN ('150685-100N') accepted by the test IdP.
  //
  // TODO: implement after confirming the second test SSN works in the test IdP
  //       and extracting the token via a db task.
  // ─────────────────────────────────────────────────────────────────────────────
  context('Case 3 – Arvioinnin antaminen (kouluttaja)', () => {
    it.skip('kouluttaja sees the open assessment on the dashboard', () => {
      // Pending: verification-token login flow not yet automated.
      cy.loginAsKouluttaja()

      cy.visit('/etusivu')
      cy.contains('Avoimet asiat').should('be.visible')
      cy.contains('E2E testitapahtuma arviointipyyntöä varten.').should('be.visible')
    })

    it.skip('kouluttaja fills in and submits the assessment', () => {
      cy.loginAsKouluttaja()
      cy.visit('/arvioinnit')

      cy.contains('E2E testitapahtuma arviointipyyntöä varten.')
        .closest('tr, .list-item, [class*="row"]')
        .contains('Arvioi')
        .click({ force: true })

      // Select arviointityokalu and fill assessment
      cy.selectFirstMultiselectOption(
        cy.contains('label', 'Arviointityökalu').parent()
      )

      cy.contains('button', 'Lähetä').click()

      cy.url().should('include', '/arvioinnit')
    })
  })
})

