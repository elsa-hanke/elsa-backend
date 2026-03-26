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

  cy.getCookie('JSESSIONID').then(cookie => {
    cy.log(`[SAML] JSESSIONID before login click: ${cookie ? 'present' : 'missing'}`)
  })

  cy.contains('Kirjaudu sisään (Suomi.fi)').click()

  cy.getCookie('JSESSIONID').then(cookie => {
    cy.log(`[SAML] JSESSIONID after authn request init: ${cookie ? 'present' : 'missing'}`)
  })

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

  // After the SAML POST callback the app redirects back to localhost:8080.
  // In CI the full chain (Spring SAML processing → redirect → Vue bootstrap →
  // API calls) can take significantly longer than on a local machine, so we
  // wait explicitly before asserting on page content.
  cy.url({ timeout: 30000 }).should('not.include', 'tunnistus.fi')

  cy.getCookie('JSESSIONID').then(cookie => {
    cy.log(`[SAML] JSESSIONID after callback: ${cookie ? 'present' : 'missing'}`)
  })

  // Debug: capture exactly what page the browser landed on after SAML
  cy.url().then(url => cy.log(`[SAML] landed on: ${url}`))
  cy.screenshot('after-saml-callback')
  cy.document().then(doc => cy.log(`[SAML] page title: ${doc.title}`))

  if (email !== undefined) {
    cy.get('form .form-control').eq(0).clear().type(email)  // Sähköpostiosoite
    cy.get('form .form-control').eq(1).clear().type(email)  // Vahvista sähköpostiosoite

    cy.contains('Aloita palvelun käyttö').click()
  }
})

export {}
