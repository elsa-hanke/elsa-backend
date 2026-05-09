import { E2E_ERIKOISTUVA_EMAIL, KOULUTTAJA_EMAIL, VASTUUHENKILO_EMAIL } from '../../support/commands/credentials'

export {}

// Käyttötapaus 6.
// Koejakson koulutustodistuksen tekeminen, erikoistuja
// Käyttäjä: Erikoistuja
// Tavoite: Lähettää koejakson koulutussopimus
// Laukaisija: Erikoistuja on saanut opinto-oikeuden ja on aloittamassa koejaksoa
// Esiehto: Erikoistuvalla on opinto-oikeus opintotietojärjestelmässä.
//          Erikoistuja on lisännyt kouluttajalle katseluoikeudet.
//
// Käyttötapaus 7.
// Kouluttaja hyväksyy koulutussopimuksen
// Käyttäjä: Kouluttaja
// Suoritetaan API-kutsulla verification-tokenin kautta (valmistumispyynto-patternin mukaan)
//
// Käyttötapaus 8.
// Vastuuhenkilö hyväksyy koulutussopimuksen
// Käyttäjä: Vastuuhenkilö
// Suoritetaan API-kutsulla verification-tokenin kautta

const KOULUTTAJA_ETUNIMI     = 'Lassekalevi'
const KOULUTTAJA_SUKUNIMI    = 'Hummaamistes'
const VASTUUHENKILO_ETUNIMI  = 'Mia'
const VASTUUHENKILO_SUKUNIMI = 'Ålands'

function apiRequest(options: Partial<Cypress.RequestOptions>) {
  return cy.getCookie('XSRF-TOKEN').then((cookie) =>
    cy.request({
      ...options,
      headers: {
        'X-XSRF-TOKEN': decodeURIComponent(cookie?.value ?? ''),
        ...(options.headers ?? {}),
      },
    })
  )
}

