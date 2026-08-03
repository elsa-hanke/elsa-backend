import { E2E_ERIKOISTUVA_EMAIL } from '../../support/commands'

export {}

describe('Opintosuoritukset', () => {
  before(() => {
    Cypress.session.clearAllSavedSessions()
    cy.task('db:cleanupErikoistuva', { email: E2E_ERIKOISTUVA_EMAIL })
    cy.loginAsErikoistuva()
  })

  it('Erikoistuja tarkistaa opintosuoritusten välilehdet', () => {
    cy.intercept('GET', '**/erikoistuva-laakari/opintosuoritukset').as('opintosuorituksetGet')

    cy.visit('/opintosuoritukset')
    cy.wait('@opintosuorituksetGet', { timeout: 15000 }).then(({ response }) => {
      expect(response?.statusCode).to.eq(200)
      expect(response?.body).to.have.property('opintosuoritukset')
      expect(response?.body.opintosuoritukset).to.be.an('array')
    })

    cy.contains('h1', 'Opintosuoritukset').should('be.visible')
    cy.contains('a', 'sähköpostitse').should('have.attr', 'href').and('include', 'laaketieteelliset.fi')
    cy.contains('.nav-link', 'Johtamisopinnot').should('be.visible')
    cy.contains('.nav-link', 'Kuulustelu').should('be.visible')
    cy.contains('.nav-link', 'Muut').should('be.visible')

    cy.contains('.nav-link', 'Johtamisopinnot').click()
    cy.get('.tab-pane.active').should('be.visible')

    cy.contains('.nav-link', 'Kuulustelu').click()
    cy.get('.tab-pane.active').should('be.visible')

    cy.contains('.nav-link', 'Muut').click()
    cy.get('.tab-pane.active').should('be.visible')
  })
})
