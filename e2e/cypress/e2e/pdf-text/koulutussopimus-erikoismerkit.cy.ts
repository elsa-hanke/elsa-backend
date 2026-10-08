import {
  E2E_ERIKOISTUVA_EMAIL,
  KOULUTTAJA_EMAIL,
  VASTUUHENKILO_EMAIL
} from '../../support/commands/credentials'
import {
  HEART,
  dismissPdfTextToast,
  expectPdfTextProblem,
  expectPdfTextStateUnchanged,
  expectPdfTextToast,
  pdfTextState
} from '../../support/pdf-text'

const SOURCE = 'koejaksonkoulutussopimus'
const TRAINER_API = '/api/kouluttaja/koejakso/koulutussopimus'
const RESPONSIBLE_API = '/api/vastuuhenkilo/koejakso/koulutussopimus'
const TRAINER_FIELDS = [
  {
    label: 'Kouluttajan nimike',
    property: 'nimike',
    field: 'nimike',
    value: 'Erikoislääkäri'
  },
  {
    label: 'Toimipaikka',
    property: 'toimipaikka',
    field: 'toimipaikka',
    value: 'Jyväskylän terveyskeskus'
  },
  {
    label: 'Lähiosoite',
    property: 'lahiosoite',
    field: 'lahiosoite',
    value: 'Lääkärinkatu 1'
  },
  {
    label: 'Postitoimipaikka',
    property: 'postitoimipaikka',
    field: 'postitoimipaikka',
    value: '00100 Helsinki'
  },
  {
    label: 'Sähköpostiosoite',
    property: 'sahkoposti',
    field: 'sahkoposti',
    value: KOULUTTAJA_EMAIL
  },
  {
    label: 'Matkapuhelinnumero',
    property: 'puhelin',
    field: 'puhelinnumero',
    value: '+358401234568'
  }
] as const
const trainerInput = (label: string) =>
  cy.get('.kouluttaja-section').contains('label', label).closest('.form-group').find('input')
const trainerData = () =>
  Object.fromEntries(TRAINER_FIELDS.map(({ property, value }) => [property, value]))
const approveTrainer = (body: any) =>
  cy
    .apiRequest({
      method: 'PUT',
      url: TRAINER_API,
      body: {
        ...body,
        korjausehdotus: null,
        kouluttajat: [{ ...body.kouluttajat[0], ...trainerData() }]
      }
    })
    .then(({ status }) => expect(status).to.eq(200))

function openResponsible(id: number) {
  cy.loginAsKouluttaja(Cypress.env('kouluttajaToken'))
  cy.apiRequest({ method: 'GET', url: `${TRAINER_API}/${id}` }).then(({ status, body }) => {
    expect(status).to.eq(200)
    return approveTrainer(body)
  })
  cy.loginAsVastuuhenkilo(Cypress.env('vastuuhenkiloToken'))
  cy.intercept('GET', `**${RESPONSIBLE_API}/${id}`).as('responsibleContract')
  cy.intercept('PUT', `**${RESPONSIBLE_API}`).as('approveResponsible')
  cy.visit(`/koejakso/koulutussopimus/${id}`)
  return cy.wait('@responsibleContract').then(({ response }) => {
    expect(response?.statusCode).to.eq(200)
    expect(response?.body.vastuuhenkilo.sopimusHyvaksytty).to.eq(false)
  })
}

function fillResponsible() {
  cy.get('.koulutussopimus')
    .contains('label', 'Sähköpostiosoite')
    .closest('.form-group')
    .find('input')
    .clear()
    .type(VASTUUHENKILO_EMAIL)
  cy.get('.koulutussopimus')
    .contains('label', 'Matkapuhelinnumero')
    .closest('.form-group')
    .find('input')
    .clear()
    .type('+358401234569')
}

function confirmResponsible() {
  cy.get('.koulutussopimus')
    .contains('button[type="submit"]', /^\s*Hyväksy\s*$/)
    .should('not.be.disabled')
    .click()
  cy.get('#confirm-send-vastuuhenkilo').should('be.visible').contains('button', 'Hyväksy').click()
}

