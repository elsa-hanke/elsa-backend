import { E2E_ERIKOISTUVA_EMAIL } from '../../support/commands'

export {}

describe('Päivittäinen merkintä', () => {
  before(() => {
    Cypress.session.clearAllSavedSessions()
    cy.task('db:cleanupErikoistuva', { email: E2E_ERIKOISTUVA_EMAIL })
    cy.loginAsErikoistuva()
  })

  it('Erikoistuja lisää päivittäisen merkinnän ja näkee sen listalla', () => {
    cy.visit('/paivittaiset-merkinnat')
    cy.contains('h1', 'Päivittäiset merkinnät').should('be.visible')

    cy.visit('/paivittaiset-merkinnat/uusi')
    cy.contains('h1', 'Lisää merkintä').should('be.visible')
    cy.get('[role="status"]', { timeout: 10000 }).should('not.exist')

    cy.contains('label', 'Päivämäärä')
      .parent()
      .find('input.date-input, input[type="text"]')
      .first()
      .clear()
      .type('15.05.2025')
      .blur()

    cy.get('input[name="paivakirja-merkinta-aihe"]').eq(1).check({ force: true })

    cy.contains('label', 'Oppimistapahtuma')
      .parent()
      .find('input[type="text"]')
      .clear()
      .type('E2E ohjauskeskustelu')

    cy.contains('label', 'Ajatuksia opitusta ja sen soveltamisesta')
      .parent()
      .find('textarea')
      .clear()
      .type('E2E reflektio päivittäisestä merkinnästä.')

    cy.intercept('POST', '**/erikoistuva-laakari/paivakirjamerkinnat').as('paivakirjamerkintaPost')
    cy.contains('button', 'Tallenna merkintä').click()

    cy.wait('@paivakirjamerkintaPost', { timeout: 15000 }).then(({ response }) => {
      expect(response?.statusCode).to.eq(201)
      expect(response?.body?.id).to.be.a('number')
    })

    cy.url().should('match', /\/paivittaiset-merkinnat\/\d+$/)
    cy.contains('h1', 'E2E ohjauskeskustelu').should('be.visible')
    cy.contains('E2E reflektio päivittäisestä merkinnästä.').should('be.visible')

    cy.visit('/paivittaiset-merkinnat')
    cy.contains('E2E ohjauskeskustelu').should('be.visible')
    cy.contains('E2E reflektio päivittäisestä merkinnästä.').should('be.visible')
  })
})
