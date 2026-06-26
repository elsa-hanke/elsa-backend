declare global {
  // eslint-disable-next-line @typescript-eslint/no-namespace
  namespace Cypress {
    interface Chainable {
      /**
       * Opens a Vue Multiselect dropdown and picks the first non-group,
       * non-disabled option.
       * @param container - Cypress chainable pointing to the multiselect wrapper element.
       */
      luoTyoskentelyjakso(token?: string): void
      pyydaaArviointi(token?: string): void
    }
  }
}

// ── selectFirstMultiselectOption ─────────────────────────────────────────────
// Avaa multiselect-pudotusvalikko ja valitsee ensimmäisen ei-ryhmäotsikko-vaihtoehdon.
// Käyttää `.multiselect--active`-skoopausta välttääkseen Cypressin ketjutusongelman,
// joka aiheuttaa haun väärästä kontekstista. Toimii sekä ryhmitetyillä että
// ryhmittämättömillä monivalintavalikoilla.

Cypress.Commands.add("luoTyoskentelyjakso", (token?: string) => {
  cy.visit('/tyoskentelyjaksot')
  cy.contains('h1', 'Työskentelyjaksot').should('be.visible')
  cy.visit('/tyoskentelyjaksot/uusi')
  // Odotetaan, että lomake latautuu (varmistetaan että työskentelypaikan nimi -kenttä on näkyvissä)
  cy.get('.lisaa-tyoskentelyjakso').should('be.visible')
  cy.get('[data-testid="loading"]', { timeout: 10000 }).should('not.exist')
  // Tyyppi – valitaan ensimmäinen saatavilla oleva radiovaihtoehto
  cy.get('input[type="radio"][name="tyoskentelyjakso-tyyppi"]')
    .first()
    .click({ force: true })
  // Työskentelypaikan nimi
  cy.contains('label', 'Työskentelypaikka')
    .parent()
    .find('input[type="text"]')
    .first()
    .clear()
    .type('E2E Testisairaala')
  // Kunta – valitaan ensimmäinen monivalintavaihtoehto
  cy.contains('label', 'Kunta')
    .parent()
    .as('kuntaGroup')
  cy.selectFirstMultiselectOption(cy.get('@kuntaGroup'))
  // Alkamispäivä
  cy.contains('label', 'Alkamispäivä')
    .parent()
    .find('input.date-input')
    .first()
    .clear()
    .type('01.01.2025')
    .blur()
  // Päättymispäivä
  cy.contains('label', 'Päättymispäivä')
    .parent()
    .find('input.date-input')
    .first()
    .clear()
    .type('30.06.2027')
    .blur()

  // Työaika (osaaikaprosentti) – pakollinen kenttä (50–100%)
  cy.get('input[type="number"]').first().clear().type('100')

  // Käytännön koulutus – pakollinen radiovaihtoehto
  cy.get('input[type="radio"][name="kaytannon-koulutus-tyyppi"]').first().click({ force: true })

  // Työtodistus – tiedoston liittäminen
  cy.get('input[type="file"]').first().selectFile(
    'cypress/fixtures/test.pdf',
    { force: true }
  )
  cy.wait(1)
  // Lähetä lomake
  cy.contains('button', 'Lisää').click()
  // Varmistetaan onnistuminen: ohjataan yksityiskohtasivulle tai listaan
  cy.url().should('match', /\/tyoskentelyjaksot(\/\d+)?$/)
  cy.contains('uusi-tyoskentelyjakso-lisatty', { matchCase: false }).should('not.exist')
  cy.url().should('not.include', '/uusi')

})

Cypress.Commands.add("pyydaArviointi", (token?: string) => {


})


export {}

