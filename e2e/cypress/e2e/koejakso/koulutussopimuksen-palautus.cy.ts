import {
  KOULUTTAJA_EMAIL,
  VASTUUHENKILO_EMAIL,
} from '../../support/commands/credentials'

const KOULUTTAJA_NIMI = 'Lassekalevi Hummaamistes'
const KORJAUSEHDOTUS = 'Tarkenna koulutuspaikan nimi.'
const KORJATTU_KOULUTUSPAIKKA = 'E2E Korjattu testisairaala'

describe('Koulutussopimuksen palautus', () => {
  before(() => {
    cy.prepareKoejaksoE2e({
      cleanupSupportUsers: true,
      storeTokens: true,
      kouluttaja: {
        email: KOULUTTAJA_EMAIL,
        etunimi: 'Lassekalevi',
        sukunimi: 'Hummaamistes',
      },
      vastuuhenkilo: {
        email: VASTUUHENKILO_EMAIL,
        etunimi: 'Mia',
        sukunimi: 'Ålands',
      },
    })
    cy.submitKoulutussopimusViaUi(KOULUTTAJA_NIMI).then((id) => {
      Cypress.env('koulutussopimusId', id)
    })
  })

  it('kouluttaja palauttaa koulutussopimuksen ja erikoistuja lähettää korjatut tiedot uudelleen', () => {
    cy.loginAsKouluttaja(Cypress.env('kouluttajaToken'))
    cy.apiRequest({
      method: 'GET',
      url: `/api/kouluttaja/koejakso/koulutussopimus/${Cypress.env('koulutussopimusId')}`,
    }).then(({ status, body }) => {
      expect(status).to.eq(200)
      cy.apiRequest({
        method: 'PUT',
        url: '/api/kouluttaja/koejakso/koulutussopimus',
        body: {
          ...body,
          korjausehdotus: KORJAUSEHDOTUS,
          kouluttajat: (body.kouluttajat ?? []).map((kouluttaja: any) => ({
            ...kouluttaja,
            sahkoposti: KOULUTTAJA_EMAIL,
          })),
        },
      }).then(({ status: returnStatus, body: returnedBody }) => {
        expect(returnStatus).to.eq(200)
        expect(returnedBody.korjausehdotus).to.eq(KORJAUSEHDOTUS)
        expect(returnedBody.lahetetty).to.eq(false)
      })
    })

    cy.loginAsErikoistuva()
    cy.intercept('PUT', '**/erikoistuva-laakari/koejakso/koulutussopimus').as(
      'resubmitKoulutussopimus'
    )
    cy.visit('/koejakso/koulutussopimus')
    cy.contains(
      'Koejakson koulutussopimus on palautettu takaisin muokattavaksi kouluttajan toimesta.'
    ).should('be.visible')
    cy.contains(KORJAUSEHDOTUS).should('be.visible')

    cy.contains('label', 'Toimipaikan nimi')
      .parent()
      .find('input[type="text"]')
      .first()
      .clear()
      .type(KORJATTU_KOULUTUSPAIKKA)
    cy.contains('button', 'Hyväksy ja lähetä').click()
    cy.get('#confirm-send').find('button').contains('Hyväksy ja lähetä').click()

    cy.wait('@resubmitKoulutussopimus').then(({ request, response }) => {
      expect(request.body.koulutuspaikat[0].nimi).to.eq(KORJATTU_KOULUTUSPAIKKA)
      expect(response?.statusCode).to.eq(200)
      expect(response?.body?.korjausehdotus).to.be.null
      expect(response?.body?.lahetetty).to.eq(true)
    })
    cy.contains(
      'Koejakson koulutussopimus on lähetetty kouluttajan ja vastuuhenkilön hyväksyttäväksi.'
    ).should('be.visible')
    cy.contains(KORJATTU_KOULUTUSPAIKKA).should('be.visible')
  })
})
