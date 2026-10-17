import { E2E_ERIKOISTUVA_EMAIL, SSN_ERIKOISTUVA } from '../../support/commands/credentials'

const SUOMIFI_ORIGIN = 'https://testi.apro.tunnistus.fi'
const TEST_IDP_ORIGIN = 'https://saml-test-idp.apro.tunnistus.fi'

describe('Suomi.fi single logout', () => {
  before(() => {
    cy.resetErikoistuvaE2eState()
  })

  it('päättää Suomi.fi-istunnon ja pyytää henkilötunnuksen uudelleen seuraavalla kirjautumisella', () => {
    // Keep the complete flow in one test: no cached login or cookie/storage
    // cleanup between logout and the next login may hide a broken SAML logout.
    cy.loginWithSuomifi(SSN_ERIKOISTUVA, E2E_ERIKOISTUVA_EMAIL)
    cy.location('origin', { timeout: 60000 }).should(
      'eq', new URL(Cypress.config('baseUrl') as string).origin
    )
    cy.get('main[role="main"]').should('exist')
    cy.request('/api/kayttaja').its('status').should('eq', 200)

    // Observe real requests; do not replace the backend or IdP responses.
    cy.intercept('GET', '**/api/slo-kaytossa').as('sloEnabled')
    cy.intercept(/\/logout\/saml2\/slo(?:\?|$)/).as('samlLogoutResponse')
    cy.get('.user-dropdown .dropdown-toggle').click()
    cy.get('.user-dropdown-content').contains('Kirjaudu ulos').click()
    cy.wait('@sloEnabled').its('response.body').should('eq', true)

    // Wait for the IdP callback before navigating again. An immediate visit
    // could otherwise interrupt logout while the browser is still at the IdP.
    cy.wait('@samlLogoutResponse', { timeout: 60000 })
    cy.location('origin', { timeout: 60000 }).should(
      'eq', new URL(Cypress.config('baseUrl') as string).origin
    )
    cy.request({ url: '/api/kayttaja', failOnStatusCode: false, followRedirect: false })
      .its('status').should('eq', 401)

    cy.visit('/kirjautuminen')
    cy.contains('Kirjaudu sisään (Suomi.fi)').click()
    cy.origin(SUOMIFI_ORIGIN, () => {
      cy.get('a#fakevetuma2').should('be.visible').click()
    })
    cy.origin(TEST_IDP_ORIGIN, () => {
      // A surviving Suomi.fi session skips this screen and must fail the test.
      cy.get('#hetu_input').should('be.visible').and('have.value', '')
      cy.get('#tunnistaudu').should('be.visible')
    })
  })
})
