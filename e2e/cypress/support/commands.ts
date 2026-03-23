// ***********************************************
// This file is where custom Cypress commands live.
// https://on.cypress.io/custom-commands
// ***********************************************

declare global {
  // eslint-disable-next-line @typescript-eslint/no-namespace
  namespace Cypress {
    interface Chainable {
      loginWithSuomifi(email?: string): void
    }
  }
}

Cypress.Commands.add('loginWithSuomifi', (email?: string) => {
  cy.visit('/kirjautuminen')

  cy.contains('Kirjaudu sisään (Suomi.fi)').click()

  cy.origin('https://testi.apro.tunnistus.fi', () => {
    cy.get('a#fakevetuma2').click()
  })

  cy.origin('https://saml-test-idp.apro.tunnistus.fi', () => {
    cy.get('#hetu_input').clear().type('210281-9988')
    cy.get('#tunnistaudu').click()
  })

  cy.origin('https://testi.apro.tunnistus.fi', () => {
    cy.get('#continue-button').click()
  })

  if (email !== undefined) {
    cy.get('form .form-control').eq(0).clear().type(email)  // Sähköpostiosoite
    cy.get('form .form-control').eq(1).clear().type(email)  // Vahvista sähköpostiosoite

    cy.contains('Aloita palvelun käyttö').click()
  }
})

export {}