describe('Koulutussopimuksen PDF-erikoismerkit', () => {
  before(() => cy.prepareKoejaksoE2e({ cleanupSupportUsers: true, storeTokens: true }))
  beforeEach(() => {
    cy.loginAsErikoistuva()
    cy.task('db:cleanupKoejakso', { erikoistuvaEmail: E2E_ERIKOISTUVA_EMAIL })
  })
  after(() => {
    cy.task('db:cleanupKoejakso', { erikoistuvaEmail: E2E_ERIKOISTUVA_EMAIL })
    cy.task('db:cleanupErikoistuva', { email: E2E_ERIKOISTUVA_EMAIL })
  })

  it('hylkää luonnin jokaisen kirjoitettavan tekstikentän ennen tallennusta', () => {
    pdfTextState().as('beforePdfTextFailure', { type: 'static' })
    const cases = [
      { field: 'sahkoposti', change: { erikoistuvanSahkoposti: HEART } },
      { field: 'puhelinnumero', change: { erikoistuvanPuhelinnumero: HEART } },
      {
        field: 'koulutuspaikan-nimi',
        change: { koulutuspaikat: [{ nimi: HEART }] }
      },
      ...['toimipaikka', 'lahiosoite', 'postitoimipaikka'].map((field) => ({
        field,
        change: { kouluttajat: [{ [field]: HEART }] }
      }))
    ]
    cases.forEach(({ field, change }) => {
      cy.apiRequest({
        method: 'POST',
        url: '/api/erikoistuva-laakari/koejakso/koulutussopimus',
        failOnStatusCode: false,
        body: {
          lahetetty: false,
          erikoistuvanSahkoposti: E2E_ERIKOISTUVA_EMAIL,
          erikoistuvanPuhelinnumero: '+358401234567',
          ...change
        }
      }).then(({ status, body }) => expectPdfTextProblem(status, body, field, SOURCE))
      expectPdfTextStateUnchanged()
    })
  })

  it('tarkistaa myös erikoistuvan luonnoksen päivityksen ja tallentaa korjatut tiedot', () => {
    const url = '/api/erikoistuva-laakari/koejakso/koulutussopimus'
    cy.apiRequest({
      method: 'POST',
      url,
      body: {
        lahetetty: false,
        erikoistuvanSahkoposti: E2E_ERIKOISTUVA_EMAIL,
        erikoistuvanPuhelinnumero: '+358401234567',
        koejaksonAlkamispaiva: new Date().toISOString().slice(0, 10),
        koulutuspaikat: [
          { nimi: 'Jyväskylän sairaala', koulutussopimusOmanYliopistonKanssa: true }
        ],
        kouluttajat: [
          {
            kayttajaId: Cypress.env('kouluttajaId'),
            toimipaikka: 'Jyväskylä',
            lahiosoite: 'Lääkärinkatu 1',
            postitoimipaikka: 'Jyväskylä'
          }
        ]
      }
    }).then(({ status, body: original }) => {
      expect(status).to.eq(201)
      pdfTextState().as('beforePdfTextFailure', { type: 'static' })
      const cases = [
        { field: 'sahkoposti', change: { erikoistuvanSahkoposti: HEART } },
        { field: 'puhelinnumero', change: { erikoistuvanPuhelinnumero: HEART } },
        {
          field: 'koulutuspaikan-nimi',
          change: { koulutuspaikat: [{ ...original.koulutuspaikat[0], nimi: HEART }] }
        },
        ...['toimipaikka', 'lahiosoite', 'postitoimipaikka'].map((field) => ({
          field,
          change: { kouluttajat: [{ ...original.kouluttajat[0], [field]: HEART }] }
        }))
      ]
      cases.forEach(({ field, change }) => {
        cy.apiRequest({
          method: 'PUT',
          url,
          failOnStatusCode: false,
          body: { ...original, ...change }
        }).then(({ status, body }) => expectPdfTextProblem(status, body, field, SOURCE))
        expectPdfTextStateUnchanged()
      })
      cy.apiRequest({
        method: 'PUT',
        url,
        body: {
          ...original,
          koulutuspaikat: [
            { ...original.koulutuspaikat[0], nimi: 'Päivitetty Jyväskylän sairaala' }
          ]
        }
      }).then(({ status }) => expect(status).to.eq(200))
      cy.apiRequest({ method: 'GET', url: '/api/erikoistuva-laakari/koejakso' }).then(
        ({ body }) => {
          expect(body.koulutussopimus.lahetetty).to.eq(false)
          expect(body.koulutussopimus.koulutuspaikat[0].nimi).to.eq(
            'Päivitetty Jyväskylän sairaala'
          )
        }
      )
    })
  })

  it('säilyttää koulutuspaikan virheellisen syötteen ja lähettää vasta korjauksen jälkeen', () => {
    cy.visit('/koejakso/koulutussopimus')
    cy.get('[role="status"]', { timeout: 10000 }).should('not.exist')
    cy.contains('label', 'Sähköpostiosoite')
      .parent()
      .find('input')
      .clear()
      .type(E2E_ERIKOISTUVA_EMAIL)
    cy.contains('label', 'Matkapuhelinnumero').parent().find('input').clear().type('+358401234567')
    const place = () =>
      cy.contains('label', 'Toimipaikan nimi').parent().find('input[type="text"]').first()
    place().clear().type(`Jyväskylän sairaala ${HEART}`)
    cy.contains('label', 'Kyllä').click()
    const today = new Date()
    const date = `${String(today.getDate()).padStart(2, '0')}.${String(
      today.getMonth() + 1
    ).padStart(2, '0')}.${today.getFullYear()}`
    cy.contains('label', 'Koejakson alkamispäivä')
      .parent()
      .find('input.date-input')
      .clear()
      .type(date)
      .blur()
    cy.contains('label', 'Kouluttaja').parent().find('.multiselect').click()
    cy.get('.multiselect--active .multiselect__option')
      .contains('Lassekalevi Hummaamistes')
      .click({ force: true })
    cy.intercept('POST', '**/erikoistuva-laakari/koejakso/koulutussopimus').as('createContract')
    pdfTextState().as('beforePdfTextFailure', { type: 'static' })
    cy.contains('button', 'Hyväksy ja lähetä').click()
    cy.get('#confirm-send').contains('button', 'Hyväksy ja lähetä').click()
    cy.wait('@createContract').then(({ request, response }) => {
      expect(request.body.koulutuspaikat[0].nimi).to.eq(`Jyväskylän sairaala ${HEART}`)
      expectPdfTextProblem(response?.statusCode, response?.body, 'koulutuspaikan-nimi', SOURCE)
    })
    expectPdfTextToast('Koulutuspaikan nimi')
    expectPdfTextStateUnchanged()
    place().should('have.value', `Jyväskylän sairaala ${HEART}`).clear().type('Jyväskylän sairaala')
    dismissPdfTextToast()
    cy.contains('button', 'Hyväksy ja lähetä').should('not.be.disabled').click()
    cy.get('#confirm-send').contains('button', 'Hyväksy ja lähetä').click()
    cy.wait('@createContract').then(({ response }) => {
      expect(response?.statusCode).to.eq(201)
      expect(response?.body.koulutuspaikat[0].nimi).to.eq('Jyväskylän sairaala')
    })
    cy.reload()
    cy.contains('Jyväskylän sairaala').should('be.visible')
  })

  it('tarkistaa kouluttajan kaikki kentät ja sallii korjatun hyväksynnän käyttöliittymästä', () => {
    cy.submitKoulutussopimusViaUi('Lassekalevi Hummaamistes').then((id) => {
      cy.loginAsKouluttaja(Cypress.env('kouluttajaToken'))
      cy.apiRequest({ method: 'GET', url: `${TRAINER_API}/${id}` }).then(
        ({ status, body: original }) => {
          expect(status).to.eq(200)
          pdfTextState().as('beforePdfTextFailure', { type: 'static' })
          TRAINER_FIELDS.forEach(({ property, field }) => {
            cy.apiRequest({
              method: 'PUT',
              url: TRAINER_API,
              failOnStatusCode: false,
              body: {
                ...original,
                korjausehdotus: null,
                kouluttajat: [
                  {
                    ...original.kouluttajat[0],
                    ...trainerData(),
                    [property]: HEART
                  }
                ]
              }
            }).then(({ status, body }) => expectPdfTextProblem(status, body, field, SOURCE))
            expectPdfTextStateUnchanged()
          })
          cy.apiRequest({
            method: 'PUT',
            url: TRAINER_API,
            failOnStatusCode: false,
            body: { ...original, korjausehdotus: `Korjaa ${HEART}` }
          }).then(({ status, body }) =>
            expectPdfTextProblem(status, body, 'korjausehdotus', SOURCE)
          )
          expectPdfTextStateUnchanged()
        }
      )
      cy.intercept('GET', `**${TRAINER_API}/${id}`).as('trainerContract')
      cy.intercept('PUT', `**${TRAINER_API}`).as('approveTrainer')
      cy.visit(`/koejakso/koulutussopimus/${id}`)
      cy.wait('@trainerContract').its('response.statusCode').should('eq', 200)
      TRAINER_FIELDS.forEach(({ label, value }) => trainerInput(label).clear().type(value))
      trainerInput('Kouluttajan nimike').clear().type(`Erikoislääkäri ${HEART}`)
      cy.get('.koulutussopimus').contains('button', 'Hyväksy ja lähetä').click()
      cy.get('#confirm-send-kouluttaja').contains('button', 'Hyväksy ja lähetä').click()
      cy.wait('@approveTrainer').then(({ response }) =>
        expectPdfTextProblem(response?.statusCode, response?.body, 'nimike', SOURCE)
      )
      expectPdfTextToast('Nimike')
      expectPdfTextStateUnchanged()
      trainerInput('Kouluttajan nimike')
        .should('have.value', `Erikoislääkäri ${HEART}`)
        .clear()
        .type('Erikoislääkäri')
      dismissPdfTextToast()
      cy.get('.koulutussopimus')
        .contains('button', 'Hyväksy ja lähetä')
        .should('not.be.disabled')
        .click()
      cy.get('#confirm-send-kouluttaja').contains('button', 'Hyväksy ja lähetä').click()
      cy.wait('@approveTrainer').then(({ response }) => {
        expect(response?.statusCode).to.eq(200)
        expect(response?.body.kouluttajat[0].sopimusHyvaksytty).to.eq(true)
      })
      cy.apiRequest({ method: 'GET', url: `${TRAINER_API}/${id}` }).then(({ body }) => {
        expect(body.kouluttajat[0].nimike).to.eq('Erikoislääkäri')
        expect(body.kouluttajat[0].kuittausaika).not.to.be.null
      })
    })
  })

  it('tarkistaa vastuuhenkilön yhteystiedot ja korjausehdotuksen ennen tilamuutoksia', () => {
    cy.submitKoulutussopimusViaUi('Lassekalevi Hummaamistes').then((id) => {
      openResponsible(id)
      cy.apiRequest({ method: 'GET', url: `${RESPONSIBLE_API}/${id}` }).then(
        ({ body: original }) => {
          pdfTextState().as('beforePdfTextFailure', { type: 'static' })
          ;['sahkoposti', 'puhelin'].forEach((property) => {
            cy.apiRequest({
              method: 'PUT',
              url: RESPONSIBLE_API,
              failOnStatusCode: false,
              body: {
                ...original,
                korjausehdotus: null,
                vastuuhenkilo: { ...original.vastuuhenkilo, [property]: HEART }
              }
            }).then(({ status, body }) =>
              expectPdfTextProblem(
                status,
                body,
                property === 'puhelin' ? 'puhelinnumero' : property,
                SOURCE
              )
            )
            expectPdfTextStateUnchanged()
          })
          cy.apiRequest({
            method: 'PUT',
            url: RESPONSIBLE_API,
            failOnStatusCode: false,
            body: { ...original, korjausehdotus: `Korjaa ${HEART}` }
          }).then(({ status, body }) =>
            expectPdfTextProblem(status, body, 'korjausehdotus', SOURCE)
          )
          expectPdfTextStateUnchanged()
        }
      )
      fillResponsible()
      confirmResponsible()
      cy.wait('@approveResponsible', { responseTimeout: 120000 }).then(({ response }) => {
        expect(response?.statusCode).to.eq(200)
        expect(response?.body.vastuuhenkilo.sopimusHyvaksytty).to.eq(true)
      })
    })
  })

  it('estää vanhan tukemattoman tekstin PDF-hyväksynnän ja sallii hyväksynnän korjauksen jälkeen', () => {
    cy.submitKoulutussopimusViaUi('Lassekalevi Hummaamistes').then((id) => {
      openResponsible(id)
      cy.task('db:setLegacyPdfText', {
        email: E2E_ERIKOISTUVA_EMAIL,
        id,
        target: 'contract-place',
        text: `Vanha sairaala ${HEART}`
      })
      cy.reload()
      cy.wait('@responsibleContract').its('response.statusCode').should('eq', 200)
      fillResponsible()
      pdfTextState().as('beforePdfTextFailure', { type: 'static' })
      confirmResponsible()
      cy.wait('@approveResponsible', { responseTimeout: 120000 }).then(({ response }) => {
        expectPdfTextProblem(
          response?.statusCode,
          response?.body,
          'pdf-osio-koejakson-koulutussopimus',
          SOURCE
        )
      })
      expectPdfTextToast('Koejakson koulutussopimus')
      expectPdfTextStateUnchanged()
      cy.apiRequest({ method: 'GET', url: `${RESPONSIBLE_API}/${id}` }).then(({ body }) => {
        expect(body.vastuuhenkilo.sopimusHyvaksytty).to.eq(false)
        expect(body.vastuuhenkilo.kuittausaika).to.be.null
      })
      cy.task('db:setLegacyPdfText', {
        email: E2E_ERIKOISTUVA_EMAIL,
        id,
        target: 'contract-place',
        text: 'Jyväskylän sairaala'
      })
      dismissPdfTextToast()
      // Reload to discard the stale DTO containing the legacy text before retrying.
      cy.reload()
      cy.wait('@responsibleContract').its('response.statusCode').should('eq', 200)
      fillResponsible()
      confirmResponsible()
      cy.wait('@approveResponsible', { responseTimeout: 120000 }).then(({ response }) => {
        expect(response?.statusCode).to.eq(200)
        expect(response?.body.vastuuhenkilo.sopimusHyvaksytty).to.eq(true)
      })
      cy.apiRequest({ method: 'GET', url: `${RESPONSIBLE_API}/${id}` }).then(({ body }) => {
        expect(body.vastuuhenkilo.sopimusHyvaksytty).to.eq(true)
        expect(body.koulutuspaikat[0].nimi).to.eq('Jyväskylän sairaala')
      })
    })
  })
})
