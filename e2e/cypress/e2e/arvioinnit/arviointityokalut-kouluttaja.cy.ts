import { KOULUTTAJA_EMAIL } from '../../support/commands/credentials'

export {}

// Käyttötapaus: Kouluttaja selaa arviointityökalut-esittelysivua
// Käyttäjä: Kouluttaja
// Tavoite: Varmistaa, että kouluttajan arviointityökalut-endpointit toimivat
//          ArviointityokalutResource-kantaluokan refaktoroinnin jälkeen.
// Sivu: /arviointityokalut/esittely  (allowedRoles: Kouluttaja, Vastuuhenkilo)
// Testatut endpointit (sivun mount() käynnistää):
//   GET /api/kouluttaja/arviointityokalut
//   GET /api/kouluttaja/arviointityokalut/kategoriat

const KOULUTTAJA_ETUNIMI  = 'Lassekalevi'
const KOULUTTAJA_SUKUNIMI = 'Hummaamistes'

describe('Arviointityökalut – kouluttaja', () => {
  before(() => {
    Cypress.session.clearAllSavedSessions()

    cy.task('db:cleanupErikoistuva', { email: KOULUTTAJA_EMAIL })
    cy.task('db:cleanupKouluttaja', { email: KOULUTTAJA_EMAIL })

    cy.task('db:seedKouluttaja', {
      email: KOULUTTAJA_EMAIL,
      etunimi: KOULUTTAJA_ETUNIMI,
      sukunimi: KOULUTTAJA_SUKUNIMI,
    }).then((result: any) => {
      Cypress.env('kouluttajaToken', result?.token)
    })
  })

  it('Kouluttaja avaa arviointityökalut-esittelysivun ja sivu lataa API-datat onnistuneesti', () => {
    cy.loginAsKouluttaja(Cypress.env('kouluttajaToken'))

    cy.intercept('GET', '**/kouluttaja/arviointityokalut/kategoriat').as('getKategoriat')
    cy.intercept('GET', '**/kouluttaja/arviointityokalut').as('getArviointityokalut')

    cy.visit('/arviointityokalut/esittely')

    cy.wait('@getKategoriat').then(({ response }) => {
      expect(response?.statusCode).to.eq(200)
      expect(response?.body).to.be.an('array')
    })

    cy.wait('@getArviointityokalut').then(({ response }) => {
      expect(response?.statusCode).to.eq(200)
      expect(response?.body).to.be.an('array')
    })

    // Varmistetaan, että sivu on renderöitynyt oikein
    cy.contains('h1', 'Arviointityökalut').should('be.visible')
    cy.get('[role="status"]').should('not.exist')
  })
})
