import {E2E_ERIKOISTUVA_EMAIL} from "../../support/commands";


// Käyttötapaus 5.
// Koulutusjakson lisääminen koulutussuunnitelmaan
// Käyttäjä: Erikoistuja
// Tavoite: Kirjata ELSA-palveluun koulutusjakso ja sen tavoitteet
// Laukaisija: Erikoistuja suunnittelee omia opintojaan
// Esiehto: Erikoistuvalla on opinto-oikeus opintotietojärjestelmässä
// Poikkeukset: -
// Käyttötapauksen kulku:
// 1. Erikoistuja kirjautuu järjestelmään Suomi.fi-tunnistautumisella
// 2. Erikoistuja siirtyy Koulutussuunnitelma-välilehdelle
// 3. Erikoistuja lisää koulutusjakson
// 4. Erikoistuja täyttää koulutusjakson tiedot ja oppimistavoitteet
// 5. Erikoistuja tallentaa koulutusjakson

describe('Koulutussuunnitelma', () => {
  before(() => {
    Cypress.session.clearAllSavedSessions()
    cy.task('db:cleanupErikoistuva', { email: E2E_ERIKOISTUVA_EMAIL })
    cy.loginAsErikoistuva()
  })

  it('Käyttötapaus 5: Koulutusjakson lisääminen koulutussuunnitelmaan', () => {
    // 2. Siirtyminen Koulutussuunnitelma-välilehdelle
    cy.visit('/koulutussuunnitelma')
    cy.contains('h1', 'Koulutussuunnitelma').should('be.visible')

    // 3. Uuden koulutusjakson lisääminen
    cy.visit('/koulutussuunnitelma/koulutusjaksot/uusi')
    cy.contains('h1', 'Lisää koulutusjakso').should('be.visible')

    // 4. Koulutusjakson tietojen ja oppimistavoitteiden täyttäminen
    // Odotetaan, että lomake latautuu (spinner häviää)
    cy.get('.lisaa-koulutusjakso').should('be.visible')
    cy.get('[role="status"]', { timeout: 10000 }).should('not.exist')

    // Koulutusjakson nimi (pakollinen)
    cy.contains('label', 'Koulutusjakson nimi')
      .parent()
      .find('input[type="text"]')
      .first()
      .clear()
      .type('E2E Testi Koulutusjakso')

    // Muut osaamistavoitteet (valinnainen)
    cy.contains('label', 'Muut osaamistavoitteet')
      .parent()
      .find('textarea')
      .type('Testataan e2e-automaatiolla.')

    // 5. Koulutusjakson tallentaminen
    cy.intercept('POST', '**/erikoistuva-laakari/koulutussuunnitelma/koulutusjaksot').as(
      'koulutusjaksoPost'
    )
    cy.contains('button', 'Tallenna koulutusjakso').click()
    cy.wait('@koulutusjaksoPost').then(({ response }) => {
      expect(response?.statusCode).to.eq(201)
      expect(response?.body?.id).to.be.a('number')
      expect(response?.body?.nimi).to.eq('E2E Testi Koulutusjakso')
      expect(response?.body?.muutOsaamistavoitteet).to.eq('Testataan e2e-automaatiolla.')
    })

    // Tallennuksen jälkeen ohjaudutaan koulutusjakson sivulle
    cy.url().should('match', /\/koulutussuunnitelma\/koulutusjaksot\/\d+$/)
    cy.contains('E2E Testi Koulutusjakso').should('be.visible')
    cy.contains('Testataan e2e-automaatiolla.').should('be.visible')

    // Uusi koulutusjakso näkyy koulutussuunnitelmassa
    cy.visit('/koulutussuunnitelma')
    cy.contains('E2E Testi Koulutusjakso').should('be.visible')
  })
})
