import {
  E2E_ERIKOISTUVA_EMAIL,
  KOULUTTAJA_EMAIL,
  VIRKAILIJA_EMAIL
} from '../../support/commands/credentials'
import {
  HEART,
  REVIEW_API,
  REVIEW_CONTACTS,
  RESPONSIBLE_CONTACTS,
  createReview,
  dismissPdfTextToast,
  expectPdfTextProblem,
  expectPdfTextStateUnchanged,
  expectPdfTextToast,
  pdfTextState
} from '../../support/pdf-text'

const SOURCE = 'koejaksonvastuuhenkilonarvio'
const OFFICER_API = '/api/virkailija/koejakso/vastuuhenkilonarvio'
const RESPONSIBLE_API = '/api/vastuuhenkilo/koejakso/vastuuhenkilonarvio'
const SUMMARY_LABEL = 'Opintohallinnon virkailijan yhteenveto'
const GOOD_SUMMARY =
  '<p><strong>Hyvä lääkäri</strong> &amp; pätevä kouluttaja – Jyväskylä</p>' +
  '<ul><li>Tavoite</li></ul><p>&amp;#x1F497; &#xF0B7;</p>'
const main = 'main[role="main"]'
const group = (label: string) => cy.get(main).contains('label', label).closest('.form-group')
const editor = () =>
  group(SUMMARY_LABEL)
    .find('iframe.tox-edit-area__iframe')
    .should('be.visible')
    .its('0.contentDocument.body')
    .should('not.be.empty')
    .then((body: HTMLBodyElement) => cy.wrap(Cypress.$(body)))

function openOfficer(id: number) {
  cy.loginAsVirkailija(Cypress.env('virkailijaToken'))
  cy.intercept('GET', `**${OFFICER_API}/${id}`).as('officerReview')
  cy.intercept('PUT', `**${OFFICER_API}`).as('saveOfficer')
  cy.visit(`/koejakso/virkailijan-tarkistus/${id}`)
  return cy.wait('@officerReview').then(({ response }) => {
    expect(response?.statusCode).to.eq(200)
    expect(response?.body.virkailija?.sopimusHyvaksytty ?? false).to.eq(false)
    return response?.body
  })
}

function openResponsible(id: number) {
  cy.loginAsVirkailija(Cypress.env('virkailijaToken'))
  cy.apiRequest({ method: 'GET', url: `${OFFICER_API}/${id}` }).then(({ status, body }) => {
    expect(status).to.eq(200)
    return cy
      .apiRequest({
        method: 'PUT',
        url: OFFICER_API,
        body: {
          ...body,
          virkailijanKorjausehdotus: null,
          virkailijanYhteenveto: GOOD_SUMMARY
        }
      })
      .then(({ status }) => expect(status).to.eq(200))
  })
  return visitResponsible(id)
}

function visitResponsible(id: number) {
  cy.loginAsVastuuhenkilo(Cypress.env('vastuuhenkiloToken'))
  cy.intercept('GET', `**${RESPONSIBLE_API}/${id}`).as('responsibleReview')
  cy.intercept('PUT', `**${RESPONSIBLE_API}`).as('saveResponsible')
  cy.visit(`/koejakso/vastuuhenkilon-arvio/${id}`)
  return cy.wait('@responsibleReview').then(({ response }) => {
    expect(response?.statusCode).to.eq(200)
    expect(response?.body.arkistoitava, 'external archiving disabled in E2E').to.eq(false)
    expect(response?.body.vastuuhenkilo?.sopimusHyvaksytty ?? false).to.eq(false)
    return response?.body
  })
}

function fillResponsible() {
  group('Sähköpostiosoite')
    .find('input')
    .clear()
    .type(RESPONSIBLE_CONTACTS.vastuuhenkilonSahkoposti)
  group('Matkapuhelinnumero')
    .find('input')
    .clear()
    .type(RESPONSIBLE_CONTACTS.vastuuhenkilonPuhelinnumero)
}

