import { E2E_ERIKOISTUVA_EMAIL } from '../../support/commands'

export {}

describe('Suoritemerkintä', () => {
  before(() => {
    Cypress.session.clearAllSavedSessions()
    cy.task('db:cleanupErikoistuva', { email: E2E_ERIKOISTUVA_EMAIL })
    cy.loginAsErikoistuva()
    cy.task('db:seedTyoskentelyjakso', { email: E2E_ERIKOISTUVA_EMAIL })
  })

  it('Erikoistuja lisää suoritemerkinnän ja tarkistaa sen tiedot', () => {
    cy.visit('/suoritemerkinnat')
    cy.contains('h1', 'Suoritemerkinnät').should('be.visible')

    cy.visit('/suoritemerkinnat/uusi')
    cy.contains('h1', 'Lisää suoritemerkintä').should('be.visible')
    cy.get('[role="status"]', { timeout: 10000 }).should('not.exist')

    cy.selectFirstMultiselectOption(cy.contains('label', 'Suorite').parent())

    cy.get('body').then(($body) => {
      if ($body.find('label:contains("Suoriutumisen taso"), label:contains("Etappi")').length > 0) {
        cy.selectFirstMultiselectOption(cy.contains('label', /Suoriutumisen taso|Etappi/).parent())
      }
    })

    cy.selectFirstMultiselectOption(cy.contains('label', 'Vaativuustaso').parent())

    cy.contains('label', 'Suorituspäivä')
      .parent()
      .find('input.date-input, input[type="text"]')
      .first()
      .clear()
      .type('01.02.2025')
      .blur()

    cy.contains('label', 'Lisätiedot')
      .parent()
      .find('textarea')
      .clear()
      .type('E2E lisätiedot suoritemerkinnälle.')

    cy.intercept('POST', '**/erikoistuva-laakari/suoritemerkinnat').as('suoritemerkintaPost')
    cy.contains('button', 'Tallenna').click()

    cy.wait('@suoritemerkintaPost', { timeout: 15000 }).then(({ response }) => {
      expect(response?.statusCode).to.eq(201)
      expect(response?.body).to.have.length(1)
      Cypress.env('suoritemerkintaId', response?.body?.[0]?.id)
    })

    cy.then(() => {
      cy.visit(`/suoritemerkinnat/${Cypress.env('suoritemerkintaId')}`)
    })
    cy.contains('h1', 'Suoritemerkintä').should('be.visible')
    cy.contains('E2E Testisairaala').should('be.visible')
    cy.contains(/1\.2\.2025|01\.02\.2025/).should('be.visible')
    cy.contains('E2E lisätiedot suoritemerkinnälle.').should('be.visible')

    cy.visit('/suoritemerkinnat')
    cy.contains(/1\.2\.2025|01\.02\.2025/).should('be.visible')
  })
})
