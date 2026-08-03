import { E2E_ERIKOISTUVA_EMAIL } from '../../support/commands'

export {}

describe('Asiakirjat', () => {
  before(() => {
    Cypress.session.clearAllSavedSessions()
    cy.task('db:cleanupErikoistuva', { email: E2E_ERIKOISTUVA_EMAIL })
    cy.loginAsErikoistuva()
  })

  it('Erikoistuja lisää ja poistaa asiakirjan', () => {
    cy.visit('/asiakirjat')
    cy.contains('h1', 'Asiakirjat').should('be.visible')

    cy.intercept('POST', '**/erikoistuva-laakari/asiakirjat').as('asiakirjaPost')
    cy.get('input[type="file"]').selectFile('cypress/fixtures/test.pdf', { force: true })

    cy.wait('@asiakirjaPost', { timeout: 30000 }).then(({ response }) => {
      expect(response?.statusCode).to.eq(201)
      expect(response?.body?.[0]?.nimi).to.eq('test.pdf')
    })

    cy.contains('.asiakirjat-table', 'test.pdf').should('be.visible')

    cy.intercept('DELETE', '**/erikoistuva-laakari/asiakirjat/*').as('asiakirjaDelete')
    cy.contains('tr', 'test.pdf').find('button').last().click({ force: true })
    cy.get('.modal-content').contains('button', 'Poista').click()

    cy.wait('@asiakirjaDelete', { timeout: 15000 }).its('response.statusCode').should('eq', 204)
    cy.contains('.asiakirjat-table', 'test.pdf').should('not.exist')
  })
})
