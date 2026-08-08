import { E2E_ERIKOISTUVA_EMAIL } from './credentials'

declare global {
  // eslint-disable-next-line @typescript-eslint/no-namespace
  namespace Cypress {
    interface Chainable {
      /**
       * Submits a valid graduation request through the resident UI and returns its id.
       */
      submitValmistumispyyntoViaUi(): Chainable<number>
    }
  }
}

Cypress.Commands.add('submitValmistumispyyntoViaUi', () => {
  const today = new Date()
  const day = String(today.getDate()).padStart(2, '0')
  const month = String(today.getMonth() + 1).padStart(2, '0')
  const todayText = `${day}.${month}.${today.getFullYear()}`

  cy.intercept('POST', '**/erikoistuva-laakari/valmistumispyynto').as(
    'submitValmistumispyynto'
  )
  cy.visit('/valmistumispyynto')
  cy.get('[role="status"]', { timeout: 10000 }).should('not.exist')

  cy.get('input[type="checkbox"]').each(($checkbox) => {
    cy.wrap($checkbox).check({ force: true })
  })
  cy.contains('button', 'Tee valmistumispyyntö').click()

  cy.contains('label', 'Sähköpostiosoite')
    .parent()
    .find('input')
    .clear()
    .type(E2E_ERIKOISTUVA_EMAIL)
  cy.contains('label', 'Matkapuhelinnumero')
    .parent()
    .find('input')
    .clear()
    .type('+358401234567')

  cy.get('body').then(($body) => {
    if ($body.find('label:contains("Valviran laillistamispäivä")').length > 0) {
      cy.contains('label', 'Valviran laillistamispäivä')
        .parent()
        .find('input.date-input, input[type="text"]')
        .first()
        .clear()
        .type(todayText)
        .blur()
    }
    if ($body.find('input[type="file"]').length > 0) {
      cy.get('input[type="file"]')
        .first()
        .selectFile('cypress/fixtures/test.pdf', { force: true })
    }
    if ($body.find('textarea').length > 0) {
      cy.get('textarea').first().clear().type('E2E-selvitys vanhentuneista suorituksista.')
    }
  })

  cy.contains('button', 'Lähetä pyyntö').click()
  cy.get('#confirm-send').find('button').contains('Lähetä pyyntö').click()

  return cy.wait('@submitValmistumispyynto').then(({ response }) => {
    expect(response?.statusCode).to.eq(201)
    expect(response?.body?.id).to.be.a('number')
    return response?.body?.id as number
  })
})

export {}
