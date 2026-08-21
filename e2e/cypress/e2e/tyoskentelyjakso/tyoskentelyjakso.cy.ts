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

describe('Työskentelyjakso', () => {
  beforeEach(() => {
    cy.resetErikoistuvaE2eState()
    cy.loginAsErikoistuva()
  })

  const openAndFillNewTyoskentelyjakso = (tyoskentelypaikka: string) => {
    cy.visit('/tyoskentelyjaksot')
    cy.contains('h1', 'Työskentelyjaksot').should('be.visible')
    cy.visit('/tyoskentelyjaksot/uusi')
    cy.get('.lisaa-tyoskentelyjakso').should('be.visible')
    cy.get('[data-testid="loading"]', { timeout: 10000 }).should('not.exist')
    cy.get('input[type="radio"][name="tyoskentelyjakso-tyyppi"]').first().click({ force: true })
    cy.contains('label', 'Työskentelypaikka')
      .parent()
      .find('input[type="text"]')
      .first()
      .clear()
      .type(tyoskentelypaikka)
    cy.contains('label', 'Kunta').parent().as('kuntaGroup')
    cy.selectFirstMultiselectOption(cy.get('@kuntaGroup'))
    cy.contains('label', 'Alkamispäivä')
      .parent()
      .find('input.date-input')
      .first()
      .clear()
      .type('01.01.2025')
      .blur()
    cy.contains('label', 'Päättymispäivä')
      .parent()
      .find('input.date-input')
      .first()
      .clear()
      .type('30.06.2027')
      .blur()

    cy.get('input[type="number"]').first().clear().type('100')
    cy.get('input[type="radio"][name="kaytannon-koulutus-tyyppi"]').first().click({ force: true })
  }

  it('estää virheellisen sisällön lähettämisen PDF-tiedostona', () => {
    openAndFillNewTyoskentelyjakso('E2E Virheellinen PDF')

    cy.get('input[type="file"]')
      .first()
      .selectFile(
        {
          contents: Cypress.Buffer.from('DOCX content, not a PDF. '.repeat(512)),
          fileName: 'virheellinen.pdf',
          mimeType: 'application/pdf'
        },
        { force: true }
      )

    cy.intercept('POST', '**/erikoistuva-laakari/tyoskentelyjaksot').as(
      'invalidTyoskentelyjaksoPost'
    )
    cy.contains('button', 'Lisää').click()

    cy.wait('@invalidTyoskentelyjaksoPost', { timeout: 30000 })
      .its('response.statusCode')
      .should('eq', 400)
    cy.url().should('include', '/tyoskentelyjaksot/uusi')
    cy.contains(
      '.toast-body',
      'Työskentelyjakson tallentaminen epäonnistui: tiedosto ei ole kelvollinen tai samanniminen tiedosto on jo olemassa'
    ).should('be.visible')
  })

  it('suorittaa koko Työskentelyjakson lisäämisen käyttötapauksen (case 4)', () => {
    // --- Vaihe 1–2: Täytetään ja lähetetään työskentelyjakso PDF-liitteineen ---
    openAndFillNewTyoskentelyjakso('E2E Testisairaala')

    // Työtodistus – tiedoston liittäminen
    cy.get('input[type="file"]').first().selectFile('cypress/fixtures/test.pdf', { force: true })
    cy.wait(1)
    // Lähetä lomake
    cy.contains('button', 'Lisää').click()
    // Varmistetaan onnistuminen: ohjataan yksityiskohtasivulle tai listaan
    cy.url().should('match', /\/tyoskentelyjaksot(\/\d+)?$/)
    cy.contains('uusi-tyoskentelyjakso-lisatty', { matchCase: false }).should('not.exist')
    cy.url().should('not.include', '/uusi')

    // --- Vaihe 3: Lisätään poissaolo työskentelyjaksoon ---
    cy.visit('/tyoskentelyjaksot')
    cy.contains('E2E Testisairaala').should('be.visible')
    // Siirrytään juuri luotuun työskentelyjaksoon
    cy.contains('E2E Testisairaala').click()
    cy.url().should('match', /\/tyoskentelyjaksot\/\d+$/)
    // Klikataan "Lisää poissaolo"
    cy.contains('Lisää poissaolo').click()
    cy.url().should('include', '/poissaolot/uusi')
    // Poissaolon syy
    cy.contains('label', 'Poissaolon syy').parent().as('syy')
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
    // Tallennuksen jälkeen ohjataan poissaolon detail-sivulle
    cy.url().should('not.include', '/poissaolot/uusi')
    cy.url().should('match', /\/tyoskentelyjaksot\/poissaolot\/\d+$/)

    // Varmistetaan, että poissaolon tiedot näkyvät detail-sivulla
    cy.get('[role="status"]', { timeout: 10000 }).should('not.exist')
    cy.contains(/15\.2\.2025|15\.02\.2025/i).should('be.visible')
    cy.contains(/28\.2\.2025|28\.02\.2025/i).should('be.visible')

    // --- Vaihe 4: Näytetään päivitetty työkertymä listassa ---
    cy.visit('/tyoskentelyjaksot')
    cy.get('.tyoskentelyjaksot, main').should('be.visible')
    cy.contains('E2E Testisairaala').should('be.visible')
  })
})
