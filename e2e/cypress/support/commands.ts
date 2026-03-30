// ***********************************************
// This file is where custom Cypress commands live.
// https://on.cypress.io/custom-commands
// ***********************************************

// ── Test credentials ────────────────────────────────────────────────────────
export const E2E_ERIKOISTUVA_EMAIL = 'e2e-erikoistuva@test.elsa'

/** Finnish SSN used in the Suomi.fi fake test IdP for the resident physician. */
const SSN_ERIKOISTUVA = '210281-9988'
/** Finnish SSN used for the trainer (kouluttaja) test user. */
const SSN_KOULUTTAJA  = '150685-100N'

// ── Type declarations ────────────────────────────────────────────────────────
declare global {
  // eslint-disable-next-line @typescript-eslint/no-namespace
  namespace Cypress {
    interface Chainable {
      /**
       * Performs the full Suomi.fi test-IdP login flow.
       *
       * @param ssn   - Finnish personal identity code (SSN). Defaults to the
       *                erikoistuva test SSN.
       * @param email - If provided, fills in the "Aloita palvelun käyttö" form
       *                (shown only on first-ever login for a user with no e-mail).
       */
      loginWithSuomifi(ssn?: string, email?: string): void

      /**
       * Logs in as the resident physician (erikoistuva lääkäri).
       * The session is cached by cy.session() so re-authentication is skipped
       * between tests in the same run.
       */
      loginAsErikoistuva(): void

      /**
       * Logs in as the trainer (kouluttaja).
       * Uses a different SSN from loginAsErikoistuva.
       * Intended for use after the kouluttaja has been seeded via db:seedKouluttaja
       * and their account activated via the verification-token invite flow.
       */
      loginAsKouluttaja(): void

      /**
       * Opens a Vue Multiselect dropdown and picks the first option.
       * @param container - Cypress chainable pointing to the multiselect wrapper element.
       */
      selectFirstMultiselectOption(container: Cypress.Chainable<JQuery>): void
    }
  }
}

// ── loginWithSuomifi ─────────────────────────────────────────────────────────
Cypress.Commands.add('loginWithSuomifi', (ssn = SSN_ERIKOISTUVA, email?: string) => {
  cy.visit('/kirjautuminen')
  cy.contains('Kirjaudu sisään (Suomi.fi)').click()

  cy.origin('https://testi.apro.tunnistus.fi', () => {
    cy.get('a#fakevetuma2').click()
  })

  cy.origin('https://saml-test-idp.apro.tunnistus.fi', { args: { ssn } }, ({ ssn }) => {
    cy.get('#hetu_input').clear().type(ssn)
    cy.get('#tunnistaudu').click()
  })

  cy.origin('https://testi.apro.tunnistus.fi', () => {
    cy.get('#continue-button').click()
  })

  if (email !== undefined) {
    // "Aloita palvelun käyttö" screen — only shown once per user.
    cy.get('form .form-control').eq(0).clear().type(email)
    cy.get('form .form-control').eq(1).clear().type(email)
    cy.contains('Aloita palvelun käyttö').click()
  }
})

// ── loginAsErikoistuva ───────────────────────────────────────────────────────
Cypress.Commands.add('loginAsErikoistuva', () => {
  cy.session(
    'erikoistuva',
    () => {
      cy.loginWithSuomifi(SSN_ERIKOISTUVA, E2E_ERIKOISTUVA_EMAIL)
      cy.url().should('not.include', '/kirjautuminen')
      cy.get('main[role="main"]').should('exist')
    },
    {
      // Re-validate: confirm the session is still alive by checking we land on
      // etusivu after a visit.
      validate() {
        cy.visit('/etusivu')
        cy.url().should('not.include', '/kirjautuminen')
      },
    }
  )
})

// ── loginAsKouluttaja ────────────────────────────────────────────────────────
Cypress.Commands.add('loginAsKouluttaja', () => {
  cy.session(
    'kouluttaja',
    () => {
      // The kouluttaja account must have been pre-seeded via db:seedKouluttaja and
      // linked to this SSN through the verification-token invite flow beforehand.
      cy.loginWithSuomifi(SSN_KOULUTTAJA)
      cy.url().should('not.include', '/kirjautuminen')
      cy.get('main[role="main"]').should('exist')
    },
    {
      validate() {
        cy.visit('/etusivu')
        cy.url().should('not.include', '/kirjautuminen')
      },
    }
  )
})

// ── selectFirstMultiselectOption ─────────────────────────────────────────────
Cypress.Commands.add(
  'selectFirstMultiselectOption',
  (container: Cypress.Chainable<JQuery>) => {
    container.find('.multiselect').click()
    container
      .find('.multiselect__content .multiselect__element')
      .first()
      .find('.multiselect__option')
      .click({ force: true })
  }
)

export {}
