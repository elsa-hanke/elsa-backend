import {
  E2E_ERIKOISTUVA_EMAIL,
  SSN_ERIKOISTUVA,
  SSN_KOULUTTAJA,
  SSN_VASTUUHENKILO,
  SSN_VIRKAILIJA,
} from './credentials'

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
      loginWithSuomifi(ssn?: string, email?: string, token?: string): void

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
      loginAsKouluttaja(token?: string): void

      loginAsVastuuhenkilo(token?: string): void

      loginAsVirkailija(token?: string): void
    }
  }
}

// ── loginWithSuomifi ─────────────────────────────────────────────────────────
Cypress.Commands.add('loginWithSuomifi', (ssn = SSN_ERIKOISTUVA, email?: string, token?: string) => {
  cy.visit(token ? `/kirjautuminen?token=${token}` : '/kirjautuminen')
  cy.contains('Kirjaudu sisään (Suomi.fi)').click()

  cy.origin('https://testi.apro.tunnistus.fi', () => {
    cy.get('a#fakevetuma2').click()
  })

  cy.origin('https://saml-test-idp.apro.tunnistus.fi', { args: { ssn } }, ({ ssn }) => {
    cy.get('#hetu_input').clear().type(ssn)
    cy.get('#tunnistaudu').click()
  })

  cy.origin('https://testi.apro.tunnistus.fi', () => {

    cy.get('body').then(($body) => {
      const exists = $body.find('[name="_eventId_proceed"]').length > 0
      if (exists) {
        cy.get('[name="_eventId_proceed"]').click()
      }
      else {
        cy.get('#continue-button').click()
      }
    })
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
      cy.location('origin', { timeout: 60000 }).should(
        'eq',
        new URL(Cypress.config('baseUrl') as string).origin
      )
      cy.location('pathname').should('not.eq', '/kirjautuminen')
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
Cypress.Commands.add('loginAsKouluttaja', (token?: string) => {
  cy.session(
    ['kouluttaja', token ?? 'linked'],
    () => {
      // The kouluttaja account must have been pre-seeded via db:seedKouluttaja and
      // linked to this SSN through the verification-token invite flow beforehand.
      cy.loginWithSuomifi(SSN_KOULUTTAJA, undefined, token)
      cy.location('origin', { timeout: 60000 }).should(
        'eq',
        new URL(Cypress.config('baseUrl') as string).origin
      )
      cy.location('pathname').should('not.eq', '/kirjautuminen')
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

// ── loginAsVastuuhenkilo ─────────────────────────────────────────────────────
Cypress.Commands.add('loginAsVastuuhenkilo', (token?: string) => {
  cy.session(
    ['vastuuhenkilo', token ?? 'linked'],
    () => {
      cy.loginWithSuomifi(SSN_VASTUUHENKILO, undefined, token)
      cy.location('origin', { timeout: 60000 }).should(
        'eq',
        new URL(Cypress.config('baseUrl') as string).origin
      )
      cy.location('pathname').should('not.eq', '/kirjautuminen')
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

// ── loginAsVirkailija ────────────────────────────────────────────────────────
Cypress.Commands.add('loginAsVirkailija', (token?: string) => {
  cy.session(
    ['virkailija', token ?? 'linked'],
    () => {
      cy.loginWithSuomifi(SSN_VIRKAILIJA, undefined, token)
      cy.location('origin', { timeout: 60000 }).should(
        'eq',
        new URL(Cypress.config('baseUrl') as string).origin
      )
      cy.location('pathname').should('not.eq', '/kirjautuminen')
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

export {}
