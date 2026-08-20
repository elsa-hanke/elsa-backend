import { E2E_ERIKOISTUVA_EMAIL } from './credentials'

declare global {
  // eslint-disable-next-line @typescript-eslint/no-namespace
  namespace Cypress {
    interface Chainable {
      /**
       * Submits a valid training agreement through the resident UI and returns its id.
       */
      submitKoulutussopimusViaUi(kouluttajaNimi: string): Chainable<number>
    }
  }
}

Cypress.Commands.add('submitKoulutussopimusViaUi', (kouluttajaNimi: string) => {
  const today = new Date()
  const day = String(today.getDate()).padStart(2, '0')
  const month = String(today.getMonth() + 1).padStart(2, '0')
  const todayText = `${day}.${month}.${today.getFullYear()}`

  cy.intercept('POST', '**/erikoistuva-laakari/koejakso/koulutussopimus').as(
    'submitKoulutussopimus'
  )
  cy.visit('/koejakso/koulutussopimus')
  cy.get('[role="status"]', { timeout: 10000 }).should('not.exist')

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
  cy.contains('label', 'Toimipaikan nimi')
    .parent()
    .find('input[type="text"]')
    .first()
    .clear()
    .type('E2E Testisairaala')
  cy.contains('Toimipaikalla on koulutussopimus oman yliopiston kanssa').should('be.visible')
  cy.contains('label', 'Kyllä').click()
  cy.contains('label', 'Koejakson alkamispäivä')
    .parent()
    .find('input.date-input, input[type="text"]')
    .first()
    .clear()
    .type(todayText)
    .blur()
  cy.contains('label', 'Kouluttaja').parent().find('.multiselect').click()
  cy.get('.multiselect--active .multiselect__option')
    .contains(kouluttajaNimi)
    .click({ force: true })

  cy.contains('button', 'Hyväksy ja lähetä').click()
  cy.get('#confirm-send').find('button').contains('Hyväksy ja lähetä').click()

  return cy.wait('@submitKoulutussopimus').then(({ response }) => {
    expect(response?.statusCode).to.eq(201)
    expect(response?.body?.id).to.be.a('number')
    return response?.body?.id as number
  })
})

export {}