function confirmResponsible() {
  cy.get(main)
    .contains('button', /^\s*Hyväksy\s*$/)
    .should('not.be.disabled')
    .click()
  cy.get('#confirm-sign')
    .should('be.visible')
    .contains('button', /^\s*Hyväksy\s*$/)
    .click()
}

function checkNewPdf() {
  cy.get<Record<string, unknown>>('@beforePdfTextFailure').then((before) => {
    pdfTextState().then((after) => {
      const previous = before.asiakirjat as { id: number }[]
      const current = after.asiakirjat as {
        id: number
        nimi: string
        tyyppi: string
      }[]
      const created = current.filter((doc) => !previous.some((old) => old.id === doc.id))
      expect(created).to.have.length(1)
      expect(created[0].tyyppi).to.eq('application/pdf')
    })
  })
}

describe('Koejakson loppuarvion PDF-erikoismerkit', () => {
  before(() =>
    cy.prepareKoejaksoE2e({
      cleanupSupportUsers: true,
      seedVirkailija: true,
      storeTokens: true
    })
  )
  beforeEach(() => {
    cy.loginAsErikoistuva()
    cy.task('db:cleanupKoejakso', { erikoistuvaEmail: E2E_ERIKOISTUVA_EMAIL })
    cy.task('db:ensureLoppukeskusteluHyvaksytty', {
      erikoistuvaEmail: E2E_ERIKOISTUVA_EMAIL,
      kouluttajaEmail: KOULUTTAJA_EMAIL
    })
    cy.task('db:ensureKoejaksoTyoskentelyjakso', {
      email: E2E_ERIKOISTUVA_EMAIL
    })
  })
  after(() => {
    cy.task('db:cleanupKoejakso', { erikoistuvaEmail: E2E_ERIKOISTUVA_EMAIL })
    cy.task('db:cleanupErikoistuva', { email: E2E_ERIKOISTUVA_EMAIL })
  })

  it('hylkää erikoistuvan tukemattomat yhteystiedot luonnissa ja päivityksessä', () => {
    pdfTextState().as('beforePdfTextFailure', { type: 'static' })
    const contacts = [
      { property: 'erikoistuvanSahkoposti', field: 'sahkoposti' },
      { property: 'erikoistuvanPuhelinnumero', field: 'puhelinnumero' }
    ]
    contacts.forEach(({ property, field }) => {
      cy.apiRequest({
        method: 'POST',
        url: REVIEW_API,
        form: true,
        failOnStatusCode: false,
        body: {
          vastuuhenkilonArvioJson: JSON.stringify({
            ...REVIEW_CONTACTS,
            [property]: HEART
          })
        }
      }).then(({ status, body }) => expectPdfTextProblem(status, body, field, SOURCE))
      expectPdfTextStateUnchanged()
    })
    createReview().then((id) => {
      cy.apiRequest({
        method: 'GET',
        url: '/api/erikoistuva-laakari/koejakso'
      }).then(({ body }) => {
        expect(body.vastuuhenkilonArvio.id).to.eq(id)
        expect(body.vastuuhenkilonArvio.erikoistuvanSahkoposti).to.eq(
          REVIEW_CONTACTS.erikoistuvanSahkoposti
        )
        const original = body.vastuuhenkilonArvio
        pdfTextState().as('beforePdfTextFailure', { type: 'static' })
        contacts.forEach(({ property, field }) => {
          cy.apiRequest({
            method: 'PUT',
            url: REVIEW_API,
            form: true,
            failOnStatusCode: false,
            body: {
              vastuuhenkilonArvioJson: JSON.stringify({ ...original, [property]: HEART }),
              deletedAsiakirjaIdsJson: '[]'
            }
          }).then(({ status, body }) => expectPdfTextProblem(status, body, field, SOURCE))
          expectPdfTextStateUnchanged()
        })
        cy.apiRequest({
          method: 'PUT',
          url: REVIEW_API,
          form: true,
          body: {
            vastuuhenkilonArvioJson: JSON.stringify({
              ...original,
              erikoistuvanPuhelinnumero: '+358401234570'
            }),
            deletedAsiakirjaIdsJson: '[]'
          }
        }).then(({ status }) => expect(status).to.eq(200))
        cy.apiRequest({ method: 'GET', url: '/api/erikoistuva-laakari/koejakso' }).then(
          ({ body }) => {
            expect(body.vastuuhenkilonArvio.erikoistuvanPuhelinnumero).to.eq('+358401234570')
          }
        )
      })
    })
  })

  it('hylkää tukemattoman liitenimen ja lähettää uudelleen kelvollisella nimellä', () => {
    cy.intercept('POST', `**${REVIEW_API}`).as('createReviewWithFile')
    const fillForm = () => {
      cy.visit('/koejakso/vastuuhenkilon-arvio')
      cy.get('[role="status"]', { timeout: 10000 }).should('not.exist')
      group('Sähköpostiosoite').find('input').clear().type(REVIEW_CONTACTS.erikoistuvanSahkoposti)
      group('Matkapuhelinnumero')
        .find('input')
        .clear()
        .type(REVIEW_CONTACTS.erikoistuvanPuhelinnumero)
      group('Koulutussuunnitelma').find('input[type="checkbox"]').check({ force: true })
    }
    const attach = (fileName: string) =>
      cy.fixture('test.pdf', null).then((contents) => {
        cy.get(main)
          .find('input[type="file"]')
          .first()
          .should('not.be.disabled')
          .selectFile({ contents, fileName, mimeType: 'application/pdf' }, { force: true })
      })
    const send = () => {
      cy.get(main)
        .contains('button', /^\s*Lähetä\s*$/)
        .should('not.be.disabled')
        .click()
      cy.get('#confirm-send')
        .contains('button', /^\s*Lähetä\s*$/)
        .click()
    }
    fillForm()
    attach(`Työtodistus ${HEART}.pdf`)
    pdfTextState().as('beforePdfTextFailure', { type: 'static' })
    send()
    cy.wait('@createReviewWithFile').then(({ response }) => {
      expectPdfTextProblem(response?.statusCode, response?.body, 'liitetiedoston-nimi', SOURCE)
    })
    expectPdfTextToast('Liitetiedoston nimi')
    expectPdfTextStateUnchanged()
    cy.get(main).contains(`Työtodistus ${HEART}.pdf`).should('be.visible')
    dismissPdfTextToast()
    // Reload the failed, unsaved request to select the same bytes with a corrected filename.
    cy.reload()
    fillForm()
    attach('Työtodistus.pdf')
    send()
    cy.wait('@createReviewWithFile').then(({ response }) => {
      expect(response?.statusCode).to.eq(201)
      expect(response?.body.asiakirjat.map((doc: { nimi: string }) => doc.nimi)).to.include(
        'Työtodistus.pdf'
      )
    })
  })

  it('hylkää literal-, numero- ja HTML-entiteetit virkailijan yhteenvedossa myös palautuksessa', () => {
    createReview().then((id) => {
      openOfficer(id).then((original) => {
        pdfTextState().as('beforePdfTextFailure', { type: 'static' })
        const summaries = [HEART, '&#x1F497;', '&#X1f497;', '&#128151;', '&check;']
        summaries.forEach((character) => {
          // Send encoded HTML unchanged; entering it in TinyMCE may normalise it before saving.
          cy.apiRequest({
            method: 'PUT',
            url: OFFICER_API,
            failOnStatusCode: false,
            body: {
              ...original,
              virkailijanYhteenveto: `<p><strong>${character}</strong></p>`,
              virkailijanKorjausehdotus: null
            }
          }).then(({ status, body }) =>
            expectPdfTextProblem(status, body, 'virkailijan-valmistumisen-yhteenveto', SOURCE, [
              character === '&check;' ? '✓ (U+2713)' : '💗 (U+1F497)'
            ])
          )
          expectPdfTextStateUnchanged()
        })
        cy.apiRequest({
          method: 'PUT',
          url: OFFICER_API,
          failOnStatusCode: false,
          body: {
            ...original,
            virkailijanYhteenveto: '<p>&#x1F497;</p>',
            virkailijanKorjausehdotus: 'Korjaa päivämäärä'
          }
        }).then(({ status, body }) =>
          expectPdfTextProblem(status, body, 'virkailijan-valmistumisen-yhteenveto', SOURCE)
        )
        expectPdfTextStateUnchanged()
        ;[
          {
            property: 'lisatiedotVirkailijalta',
            field: 'lisatiedot-vastuuhenkilolle'
          },
          { property: 'virkailijanKorjausehdotus', field: 'korjausehdotus' }
        ].forEach(({ property, field }) => {
          cy.apiRequest({
            method: 'PUT',
            url: OFFICER_API,
            failOnStatusCode: false,
            body: {
              ...original,
              virkailijanYhteenveto: GOOD_SUMMARY,
              [property]: HEART
            }
          }).then(({ status, body }) => expectPdfTextProblem(status, body, field, SOURCE))
          expectPdfTextStateUnchanged()
        })
        cy.apiRequest({
          method: 'PUT',
          url: OFFICER_API,
          body: {
            ...original,
            virkailijanYhteenveto: GOOD_SUMMARY,
            lisatiedotVirkailijalta: 'Kirjaimellinen teksti &#x1F497;',
            virkailijanKorjausehdotus: null
          }
        }).then(({ status, body }) => {
          expect(status).to.eq(200)
          expect(body.virkailija.sopimusHyvaksytty).to.eq(true)
        })
        cy.apiRequest({ method: 'GET', url: `${OFFICER_API}/${id}` }).then(({ body }) => {
          expect(body.virkailijanYhteenveto).to.eq(GOOD_SUMMARY)
          expect(body.lisatiedotVirkailijalta).to.eq('Kirjaimellinen teksti &#x1F497;')
        })
      })
    })
  })

  it('näyttää editorin virheen, säilyttää tekstin ja mahdollistaa korjauksen sekä uuden lähetyksen', () => {
    createReview().then((id) => {
      openOfficer(id)
      editor().type(`Hyvä lääkäri ${HEART}`)
      pdfTextState().as('beforePdfTextFailure', { type: 'static' })
      cy.get(main).contains('button', 'Hyväksy ja lähetä').click()
      cy.get('#confirm-sign').contains('button', 'Hyväksy ja lähetä').click()
      cy.wait('@saveOfficer').then(({ response }) =>
        expectPdfTextProblem(
          response?.statusCode,
          response?.body,
          'virkailijan-valmistumisen-yhteenveto',
          SOURCE
        )
      )
      expectPdfTextToast(SUMMARY_LABEL)
      expectPdfTextStateUnchanged()
      editor().should('contain.text', HEART).type('{selectall}Hyvä lääkäri – Jyväskylä')
      dismissPdfTextToast()
      cy.get(main).contains('button', 'Hyväksy ja lähetä').should('not.be.disabled').click()
      cy.get('#confirm-sign')
        .contains('label', 'Lisätiedot vastuuhenkilölle')
        .parent()
        .find('textarea')
        .type('Hyvä kehitys')
      cy.get('#confirm-sign').contains('button', 'Hyväksy ja lähetä').click()
      cy.wait('@saveOfficer').its('response.statusCode').should('eq', 200)
      cy.apiRequest({ method: 'GET', url: `${OFFICER_API}/${id}` }).then(({ body }) => {
        expect(body.virkailija.sopimusHyvaksytty).to.eq(true)
        expect(Cypress.$('<div>').html(body.virkailijanYhteenveto).text()).to.contain(
          'Hyvä lääkäri – Jyväskylä'
        )
        expect(body.lisatiedotVirkailijalta).to.eq('Hyvä kehitys')
      })
    })
  })

  it('hylkää virheellisen palautussyyn ja sallii korjatun palautuksen käyttöliittymässä', () => {
    createReview().then((id) => {
      openOfficer(id)
      pdfTextState().as('beforePdfTextFailure', { type: 'static' })
      cy.get(main).contains('button', 'Palauta muokattavaksi').click()
      cy.get('#return-to-sender').find('textarea').type(`Korjaa ${HEART}`)
      cy.get('#return-to-sender').contains('button', 'Palauta muokattavaksi').click()
      cy.wait('@saveOfficer').then(({ response }) =>
        expectPdfTextProblem(response?.statusCode, response?.body, 'korjausehdotus', SOURCE)
      )
      expectPdfTextToast('Korjausehdotus')
      expectPdfTextStateUnchanged()
      dismissPdfTextToast()
      cy.get(main).contains('button', 'Palauta muokattavaksi').should('not.be.disabled').click()
      cy.get('#return-to-sender').find('textarea').clear().type('Korjaa päivämäärä')
      cy.get('#return-to-sender').contains('button', 'Palauta muokattavaksi').click()
      cy.wait('@saveOfficer').its('response.statusCode').should('eq', 200)
      cy.apiRequest({ method: 'GET', url: `${OFFICER_API}/${id}` }).then(({ body }) => {
        expect(body.virkailijanKorjausehdotus).to.eq('Korjaa päivämäärä')
        expect(body.erikoistuvanKuittausaika).to.be.null
      })
    })
  })

  it('tarkistaa vastuuhenkilön yhteystiedot, palautussyyn ja hylkäysperustelun muuttamatta aiempia kuittauksia', () => {
    createReview().then((id) => {
      openResponsible(id).then((original) => {
        pdfTextState().as('beforePdfTextFailure', { type: 'static' })
        const fields = [
          { property: 'vastuuhenkilonSahkoposti', field: 'sahkoposti' },
          { property: 'vastuuhenkilonPuhelinnumero', field: 'puhelinnumero' },
          { property: 'vastuuhenkilonKorjausehdotus', field: 'korjausehdotus' },
          {
            property: 'perusteluHylkaamiselle',
            field: 'perustelu-hylkaamiselle'
          }
        ]
        fields.forEach(({ property, field }) => {
          cy.apiRequest({
            method: 'PUT',
            url: RESPONSIBLE_API,
            failOnStatusCode: false,
            body: {
              ...original,
              ...RESPONSIBLE_CONTACTS,
              koejaksoHyvaksytty: false,
              perusteluHylkaamiselle: 'Lisää harjoittelua',
              vastuuhenkilonKorjausehdotus: null,
              [property]: HEART
            }
          }).then(({ status, body }) => expectPdfTextProblem(status, body, field, SOURCE))
          expectPdfTextStateUnchanged()
        })
      })
      fillResponsible()
      group('Koejakso on').find('input[type="radio"][value="false"]').check({ force: true })
      group('Perustelu hylkäämiselle').find('textarea').type(`Lisää harjoittelua ${HEART}`)
      group('Hylätyn koejakson arviointi on käyty läpi koejakson suorittajan kanssa keskustellen')
        .find('input[type="checkbox"]')
        .check({ force: true })
      confirmResponsible()
      cy.wait('@saveResponsible').then(({ response }) =>
        expectPdfTextProblem(
          response?.statusCode,
          response?.body,
          'perustelu-hylkaamiselle',
          SOURCE
        )
      )
      expectPdfTextToast('Perustelu hylkäämiselle')
      expectPdfTextStateUnchanged()
      group('Perustelu hylkäämiselle')
        .find('textarea')
        .should('have.value', `Lisää harjoittelua ${HEART}`)
        .clear()
        .type('Tarvitaan lisää harjoittelua')
      dismissPdfTextToast()
      confirmResponsible()
      cy.wait('@saveResponsible', { responseTimeout: 120000 }).then(({ response }) => {
        expect(response?.statusCode).to.eq(200)
        expect(response?.body.koejaksoHyvaksytty).to.eq(false)
        expect(response?.body.vastuuhenkilo.sopimusHyvaksytty).to.eq(true)
      })
      checkNewPdf()
      cy.apiRequest({ method: 'GET', url: `${RESPONSIBLE_API}/${id}` }).then(({ body }) => {
        expect(body.perusteluHylkaamiselle).to.eq('Tarvitaan lisää harjoittelua')
        expect(body.virkailija.sopimusHyvaksytty).to.eq(true)
      })
    })
  })

  it('estää vanhan HTML-entiteetin PDF-hyväksynnän ja sallii korjatun tekstin', () => {
    const legacySummary = '<p>Vanha &#x1F497;</p>'
    cy.task<number>('db:createLegacyPdfReview', {
      email: E2E_ERIKOISTUVA_EMAIL,
      virkailijaEmail: VIRKAILIJA_EMAIL,
      text: legacySummary
    }).then((id) => {
      visitResponsible(id).then((body) => {
        expect(body.virkailijanYhteenveto, 'backend must load the legacy entity').to.eq(
          legacySummary
        )
        expect(body.virkailija.sopimusHyvaksytty).to.eq(true)
      })
      fillResponsible()
      group('Koejakso on').find('input[type="radio"][value="true"]').check({ force: true })
      pdfTextState().as('beforePdfTextFailure', { type: 'static' })
      confirmResponsible()
      cy.wait('@saveResponsible', { responseTimeout: 120000 }).then(({ response }) => {
        expectPdfTextProblem(
          response?.statusCode,
          response?.body,
          'pdf-osio-koejakson-vastuuhenkilon-arvio',
          SOURCE
        )
      })
      expectPdfTextToast('Koejakson vastuuhenkilön arvio')
      expectPdfTextStateUnchanged()
      cy.apiRequest({ method: 'GET', url: `${RESPONSIBLE_API}/${id}` }).then(({ body }) => {
        expect(body.vastuuhenkilo?.sopimusHyvaksytty ?? false).to.eq(false)
        expect(body.koejaksoHyvaksytty).to.be.null
      })
      // Repair through the real return/resubmit workflow so Hibernate's cache
      // stays consistent with the database throughout the retry.
      cy.apiRequest({ method: 'GET', url: `${RESPONSIBLE_API}/${id}` }).then(({ body }) => {
        cy.apiRequest({
          method: 'PUT',
          url: RESPONSIBLE_API,
          body: {
            ...body,
            ...RESPONSIBLE_CONTACTS,
            vastuuhenkilonKorjausehdotus: 'Korjaa yhteenvedon erikoismerkit'
          }
        }).then(({ status, body }) => {
          expect(status).to.eq(200)
          expect(body.virkailija.sopimusHyvaksytty).to.eq(false)
          expect(body.erikoistuvanKuittausaika).to.be.null
        })
      })
      dismissPdfTextToast()
      cy.loginAsErikoistuva()
      cy.apiRequest({
        method: 'PUT',
        url: REVIEW_API,
        form: true,
        body: { vastuuhenkilonArvioJson: JSON.stringify({ id, ...REVIEW_CONTACTS }) }
      }).then(({ status, body }) => {
        expect(status).to.eq(200)
        expect(body.erikoistuvanKuittausaika).not.to.be.null
      })
      openResponsible(id).then((body) => {
        expect(body.virkailijanYhteenveto).to.eq(GOOD_SUMMARY)
        expect(body.virkailija.sopimusHyvaksytty).to.eq(true)
      })
      fillResponsible()
      group('Koejakso on').find('input[type="radio"][value="true"]').check({ force: true })
      confirmResponsible()
      cy.wait('@saveResponsible', { responseTimeout: 120000 }).then(({ response }) => {
        expect(response?.statusCode).to.eq(200)
        expect(response?.body.koejaksoHyvaksytty).to.eq(true)
        expect(response?.body.vastuuhenkilo.sopimusHyvaksytty).to.eq(true)
      })
      checkNewPdf()
    })
  })
})
