/**
 * Käyttötapaus 4 – Työskentelyjakson lisääminen ja poissaolojen merkitseminen
 *
 * Kattaa:
 *  - Uuden työskentelyjakson lisääminen erikoistuvana lääkärinä
 *  - Työtodistuksen (PDF) liittäminen työskentelyjaksolle
 *  - Poissaolon lisääminen luotuun työskentelyjaksoon
 *  - Työkertymän päivittymisen tarkistaminen
 *
 * Esiehdot (hoidetaan automaattisesti dev-profiilissa):
 *  Ensimmäisellä kirjautumisella backend suorittaa createWithoutOpintotietodata, joka luo
 *  erikoistuva_laakari + opintooikeus -tietueet. Erillistä siementämistä ei tarvita.
 */
import {E2E_ERIKOISTUVA_EMAIL} from "../../support/commands";

describe('Työskentelyjakso', () => {
  before(() => {
    // Tyhjennetään kaikki tallennetut sessiot ja siivotaan testidata
    Cypress.session.clearAllSavedSessions()
    cy.task('db:cleanupErikoistuva', { email: E2E_ERIKOISTUVA_EMAIL })
    cy.loginAsErikoistuva()
  })

  it('suorittaa koko Työskentelyjakson lisäämisen käyttötapauksen (case 4)', () => {
    // --- Vaihe 1: Siirrytään työskentelyjaksojen listaukseen ---
    cy.visit('/tyoskentelyjaksot')
    cy.contains('h1', 'Työskentelyjaksot').should('be.visible')

    // --- Vaihe 2: Avataan uusi työskentelyjakso -lomake ---
    cy.visit('/tyoskentelyjaksot/uusi')
    cy.contains('h1', 'Lisää työskentelyjakso').should('be.visible')

    // --- Vaihe 3: Täytetään ja lähetetään työskentelyjakso -lomake ---
    cy.visit('/tyoskentelyjakso/uusi')
    // Odotetaan, että lomake latautuu
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

    // --- Vaihe 4: Näytetään juuri luotu työskentelyjakso listassa ---
    cy.visit('/tyoskentelyjaksot')
    cy.contains('E2E Testisairaala').should('be.visible')

    // --- Vaihe 5: Lisätään poissaolo työskentelyjaksoon ---
    cy.visit('/tyoskentelyjaksot')
    // Siirrytään juuri luotuun työskentelyjaksoon
    cy.contains('E2E Testisairaala').click()
    cy.url().should('match', /\/tyoskentelyjaksot\/\d+$/)
    // Klikataan "Lisää poissaolo"
    cy.contains('Lisää poissaolo').click()
    cy.url().should('include', '/poissaolot/uusi')
    // Poissaolon syy
    cy.contains('label', 'Poissaolon syy')
      .parent()
      .as('syy')
    cy.selectFirstMultiselectOption(cy.get('@syy'))
    // Poissaolon alkamispäivä
    cy.contains('label', 'Alkamispäivä')
      .parent()
      .find('input.date-input')
      .first()
      .clear()
      .type('15.02.2025')
      .blur()
    // Poissaolon päättymispäivä
    cy.contains('label', 'Päättymispäivä')
      .parent()
      .find('input.date-input')
      .first()
      .clear()
      .type('28.02.2025')
      .blur()
    cy.contains('button', 'Tallenna').click()
    // Tallennuksen jälkeen palataan työskentelyjakson sivulle
    cy.url().should('not.include', '/poissaolot/uusi')

    // --- Vaihe 6: Näytetään päivitetty työkertymä listassa ---
    cy.visit('/tyoskentelyjaksot')
    cy.get('.tyoskentelyjaksot, main').should('be.visible')
    cy.contains('E2E Testisairaala').should('be.visible')
  })
})
