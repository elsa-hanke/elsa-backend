import {
  KOULUTTAJA_EMAIL,
  VASTUUHENKILO_EMAIL,
} from '../../support/commands/credentials'

const KORJAUSEHDOTUS = 'Tarkenna yhteisiä merkintöjä ennen hyväksyntää.'
const KORJATUT_MERKINNAT = 'E2E korjatut yhteiset merkinnät ja jatkosuunnitelma.'

describe('Seurantajakson palautus', () => {
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
    cy.then(() => {
      cy.createSeurantajaksoViaApi({
        kouluttajaId: Cypress.env('kouluttajaId'),
        yhteisetMerkinnat: 'E2E alkuperäiset yhteiset merkinnät.',
      }).then((seurantajakso) => {
        Cypress.env('seurantajaksoId', seurantajakso.id)
      })
    })
  })

  it('kouluttaja palauttaa seurantajakson ja erikoistuja lähettää korjatut tiedot uudelleen', () => {
    cy.loginAsKouluttaja(Cypress.env('kouluttajaToken'))
    cy.visit(
      `/seurantakeskustelut/seurantajakso/${Cypress.env('seurantajaksoId')}/muokkaa`
    )
    cy.contains('h1', 'Seurantajakson yhteenveto').should('be.visible')

    cy.intercept(
      'PUT',
      '**/kouluttaja/seurantakeskustelut/seurantajakso/**'
    ).as('returnSeurantajakso')
    cy.contains('button', 'Palauta muokattavaksi').click()
    cy.get('#return-to-sender')
      .contains('label', 'Syy palautukseen')
      .parent()
      .find('textarea')
      .type(KORJAUSEHDOTUS)
    cy.get('#return-to-sender').find('button').contains('Palauta muokattavaksi').click()

    cy.wait('@returnSeurantajakso').then(({ request, response }) => {
      expect(request.body.korjausehdotus).to.eq(KORJAUSEHDOTUS)
      expect(response?.statusCode).to.eq(200)
    })
    cy.contains('Seurantajakso palautettu muokattavaksi').should('be.visible')

    cy.loginAsErikoistuva()
    cy.visit(`/seurantakeskustelut/seurantajakso/${Cypress.env('seurantajaksoId')}`)
    cy.contains(KORJAUSEHDOTUS).should('be.visible')
    cy.contains('a', 'Muokkaa tietoja').click()

    cy.contains('label', 'Yhteiset merkinnät keskustelusta ja jatkosuunnitelmista')
      .parent()
      .find('textarea')
      .clear()
      .type(KORJATUT_MERKINNAT)

    cy.intercept(
      'PUT',
      '**/erikoistuva-laakari/seurantakeskustelut/seurantajakso/**'
    ).as('resubmitSeurantajakso')
    cy.contains('button', 'Tallenna ja lähetä').click()
    cy.get('#confirm-modal').find('button').contains('Tallenna ja lähetä').click()

    cy.wait('@resubmitSeurantajakso').then(({ request, response }) => {
      expect(request.body.seurantakeskustelunYhteisetMerkinnat).to.eq(KORJATUT_MERKINNAT)
      expect(response?.statusCode).to.eq(200)
      expect(response?.body?.korjausehdotus).to.be.null
    })
    cy.contains(KORJATUT_MERKINNAT).should('be.visible')

    cy.apiRequest({
      method: 'GET',
      url: `/api/erikoistuva-laakari/seurantakeskustelut/seurantajakso/${Cypress.env('seurantajaksoId')}`,
    }).then(({ status, body }) => {
      expect(status).to.eq(200)
      expect(body.tila).to.eq('ODOTTAA_ARVIOINTIA')
      expect(body.korjausehdotus).to.be.null
    })
  })
})
