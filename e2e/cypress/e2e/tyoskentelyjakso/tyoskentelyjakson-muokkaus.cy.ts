import { E2E_ERIKOISTUVA_EMAIL } from '../../support/commands'

export {}

// Käyttötapaus 4b.
// Työskentelyjakson muokkaaminen
// Käyttäjä: Erikoistuja
// Tavoite: Päivittää olemassa olevan työskentelyjakson osaaikaprosentti
// Laukaisija: Erikoistuja tarvitsee korjata työskentelyjakson tietoja
// Esiehto: Erikoistuvalla on vähintään yksi työskentelyjakso tallennettuna.
// Käyttötapauksen kulku:
// 1. Erikoistuja siirtyy työskentelyjaksojen listalle
// 2. Erikoistuja valitsee muokattavan työskentelyjakson
// 3. Erikoistuja siirtyy muokkauslomakkeelle
// 4. Erikoistuja muuttaa osaaikaprosenttia
// 5. Erikoistuja tallentaa muutokset
// 6. Muutokset näkyvät työskentelyjakson tiedoissa

describe('Työskentelyjakson muokkaaminen', () => {
  // Esialustetaan tietokanta koko testisarjaa varten
  before(() => {
    Cypress.session.clearAllSavedSessions()
    cy.task('db:cleanupErikoistuva', { email: E2E_ERIKOISTUVA_EMAIL })
    // Kirjautuminen luo erikoistuvan ja opinto-oikeuden (createWithoutOpintotietodata)
    cy.loginAsErikoistuva()
    // Siemennetään työskentelyjakso suoraan tietokantaan (nopein tapa varmistaa esiehto)
    cy.task('db:seedTyoskentelyjakso', { email: E2E_ERIKOISTUVA_EMAIL })
  })

  it('Käyttötapaus 4b: Erikoistuja muokkaa työskentelyjakson osaaikaprosenttia', () => {
    // 1. Siirrytään työskentelyjaksojen listalle
    cy.visit('/tyoskentelyjaksot')
    cy.contains('h1', 'Työskentelyjaksot').should('be.visible')
    cy.contains('E2E Testisairaala').should('be.visible')

    // 2. Valitaan muokattava työskentelyjakso – klikataan nimeä avatakseen detail-sivun
    cy.contains('E2E Testisairaala').click()
    cy.url().should('match', /\/tyoskentelyjaksot\/\d+$/)

    // Tallennetaan ID myöhempää navigointia varten
    cy.url().then((url) => {
      const id = url.split('/').pop()
      Cypress.env('tyoskentelyjaksoId', id)
    })

    // Tarkistetaan, että detail-sivu latautuu oikein
    cy.get('[role="status"]', { timeout: 10000 }).should('not.exist')
    cy.contains('E2E Testisairaala').should('be.visible')

    // 3. Siirrytään muokkauslomakkeelle
    cy.then(() => {
      cy.visit(`/tyoskentelyjaksot/${Cypress.env('tyoskentelyjaksoId')}/muokkaus`)
    })
    cy.get('[role="status"]', { timeout: 10000 }).should('not.exist')

    // Varmistetaan, että lomake on ladattu ennen muokkaamista
    cy.get('input[type="number"]').first().should('not.have.value', '')

    // 4. Muutetaan osaaikaprosentti sadasta kahdeksaankymmeneen
    cy.get('input[type="number"]').first().clear().type('80')

    // 5. Tallennetaan muutokset
    cy.intercept('PUT', '**/erikoistuva-laakari/tyoskentelyjaksot').as('tyoskentelyjaksosPut')
    cy.contains('button', 'Tallenna').click()

    cy.wait('@tyoskentelyjaksosPut', { timeout: 15000 }).then(({ response }) => {
      expect(response?.statusCode).to.eq(200)
    })

    // 6. Muutokset näkyvät – ohjataan pois lomakkeelta
    cy.url().should('not.include', '/muokkaus')
    cy.url().should('match', /\/tyoskentelyjaksot(\/\d+)?$/)

    // Työskentelyjakso näkyy edelleen listalla muokkauksen jälkeen
    cy.visit('/tyoskentelyjaksot')
    cy.contains('E2E Testisairaala').should('be.visible')
  })
})

