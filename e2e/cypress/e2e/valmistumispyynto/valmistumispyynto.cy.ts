import { E2E_ERIKOISTUVA_EMAIL, KOULUTTAJA_EMAIL, VASTUUHENKILO_EMAIL, VIRKAILIJA_EMAIL } from '../../support/commands/credentials'

export {}

// Käyttötapaus 13.
// Valmistumispyynnön tekeminen
// Käyttäjä: Erikoistuja, Vastuuhenkilö, Virkailija
// Tavoite: Erikoistuja tekee valmistumispyynnön, vastuuhenkilö arvioi ja virkailija tarkistaa,
//          jonka jälkeen vastuuhenkilö hyväksyy valmistumisen
// Laukaisija: Erikoistuja on suorittanut erikoistumiskoulutuksen ja pyytää valmistumista
// Esiehto: Erikoistujalla on opinto-oikeus opintotietojärjestelmässä. Koejakso on hyväksytty.
// Käyttötapauksen kulku:
// 1. Erikoistuja siirtyy valmistumispyyntö-sivulle
// 2. Erikoistuja täyttää valmistumispyynnön tiedot ja lähettää pyynnön
// 3. Vastuuhenkilö arvioi erikoistujan osaamisen
// 4. Virkailija tarkistaa tiedot
// 5. Vastuuhenkilö hyväksyy tai hylkää valmistumisen

const KOULUTTAJA_ETUNIMI     = 'E2E'
const KOULUTTAJA_SUKUNIMI    = 'Kouluttaja'
const VASTUUHENKILO_ETUNIMI  = 'Mia'
const VASTUUHENKILO_SUKUNIMI = 'Ålands'
const VIRKAILIJA_ETUNIMI     = 'Daniel'
const VIRKAILIJA_SUKUNIMI    = 'Siekkinen'

