import { E2E_ERIKOISTUVA_EMAIL } from '../../support/commands'

export {}

// Käyttötapaus: Teoriakoulutuksen lisääminen
// Käyttäjä: Erikoistuja
// Tavoite: Kirjata ELSA-palveluun teoriakoulutus ja sen tunnit
// Laukaisija: Erikoistuja haluaa dokumentoida osallistumansa teoriakoulutuksen
// Esiehto: Erikoistuvalla on opinto-oikeus opintotietojärjestelmässä.
// Käyttötapauksen kulku:
// 1. Erikoistuja siirtyy Teoriakoulutukset-sivulle
// 2. Erikoistuja avaa uuden teoriakoulutuksen lomakkeen
// 3. Erikoistuja täyttää pakolliset tiedot (nimi, paikka, ajankohta, tuntimäärä)
// 4. Erikoistuja tallentaa teoriakoulutuksen
// 5. Tallennettu teoriakoulutus näkyy listalla

describe('Teoriakoulutus', () => {
  // Esialustetaan tietokanta koko testisarjaa varten
  before(() => {
    Cypress.session.clearAllSavedSessions()
    cy.task('db:cleanupErikoistuva', { email: E2E_ERIKOISTUVA_EMAIL })
    // Kirjautuminen luo erikoistuvan ja opinto-oikeuden (createWithoutOpintotietodata)
    cy.loginAsErikoistuva()
  })

  it('Lisää teoriakoulutuksen ja varmistaa sen näkyvän listalla', () => {
    // 1. Siirtyminen Teoriakoulutukset-listasivulle
    cy.visit('/teoriakoulutukset')
    cy.contains('h1', 'Teoriakoulutukset').should('be.visible')

    // 2. Siirtyminen uuden teoriakoulutuksen lomakkeelle
    cy.visit('/teoriakoulutukset/uusi')
    cy.get('[role="status"]', { timeout: 10000 }).should('not.exist')

    // 3. Lomakkeen täyttäminen

    // Koulutuksen nimi (pakollinen)
    cy.contains('label', 'Koulutuksen nimi')
      .parent()
      .find('input[type="text"]')
      .first()
      .clear()
      .type('E2E Testiteoriakoulutus')

    // Paikka (pakollinen)
    cy.contains('label', 'Paikka')
      .parent()
      .find('input[type="text"]')
      .first()
      .clear()
      .type('E2E Testipaikka')

    // Alkamispäivä
    cy.contains('label', 'Alkamispäivä')
      .parent()
      .find('input.date-input, input[type="text"]')
      .first()
      .clear()
      .type('01.03.2025')
      .blur()

    // Päättymispäivä
    cy.contains('label', 'Päättymispäivä')
      .parent()
      .find('input.date-input, input[type="text"]')
      .first()
      .clear()
      .type('02.03.2025')
      .blur()

    // Erikoistumiseen hyväksyttävä tuntimäärä
    cy.contains('label', 'Erikoistumiseen hyväksyttävä tuntimäärä')
      .parent()
      .find('input')
      .first()
      .clear()
      .type('8')

    // 4. Teoriakoulutuksen tallentaminen
    cy.intercept('POST', '**/erikoistuva-laakari/teoriakoulutukset').as('teoriakoulutusPost')
    cy.contains('button', 'Tallenna teoriakoulutus').click()

    cy.wait('@teoriakoulutusPost', { timeout: 15000 }).then(({ response }) => {
      expect(response?.statusCode).to.eq(201)
      // Endpoint käyttää multipart/form-data – vastaustekstikenttien saatavuus
      // riippuu backendin serialisoinnista; id riittää vahvistamaan tallennuksen
      expect(response?.body?.id).to.be.a('number')
    })

    // Tallennettu – ohjataan onnistumissivulle tai listalle
    cy.url().should('not.include', '/uusi')

    // 5. Tallennettu teoriakoulutus näkyy listalla
    cy.visit('/teoriakoulutukset')
    cy.contains('h1', 'Teoriakoulutukset').should('be.visible')
    cy.contains('E2E Testiteoriakoulutus').should('be.visible')
    cy.contains('E2E Testipaikka').should('be.visible')
  })
})

