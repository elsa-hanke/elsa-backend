import {
  E2E_ERIKOISTUVA_EMAIL,
  KOULUTTAJA_EMAIL,
  VASTUUHENKILO_EMAIL,
  VIRKAILIJA_EMAIL,
} from '../../support/commands/credentials'

export {}

// Käyttötapaus 12.
// Erikoisalan vastuuhenkilön arvion lähettäminen ja sen hyväksyminen
// opintohallinnon virkailijan ja erikoisalan vastuuhenkilön toimesta.
// Käyttäjä: Erikoistuja, virkailija, vastuuhenkilö
// Tavoite: Hyväksyttää vastuuhenkilön arvio koejaksosta
// Laukaisija: Erikoistuja on saanut koejakson päätökseen ja pyytää siitä arviota
// Esiehto: Muut koejakson lomakkeet on hyväksytty ja koejaksoon on liitetty työtodistuksellinen työskentelyjakso.

const KOULUTTAJA_ETUNIMI = 'E2E'
const KOULUTTAJA_SUKUNIMI = 'Kouluttaja'
const VASTUUHENKILO_ETUNIMI = 'Mia'
const VASTUUHENKILO_SUKUNIMI = 'Ålands'
const VIRKAILIJA_ETUNIMI = 'Daniel'
const VIRKAILIJA_SUKUNIMI = 'Siekkinen'

