import { VASTUUHENKILO_EMAIL } from '../../support/commands/credentials'

export {}

// Käyttötapaus: Vastuuhenkilö selaa arviointityökalut-esittelysivua
// Käyttäjä: Vastuuhenkilö
// Tavoite: Varmistaa, että vastuuhenkilön arviointityökalut-endpointit toimivat
//          ArviointityokalutResource-kantaluokan refaktoroinnin jälkeen.
// Sivu: /arviointityokalut/esittely  (allowedRoles: Kouluttaja, Vastuuhenkilo)
// Testatut endpointit (sivun mount() käynnistää):
//   GET /api/vastuuhenkilo/arviointityokalut
//   GET /api/vastuuhenkilo/arviointityokalut/kategoriat

const VASTUUHENKILO_ETUNIMI  = 'Mia'
const VASTUUHENKILO_SUKUNIMI = 'Ålands'

describe('Arviointityökalut – vastuuhenkilö', () => {
  before(() => {
    Cypress.session.clearAllSavedSessions()

    cy.task('db:cleanupErikoistuva', { email: VASTUUHENKILO_EMAIL })
    cy.task('db:cleanupVastuuhenkilo', { email: VASTUUHENKILO_EMAIL })

    cy.task('db:seedVastuuhenkilo', {
      email: VASTUUHENKILO_EMAIL,
      etunimi: VASTUUHENKILO_ETUNIMI,
      sukunimi: VASTUUHENKILO_SUKUNIMI,
    }).then((result: any) => {
      Cypress.env('vastuuhenkiloToken', result?.token)
    })
  })

  it('Vastuuhenkilö avaa arviointityökalut-esittelysivun ja sivu lataa API-datat onnistuneesti', () => {
    cy.loginAsVastuuhenkilo(Cypress.env('vastuuhenkiloToken'))

    cy.intercept('GET', '**/vastuuhenkilo/arviointityokalut/kategoriat').as('getKategoriat')
    cy.intercept('GET', '**/vastuuhenkilo/arviointityokalut').as('getArviointityokalut')

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