describe('Koulutussopimus', () => {
  // ── Esialustetaan tietokanta testisarjaa varten ──────────────────────────────
  before(() => {
    Cypress.session.clearAllSavedSessions()

    // Siivotaan vanhat koejakso-lomakkeet ennen erikoistuvan poistoa (FK-turvallisuus)
    cy.task('db:cleanupKoejakso', { erikoistuvaEmail: E2E_ERIKOISTUVA_EMAIL })
    cy.task('db:cleanupErikoistuva', { email: E2E_ERIKOISTUVA_EMAIL })
    cy.task('db:cleanupKouluttaja', { email: KOULUTTAJA_EMAIL })
    cy.task('db:cleanupVastuuhenkilo', { email: VASTUUHENKILO_EMAIL })

    // Siemennetään tukikäyttäjät ja talletetaan verification-tokenit Cypress.env:iin
    cy.task('db:seedKouluttaja', {
      email: KOULUTTAJA_EMAIL,
      etunimi: KOULUTTAJA_ETUNIMI,
      sukunimi: KOULUTTAJA_SUKUNIMI,
    }).then((result: any) => {
      Cypress.env('kouluttajaToken', result?.token)
    })
    cy.task('db:seedVastuuhenkilo', {
      email: VASTUUHENKILO_EMAIL,
      etunimi: VASTUUHENKILO_ETUNIMI,
      sukunimi: VASTUUHENKILO_SUKUNIMI,
    }).then((result: any) => {
      Cypress.env('vastuuhenkiloToken', result?.token)
    })

    // Kirjautuminen luo erikoistuvan ja opinto-oikeuden (createWithoutOpintotietodata)
    cy.loginAsErikoistuva()

    // Luodaan kouluttajavaltuutus kirjautumisen jälkeen (opinto-oikeus täytyy olla olemassa)
    cy.task('db:seedKouluttajavaltuutus', {
      erikoistuvaEmail: E2E_ERIKOISTUVA_EMAIL,
      kouluttajaEmail: KOULUTTAJA_EMAIL,
    })
  })

  // ── Käyttötapaukset 6, 7, 8: koko koulutussopimusprosessi ───────────────────

  it('Käyttötapaus 6: Erikoistuja täyttää ja lähettää koulutussopimuksen', () => {
    // Siivotaan mahdollinen edellinen koulutussopimus jotta testi on idempotentti
    cy.task('db:cleanupKoejakso', { erikoistuvaEmail: E2E_ERIKOISTUVA_EMAIL })

    const today = new Date()
    const dd = String(today.getDate()).padStart(2, '0')
    const mm = String(today.getMonth() + 1).padStart(2, '0')
    const yyyy = today.getFullYear()
    const todayStr = `${dd}.${mm}.${yyyy}`

    // ── Käyttötapaus 6: Erikoistuja täyttää ja lähettää koulutussopimuksen ────

    // 1. Erikoistuja siirtyy koejakso-sivulle
    cy.visit('/koejakso')
    cy.contains('h1', 'Koejakso').should('be.visible')

    // 2. Erikoistuja avaa koulutussopimus-lomakkeen
    cy.visit('/koejakso/koulutussopimus')
    cy.get('[role="status"]', { timeout: 10000 }).should('not.exist')

    // 3. Erikoistuja täyttää lomakkeen – sähköposti
    cy.contains('label', 'Sähköpostiosoite')
      .parent()
      .find('input')
      .clear()
      .type(E2E_ERIKOISTUVA_EMAIL)

    // Matkapuhelinnumero
    cy.contains('label', 'Matkapuhelinnumero')
      .parent()
      .find('input')
      .clear()
      .type('+358401234567')

    // Koulutuspaikan nimi
    cy.contains('label', 'Toimipaikan nimi')
      .parent()
      .find('input[type="text"]')
      .first()
      .clear()
      .type('E2E Testisairaala')

    // Valitaan "Kyllä" koulutussopimus-radioryhmästä
    cy.contains('Toimipaikalla on koulutussopimus oman yliopiston kanssa')
      .should('be.visible')
    cy.contains('label', 'Kyllä').click()

    // Koejakson alkamispäivä
    cy.contains('label', 'Koejakson alkamispäivä')
      .parent()
      .find('input.date-input, input[type="text"]')
      .first()
      .clear()
      .type(todayStr)
      .blur()

    // 5. Kouluttajan valinta – valitaan siemennetty kouluttaja
    cy.contains('label', 'Kouluttaja')
      .parent()
      .find('.multiselect')
      .click()
    cy.get('.multiselect--active .multiselect__option')
      .contains(`${KOULUTTAJA_ETUNIMI} ${KOULUTTAJA_SUKUNIMI}`)
      .click({ force: true })

    // 6. Erikoistuja lähettää lomakkeen
    cy.intercept('POST', '**/erikoistuva-laakari/koejakso/koulutussopimus').as('koulutussopimusPost')
    cy.intercept('GET', '**/erikoistuva-laakari/koejakso').as('koejaksoAfterSubmit')

    cy.contains('button', 'Hyväksy ja lähetä').click()
    cy.get('#confirm-send').find('button').contains('Hyväksy ja lähetä').click()

    cy.wait('@koulutussopimusPost').then(({ response }) => {
      expect(response?.statusCode).to.eq(201)
      expect(response?.body?.id).to.be.a('number')
      Cypress.env('koulutussopimusId', response?.body?.id)
    })
    cy.wait('@koejaksoAfterSubmit')

    // Vahvistetaan onnistuminen – lomake siirtyy odottamaan hyväksyntöjä readonly-tilaan
    cy.url().should('include', '/koejakso/koulutussopimus')
    cy.contains(
      'Koejakson koulutussopimus on lähetetty kouluttajan ja vastuuhenkilön hyväksyttäväksi.',
      { timeout: 15000 }
    ).should('be.visible')
    cy.contains('E2E Testisairaala').should('be.visible')
    cy.contains(`${KOULUTTAJA_ETUNIMI} ${KOULUTTAJA_SUKUNIMI}`).should('be.visible')

    cy.then(() => {
      const sopimusId = Cypress.env('koulutussopimusId')

      // ── Käyttötapaus 7: Kouluttaja hyväksyy koulutussopimuksen ──────────────
      // 1. Kouluttaja kirjautuu ELSA-palveluun verification-tokenin kautta
      cy.loginAsKouluttaja(Cypress.env('kouluttajaToken'))

      // 2. Kouluttaja hakee koulutussopimuksen tiedot
      apiRequest({
        method: 'GET',
        url: `/api/kouluttaja/koejakso/koulutussopimus/${sopimusId}`,
      }).then(({ status, body }) => {
        expect(status).to.eq(200)

        // 3–4. Kouluttaja täyttää koulutuspaikan tiedot ja lähettää hyväksynnän
        // (korjausehdotus=null → hyväksyntä, ei palautus korjattavaksi)
        // Huom: sahkoposti täytyy sisällyttää DTO:hon, muuten palvelu nollaa käyttäjän sähköpostin
        const kouluttajat = (body.kouluttajat ?? []).map((k: any) => ({
          ...k,
          toimipaikka: 'E2E Testisairaala',
          lahiosoite: 'Testikatu 1',
          postitoimipaikka: '00100 Helsinki',
          sahkoposti: KOULUTTAJA_EMAIL,
        }))

        apiRequest({
          method: 'PUT',
          url: '/api/kouluttaja/koejakso/koulutussopimus',
          body: {
            ...body,
            kouluttajat,
            korjausehdotus: null,
          },
        }).then(({ status, body }) => {
          expect(status).to.eq(200)
          expect(body.kouluttajat?.some((k: any) => k.sopimusHyvaksytty === true)).to.eq(true)
        })
      })

      // ── Käyttötapaus 8: Vastuuhenkilö hyväksyy koulutussopimuksen ───────────
      // 1. Vastuuhenkilö kirjautuu ELSA-palveluun verification-tokenin kautta
      cy.loginAsVastuuhenkilo(Cypress.env('vastuuhenkiloToken'))

      // 2. Vastuuhenkilö hakee koulutussopimuksen tiedot
      apiRequest({
        method: 'GET',
        url: `/api/vastuuhenkilo/koejakso/koulutussopimus/${sopimusId}`,
      }).then(({ status, body }) => {
        expect(status).to.eq(200)

        // 3–4. Vastuuhenkilö tarkistaa tiedot ja hyväksyy
        // (korjausehdotus=null → hyväksyntä, ei palautus)
        // Huom: vastuuhenkilo.sahkoposti täytyy sisällyttää DTO:hon, muuten palvelu nollaa käyttäjän sähköpostin
        apiRequest({
          method: 'PUT',
          url: '/api/vastuuhenkilo/koejakso/koulutussopimus',
          body: {
            ...body,
            korjausehdotus: null,
            vastuuhenkilo: {
              ...(body.vastuuhenkilo ?? {}),
              sahkoposti: VASTUUHENKILO_EMAIL,
            },
          },
        }).then(({ status, body }) => {
          expect(status).to.eq(200)
          expect(body.vastuuhenkilo?.sopimusHyvaksytty).to.eq(true)
        })
      })
    })
  })
})