describe('Koejakson vastuuhenkilön arvio', () => {
  before(() => {
    Cypress.session.clearAllSavedSessions()

    cy.task('db:cleanupKoejakso', { erikoistuvaEmail: E2E_ERIKOISTUVA_EMAIL })
    cy.task('db:cleanupErikoistuva', { email: E2E_ERIKOISTUVA_EMAIL })
    cy.task('db:cleanupKouluttaja', { email: KOULUTTAJA_EMAIL })
    cy.task('db:cleanupVastuuhenkilo', { email: VASTUUHENKILO_EMAIL })
    cy.task('db:cleanupVirkailija', { email: VIRKAILIJA_EMAIL })

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

    cy.loginAsErikoistuva()
    cy.task('db:seedKouluttajavaltuutus', {
      erikoistuvaEmail: E2E_ERIKOISTUVA_EMAIL,
      kouluttajaEmail: KOULUTTAJA_EMAIL,
    })
  })

  it('Käyttötapaus 12: Erikoistuja pyytää ja vastuuhenkilö hyväksyy koejakson arvion', () => {
    // Finish: Esiehdot
    cy.task('db:cleanupKoejakso', { erikoistuvaEmail: E2E_ERIKOISTUVA_EMAIL })
    cy.task('db:ensureLoppukeskusteluHyvaksytty', {
      erikoistuvaEmail: E2E_ERIKOISTUVA_EMAIL,
      kouluttajaEmail: KOULUTTAJA_EMAIL,
    })
    cy.task('db:ensureKoejaksoTyoskentelyjakso', { email: E2E_ERIKOISTUVA_EMAIL })

    // Finish: Erikoistuja avaa vastuuhenkilön arvion
    cy.intercept('GET', '**/erikoistuva-laakari/koejakso').as('getKoejakso')
    cy.intercept('GET', '**/erikoistuva-laakari/vastuuhenkilonarvio-lomake').as(
      'getVastuuhenkilonArvioLomake'
    )
    cy.intercept('POST', '**/erikoistuva-laakari/koejakso/vastuuhenkilonarvio').as(
      'postVastuuhenkilonArvio'
    )

    cy.visit('/koejakso')
    cy.contains('h1', 'Koejakso').should('be.visible')
    cy.contains('Erikoisalan vastuuhenkilön arvio koejaksosta').should('be.visible')

    cy.visit('/koejakso/vastuuhenkilon-arvio')
    cy.wait('@getKoejakso').its('response.statusCode').should('eq', 200)
    cy.wait('@getVastuuhenkilonArvioLomake').then(({ response }) => {
      expect(response?.statusCode).to.eq(200)
      expect(response?.body?.tyoskentelyjaksoLiitetty).to.eq(true)
      expect(response?.body?.tyoskentelyjaksonPituusRiittava).to.eq(true)
      expect(response?.body?.tyotodistusLiitetty).to.eq(true)
    })
    cy.get('[role="status"]', { timeout: 10000 }).should('not.exist')
    cy.contains('h1', 'Erikoisalan vastuuhenkilön arvio koejaksosta').should('be.visible')
    cy.contains('työskentelyjaksoa ei liitetty', { matchCase: false }).should('not.exist')
    cy.contains('työtodistusta ei liitetty', { matchCase: false }).should('not.exist')
    cy.contains(`${VASTUUHENKILO_ETUNIMI} ${VASTUUHENKILO_SUKUNIMI}`).should('be.visible')

    // Finish: Erikoistuja täyttää ja lähettää arviointipyynnön
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
    cy.contains('label', 'Koulutussuunnitelma')
      .parent()
      .find('input[type="checkbox"]')
      .check({ force: true })

    cy.contains('button', 'Lähetä').should('not.be.disabled').click()
    cy.get('#confirm-send').find('button').contains('Lähetä').click()
    cy.wait('@postVastuuhenkilonArvio').then(({ response }) => {
      expect(response?.statusCode).to.eq(201)
      expect(response?.body?.id).to.be.a('number')
      expect(response?.body?.erikoistuvanSahkoposti).to.eq(E2E_ERIKOISTUVA_EMAIL)
      expect(response?.body?.erikoistuvanPuhelinnumero).to.eq('+358401234567')
      expect(response?.body?.erikoistuvanKuittausaika).to.not.be.null
      Cypress.env('vastuuhenkilonArvioId', response?.body?.id)
    })
    cy.url().should('include', '/koejakso')
    cy.contains('Odottaa virkailijan hyväksyntää', {
      timeout: 15000,
    }).should('be.visible')

    cy.then(() => {
      const vastuuhenkilonArvioId = Cypress.env('vastuuhenkilonArvioId')

      // Finish: Virkailija tarkistaa arviointipyynnön
      cy.loginAsVirkailija(Cypress.env('virkailijaToken'))
      cy.apiRequest({
        method: 'GET',
        url: `/api/virkailija/koejakso/vastuuhenkilonarvio/${vastuuhenkilonArvioId}`,
      }).then(({ status, body }) => {
        expect(status).to.eq(200)
        expect(body.id).to.eq(vastuuhenkilonArvioId)
        expect(body.virkailija?.sopimusHyvaksytty).not.to.eq(true)

        return cy.apiRequest({
          method: 'PUT',
          url: '/api/virkailija/koejakso/vastuuhenkilonarvio',
          body: {
            ...body,
            lisatiedotVirkailijalta: 'E2E virkailijan lisätiedot vastuuhenkilölle.',
            virkailijanYhteenveto: 'E2E virkailijan yhteenveto koejaksosta.',
            virkailijanKorjausehdotus: null,
          },
        }).then(({ status, body }) => {
          expect(status).to.eq(200)
          expect(body.virkailija?.sopimusHyvaksytty).to.eq(true)
          expect(body.lisatiedotVirkailijalta).to.eq(
            'E2E virkailijan lisätiedot vastuuhenkilölle.'
          )
        })
      })

      // Finish: Vastuuhenkilö hyväksyy koejakson arvion
      cy.loginAsVastuuhenkilo(Cypress.env('vastuuhenkiloToken'))
      cy.apiRequest({
        method: 'GET',
        url: `/api/vastuuhenkilo/koejakso/vastuuhenkilonarvio/${vastuuhenkilonArvioId}`,
      }).then(({ status, body }) => {
        expect(status).to.eq(200)
        expect(body.id).to.eq(vastuuhenkilonArvioId)
        expect(body.virkailija?.sopimusHyvaksytty).to.eq(true)

        return cy.apiRequest({
          method: 'PUT',
          url: '/api/vastuuhenkilo/koejakso/vastuuhenkilonarvio',
          body: {
            ...body,
            koejaksoHyvaksytty: true,
            vastuuhenkilonKorjausehdotus: null,
            vastuuhenkilonSahkoposti: VASTUUHENKILO_EMAIL,
            vastuuhenkilonPuhelinnumero: '+358401234568',
          },
          timeout: 120000,
        }).then(({ status, body }) => {
          expect(status).to.eq(200)
          expect(body.vastuuhenkilo?.sopimusHyvaksytty).to.eq(true)
          expect(body.koejaksoHyvaksytty).to.eq(true)
          expect(body.vastuuhenkilo?.kuittausaika).to.not.be.null
          expect(body.vastuuhenkilonKorjausehdotus).to.be.null
        })
      })
    })
  })
})
