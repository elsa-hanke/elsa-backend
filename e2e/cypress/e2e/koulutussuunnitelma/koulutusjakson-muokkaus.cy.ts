import { E2E_ERIKOISTUVA_EMAIL } from '../../support/commands'

export {}

// Käyttötapaus 5b.
// Koulutusjakson muokkaaminen
// Käyttäjä: Erikoistuja
// Tavoite: Päivittää olemassa olevan koulutusjakson nimi ja osaamistavoitteet
// Laukaisija: Erikoistuja haluaa tarkentaa koulutusjaksonsa tietoja
// Esiehto: Erikoistuvalla on opinto-oikeus ja vähintään yksi koulutusjakso tallennettuna.
// Käyttötapauksen kulku:
// 1. Erikoistuja luo koulutusjakson (Tallenna koulutusjakso)
// 2. Erikoistuja siirtyy koulutusjakson muokkauslomakkeelle
// 3. Erikoistuja muuttaa nimeä ja osaamistavoitteita
// 4. Erikoistuja tallentaa muutokset
// 5. Muutokset näkyvät koulutusjakson tiedoissa ja koulutussuunnitelmassa

describe('Koulutusjakson muokkaaminen', () => {
  // Esialustetaan tietokanta koko testisarjaa varten
  before(() => {
    Cypress.session.clearAllSavedSessions()
    cy.task('db:cleanupErikoistuva', { email: E2E_ERIKOISTUVA_EMAIL })
    // Kirjautuminen luo erikoistuvan ja opinto-oikeuden (createWithoutOpintotietodata)
    cy.loginAsErikoistuva()
  })

  it('Käyttötapaus 5b: Erikoistuja muokkaa olemassa olevaa koulutusjaksoa', () => {
    // 1. Luodaan koulutusjakso, jota muokataan
    cy.intercept('POST', '**/erikoistuva-laakari/koulutussuunnitelma/koulutusjaksot').as('koulutusjaksoPost')

    cy.visit('/koulutussuunnitelma/koulutusjaksot/uusi')
    cy.contains('h1', 'Lisää koulutusjakso').should('be.visible')
    cy.get('[role="status"]', { timeout: 10000 }).should('not.exist')

    cy.contains('label', 'Koulutusjakson nimi')
      .parent()
      .find('input[type="text"]')
      .first()
      .clear()
      .type('Alkuperäinen E2E Koulutusjakso')

    cy.contains('button', 'Tallenna koulutusjakso').click()
    cy.wait('@koulutusjaksoPost').then(({ response }) => {
      expect(response?.statusCode).to.eq(201)
      expect(response?.body?.id).to.be.a('number')
      expect(response?.body?.nimi).to.eq('Alkuperäinen E2E Koulutusjakso')
      Cypress.env('koulutusjaksoId', response?.body?.id)
    })
    // Luotu – sivu ohjaa koulutusjakson detail-sivulle
    cy.url().should('match', /\/koulutussuunnitelma\/koulutusjaksot\/\d+$/)
    cy.contains('Alkuperäinen E2E Koulutusjakso').should('be.visible')

    // 2. Siirtyminen muokkauslomakkeelle
    cy.then(() => {
      cy.visit(`/koulutussuunnitelma/koulutusjaksot/${Cypress.env('koulutusjaksoId')}/muokkaus`)
    })
    cy.get('[role="status"]', { timeout: 10000 }).should('not.exist')

    // Varmistetaan, että lomake on ladattu ja nimi on esi-täytetty
    cy.contains('label', 'Koulutusjakson nimi')
      .parent()
      .find('input[type="text"]')
      .first()
      .should('not.have.value', '')

    // 3. Nimen ja osaamistavoitteiden muuttaminen
    cy.contains('label', 'Koulutusjakson nimi')
      .parent()
      .find('input[type="text"]')
      .first()
      .clear()
      .type('E2E Muokattu Koulutusjakso')

    cy.contains('label', 'Muut osaamistavoitteet')
      .parent()
      .find('textarea')
      .clear()
      .type('Muokatut osaamistavoitteet e2e-testillä.')

    // 4. Tallennetaan muutokset
    cy.intercept('PUT', '**/erikoistuva-laakari/koulutussuunnitelma/koulutusjaksot/**').as('koulutusjaksoPut')
    cy.contains('button', 'Tallenna koulutusjakso').click()

    cy.wait('@koulutusjaksoPut', { timeout: 15000 }).then(({ response }) => {
      expect(response?.statusCode).to.eq(200)
      expect(response?.body?.nimi).to.eq('E2E Muokattu Koulutusjakso')
      expect(response?.body?.muutOsaamistavoitteet).to.eq('Muokatut osaamistavoitteet e2e-testillä.')
    })

    // 5. Muutokset näkyvät koulutusjakson tiedoissa
    cy.url().should('match', /\/koulutussuunnitelma\/koulutusjaksot\/\d+$/)
    cy.contains('E2E Muokattu Koulutusjakso').should('be.visible')
    cy.contains('Muokatut osaamistavoitteet e2e-testillä.').should('be.visible')

    // Vanha nimi ei enää näy
    cy.contains('Alkuperäinen E2E Koulutusjakso').should('not.exist')

    // Muokattu koulutusjakso näkyy koulutussuunnitelmassa
    cy.visit('/koulutussuunnitelma')
    cy.contains('E2E Muokattu Koulutusjakso').should('be.visible')
  })
})