describe('Valmistumispyyntö', () => {
  // ── Esialustetaan tietokanta testisarjaa varten ──────────────────────────────
  before(() => {
    Cypress.session.clearAllSavedSessions()

    // Siivotaan vanhat koejakso-lomakkeet ennen erikoistuvan poistoa (FK-turvallisuus)
    cy.task('db:cleanupKoejakso', { erikoistuvaEmail: E2E_ERIKOISTUVA_EMAIL })
    cy.task('db:cleanupErikoistuva', { email: E2E_ERIKOISTUVA_EMAIL })
    cy.task('db:cleanupKouluttaja', { email: KOULUTTAJA_EMAIL })
    cy.task('db:cleanupVastuuhenkilo', { email: VASTUUHENKILO_EMAIL })
    cy.task('db:cleanupVirkailija', { email: VIRKAILIJA_EMAIL })

    // Siemennetään tukikäyttäjät
    cy.task('db:seedKouluttaja', {
      email: KOULUTTAJA_EMAIL,
      etunimi: KOULUTTAJA_ETUNIMI,
      sukunimi: KOULUTTAJA_SUKUNIMI,
    })
    cy.task('db:seedVastuuhenkilo', {
      email: VASTUUHENKILO_EMAIL,
      etunimi: VASTUUHENKILO_ETUNIMI,
      sukunimi: VASTUUHENKILO_SUKUNIMI,
    }).then((result: any) => {
      Cypress.env('vastuuhenkiloToken', result?.token)
    })
    cy.task('db:seedVirkailija', {
      email: VIRKAILIJA_EMAIL,
      etunimi: VIRKAILIJA_ETUNIMI,
      sukunimi: VIRKAILIJA_SUKUNIMI,
    }).then((result: any) => {
      Cypress.env('virkailijaToken', result?.token)
    })

    // Kirjautuminen luo erikoistuvan ja opinto-oikeuden (createWithoutOpintotietodata)
    cy.loginAsErikoistuva()

    // Luodaan kouluttajavaltuutus
    cy.task('db:seedKouluttajavaltuutus', {
      erikoistuvaEmail: E2E_ERIKOISTUVA_EMAIL,
      kouluttajaEmail: KOULUTTAJA_EMAIL,
    })
  })

  // ── Käyttötapaus 13a: Erikoistuja tekee valmistumispyynnön ──────────────────

  it('Käyttötapaus 13: Valmistumispyyntö hyväksytään', () => {
    // Varmistetaan esiehto tietokannassa: loppukeskustelu hyväksytty
    cy.task('db:ensureLoppukeskusteluHyvaksytty', {
      erikoistuvaEmail: E2E_ERIKOISTUVA_EMAIL,
      kouluttajaEmail: KOULUTTAJA_EMAIL,
    })

    const today = new Date()
    const dd = String(today.getDate()).padStart(2, '0')
    const mm = String(today.getMonth() + 1).padStart(2, '0')
    const yyyy = today.getFullYear()
    const todayStr = `${dd}.${mm}.${yyyy}`

    // 1. Erikoistuja siirtyy valmistumispyyntö-sivulle
    cy.intercept('GET', '**/erikoistuva-laakari/valmistumispyynto').as('getValmistumispyynto')
    cy.intercept('GET', '**/erikoistuva-laakari/valmistumispyynto-suoritusten-tila').as('getSuoritustenTila')
    cy.intercept('POST', '**/erikoistuva-laakari/valmistumispyynto').as('postValmistumispyynto')

    cy.visit('/valmistumispyynto')
    cy.wait('@getValmistumispyynto').its('response.statusCode').should('eq', 200)
    cy.get('[role="status"]', { timeout: 10000 }).should('not.exist')
    cy.wait('@getSuoritustenTila').its('response.statusCode').should('eq', 200)

    // 2. Erikoistuja vahvistaa esitietovaatimukset ja avaa lomakkeen
    cy.get('input[type="checkbox"]').each(($checkbox) => {
      cy.wrap($checkbox).check({ force: true })
    })
    cy.contains('button', 'Tee valmistumispyyntö').click()

    // 3. Täytetään valmistumispyyntö-lomake
    cy.contains('label', 'Sähköpostiosoite')
      .parent()
      .find('input')
      .clear()
      .type(E2E_ERIKOISTUVA_EMAIL)

    cy.contains('label', 'Matkapuhelinnumero')
      .parent()
      .find('input')
      .clear()
      .type('+358401234567')

    cy.get('body').then(($body) => {
      if ($body.find('label:contains("Valviran laillistamispäivä")').length > 0) {
        cy.contains('label', 'Valviran laillistamispäivä')
          .parent()
          .find('input.date-input, input[type="text"]')
          .first()
          .clear()
          .type(todayStr)
          .blur()
      }
    })

    cy.get('body').then(($body) => {
      if ($body.find('input[type="file"]').length > 0) {
        cy.get('input[type="file"]').first().selectFile(
          'cypress/fixtures/test.pdf',
          { force: true }
        )
      }
    })

    cy.get('body').then(($body) => {
      if ($body.find('textarea').length > 0) {
        cy.get('textarea').first().clear().type('E2E-selvitys vanhentuneista suorituksista.')
      }
    })

    // 4. Lähetetään valmistumispyyntö
    cy.contains('button', 'Lähetä pyyntö').click()
    cy.get('#confirm-send').find('button').contains('Lähetä pyyntö').click()

    cy.wait('@postValmistumispyynto').then(({ response }) => {
      expect(response?.statusCode).to.eq(201)
      expect(response?.body?.id).to.be.a('number')
      Cypress.env('valmistumispyyntoId', response?.body?.id)
    })

    cy.contains('Valmistumispyyntö lähetetty', { timeout: 15000 }).should('be.visible')
    cy.contains('Valmistumispyyntö odottaa vastuuhenkilön suorittamaa osaamisten tarkastusta', {
      timeout: 15000,
    }).should('be.visible')

    cy.then(() => {
      const valmistumispyyntoId = Cypress.env('valmistumispyyntoId')

      // 3. Vastuuhenkilö arvioi erikoistujan osaamisen.
      cy.loginAsVastuuhenkilo(Cypress.env('vastuuhenkiloToken'))
      cy.apiRequest({
        method: 'GET',
        url: `/api/vastuuhenkilo/valmistumispyynnon-arviointi/${valmistumispyyntoId}`,
      }).its('status').should('eq', 200)
      cy.apiRequest({
        method: 'PUT',
        url: `/api/vastuuhenkilo/valmistumispyynnon-arviointi/${valmistumispyyntoId}`,
        body: {
          osaaminenRiittavaValmistumiseen: true,
          korjausehdotus: null,
        },
      }).then(({ status, body }) => {
        expect(status).to.eq(200)
        expect(body.tila).to.eq('ODOTTAA_VIRKAILIJAN_TARKASTUSTA')
      })

      // 4. Virkailija tarkistaa valmistumispyynnön tiedot.
      cy.loginAsVirkailija(Cypress.env('virkailijaToken'))
      cy.apiRequest({
        method: 'GET',
        url: `/api/virkailija/valmistumispyynnon-tarkistus/${valmistumispyyntoId}`,
      }).its('status').should('eq', 200)
      cy.apiRequest({
        method: 'PUT',
        url: `/api/virkailija/valmistumispyynnon-tarkistus/${valmistumispyyntoId}`,
        form: true,
        body: {
          id: valmistumispyyntoId,
          yekSuoritettu: false,
          ptlSuoritettu: false,
          aiempiElKoulutusSuoritettu: false,
          ltTutkintoSuoritettu: false,
          terveyskeskustyoTarkistettu: true,
          yliopistosairaalanUlkopuolinenTyoTarkistettu: true,
          yliopistosairaalatyoTarkistettu: true,
          kokonaistyoaikaTarkistettu: true,
          teoriakoulutusTarkistettu: true,
          koejaksoEiVaadittu: false,
          kommentitVirkailijoille: 'E2E virkailijan kommentti.',
          lisatiedotVastuuhenkilolle: 'E2E virkailijan saate vastuuhenkilölle.',
          keskenerainen: false,
          virkailijanYhteenveto: 'E2E virkailijan yhteenveto.',
        },
      }).then(({ status, body }) => {
        expect(status).to.eq(200)
        expect(body.valmistumispyynto.virkailijanKuittausaika).to.not.be.null
      })

      // 5. Vastuuhenkilö hyväksyy valmistumisen.
      cy.loginAsVastuuhenkilo(Cypress.env('vastuuhenkiloToken'))
      cy.apiRequest({
        method: 'GET',
        url: `/api/vastuuhenkilo/valmistumispyynnon-hyvaksynta/${valmistumispyyntoId}`,
      }).then(({ status, body }) => {
        expect(status).to.eq(200)
        expect(body.valmistumispyynto.tila).to.eq('ODOTTAA_VASTUUHENKILON_HYVAKSYNTAA')
      })
      cy.apiRequest({
        method: 'PUT',
        url: `/api/vastuuhenkilo/valmistumispyynnon-hyvaksynta/${valmistumispyyntoId}`,
        body: {},
        timeout: 120000,
      }).then(({ status, body }) => {
        expect(status).to.eq(200)
        expect(body.valmistumispyynto.tila).to.eq('HYVAKSYTTY')
      })
    })
  })
})
