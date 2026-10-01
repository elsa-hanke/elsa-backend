export {}

const invalidAttachmentMessage =
  'Uuden teoriakoulutuksen lisääminen epäonnistui: Liitetiedostoa ei voitu käsitellä tai samanniminen tiedosto on jo olemassa. Tarkista tiedosto ja sen nimi. Jos toinen samanniminen tiedosto on jo lisätty ELSA-palveluun, anna tiedostolle toinen nimi, ja lataa se sitten uudelleen. Tarkista, että saat tiedoston aukeamaan normaalisti ennen lataamista ELSA-palveluun. Jos PDF-tiedosto avautuu normaalisti, tallenna se uudelleen PDF-muodossa ja yritä uudelleen.'

function fillRequiredFields(name: string, place: string) {
  cy.contains('label', 'Koulutuksen nimi')
    .parent()
    .find('input[type="text"]')
    .first()
    .clear()
    .type(name)

  cy.contains('label', 'Paikka').parent().find('input[type="text"]').first().clear().type(place)

  cy.contains('label', 'Alkamispäivä')
    .parent()
    .find('input.date-input, input[type="text"]')
    .first()
    .clear()
    .type('01.03.2025')
    .blur()

  cy.contains('label', 'Päättymispäivä')
    .parent()
    .find('input.date-input, input[type="text"]')
    .first()
    .clear()
    .type('02.03.2025')
    .blur()

  cy.contains('label', 'Erikoistumiseen hyväksyttävä tuntimäärä')
    .parent()
    .find('input')
    .first()
    .clear()
    .type('8')
}

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
    cy.resetErikoistuvaE2eState()
  })

  // Cypress tyhjentää selaimen tilan testien välissä, joten palautetaan
  // cy.session-välimuistiin tallennettu kirjautuminen ennen jokaista testiä.
  beforeEach(() => {
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

    fillRequiredFields('E2E Testiteoriakoulutus', 'E2E Testipaikka')

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

  it('säilyttää lomakkeen verkkovirheen jälkeen ja sallii tallennuksen uudelleen', () => {
    const name = `E2E Verkkovirheen jälkeen ${Date.now()}`
    const place = 'E2E Testipaikka'
    const expectedCourse = {
      koulutuksenNimi: name,
      koulutuksenPaikka: place,
      alkamispaiva: '2025-03-01',
      paattymispaiva: '2025-03-02',
      erikoistumiseenHyvaksyttavaTuntimaara: 8
    }
    let failSave = true
    let savedId: number | undefined

    cy.intercept('POST', '**/erikoistuva-laakari/teoriakoulutukset', (req) => {
      // Keep failing until the user retries: browsers may retry network errors themselves.
      if (failSave) {
        req.alias = 'failedTeoriakoulutusSave'
        req.destroy()
      } else {
        req.alias = 'retriedTeoriakoulutusSave'
        req.continue()
      }
    })

    cy.visit('/teoriakoulutukset/uusi')
    cy.get('[role="status"]', { timeout: 10000 }).should('not.exist')
    fillRequiredFields(name, place)
    cy.contains('button', 'Tallenna teoriakoulutus').click()

    cy.wait('@failedTeoriakoulutusSave', { timeout: 15000 }).should('have.property', 'error')
    cy.get('.toast-body', { timeout: 15000 }).should(
      'contain.text',
      'Uuden teoriakoulutuksen lisääminen epäonnistui'
    )
    cy.location('pathname').should('eq', '/teoriakoulutukset/uusi')
    cy.contains('button', 'Tallenna teoriakoulutus').should('not.be.disabled')
    const retainedFields = [
      ['Koulutuksen nimi', name],
      ['Paikka', place],
      ['Erikoistumiseen hyväksyttävä tuntimäärä', '8']
    ]
    retainedFields.forEach(([label, value]) => {
      cy.contains('label', label).parent().find('input').first().should('have.value', value)
    })

    cy.then(() => {
      failSave = false
    })
    cy.contains('button', 'Tallenna teoriakoulutus').click()
    cy.wait('@retriedTeoriakoulutusSave', { timeout: 15000 }).then(({ response }) => {
      expect(response?.statusCode).to.eq(201)
      expect(response?.body?.id).to.be.a('number')
      expect(response?.body).to.include(expectedCourse)
      savedId = response?.body.id
    })
    cy.location('pathname').should('not.eq', '/teoriakoulutukset/uusi')

    cy.intercept('GET', '**/erikoistuva-laakari/teoriakoulutukset').as('teoriakoulutuksetAfterRetry')
    cy.visit('/teoriakoulutukset')
    cy.wait('@teoriakoulutuksetAfterRetry').then(({ response }) => {
      expect(response?.statusCode).to.eq(200)
      const savedCourses = response?.body.teoriakoulutukset.filter(
        (course: { koulutuksenNimi: string }) => course.koulutuksenNimi === name
      )
      expect(savedCourses).to.have.length(1)
      expect(savedCourses[0]).to.include({ ...expectedCourse, id: savedId })
    })
    cy.contains(name).should('be.visible')
  })

  it('näyttää PDF-validoinnin virhesyyn käyttäjälle', () => {
    cy.visit('/teoriakoulutukset/uusi')
    cy.get('[role="status"]', { timeout: 10000 }).should('not.exist')
    fillRequiredFields('E2E Virheellinen PDF', 'E2E Testipaikka')

    // The component requires PDFs to be at least 10 KB. This payload passes
    // client-side size/type checks but is deliberately not valid PDF data.
    cy.get('input[type="file"]').selectFile(
      {
        contents: Cypress.Buffer.alloc(11 * 1024, 'x'),
        fileName: 'virheellinen-teoriakoulutustodistus.pdf',
        mimeType: 'application/pdf'
      },
      { force: true }
    )

    cy.contains('button', 'Tallenna teoriakoulutus').click()

    cy.get('.toast-body', { timeout: 15000 }).should('contain.text', invalidAttachmentMessage)
    cy.location('pathname').should('eq', '/teoriakoulutukset/uusi')
    cy.contains('button', 'Tallenna teoriakoulutus').should('not.be.disabled')
  })
})
