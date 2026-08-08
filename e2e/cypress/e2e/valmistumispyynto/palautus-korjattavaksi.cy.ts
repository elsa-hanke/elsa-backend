import {
  E2E_ERIKOISTUVA_EMAIL,
  KOULUTTAJA_EMAIL,
  VASTUUHENKILO_EMAIL,
  VIRKAILIJA_EMAIL,
} from '../../support/commands/credentials'

const KORJAUSEHDOTUS = 'Täydennä valmistumispyynnön yhteystiedot.'

describe('Valmistumispyynnön palautus', () => {
  before(() => {
    cy.prepareKoejaksoE2e({
      cleanupSupportUsers: true,
      seedVirkailija: true,
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
      virkailija: {
        email: VIRKAILIJA_EMAIL,
        etunimi: 'Daniel',
        sukunimi: 'Siekkinen',
      },
    })
    cy.task('db:ensureLoppukeskusteluHyvaksytty', {
      erikoistuvaEmail: E2E_ERIKOISTUVA_EMAIL,
      kouluttajaEmail: KOULUTTAJA_EMAIL,
    })
    cy.submitValmistumispyyntoViaUi().then((id) => {
      Cypress.env('valmistumispyyntoId', id)
    })
  })

  it('vastuuhenkilö palauttaa valmistumispyynnön ja erikoistuja lähettää sen korjattuna uudelleen', () => {
    cy.loginAsVastuuhenkilo(Cypress.env('vastuuhenkiloToken'))
    cy.apiRequest({
      method: 'PUT',
      url: `/api/vastuuhenkilo/valmistumispyynnon-arviointi/${Cypress.env('valmistumispyyntoId')}`,
      body: {
        osaaminenRiittavaValmistumiseen: false,
        korjausehdotus: KORJAUSEHDOTUS,
      },
    }).then(({ status, body }) => {
      expect(status).to.eq(200)
      expect(body.tila).to.eq('VASTUUHENKILON_TARKASTUS_PALAUTETTU')
      expect(body.vastuuhenkiloOsaamisenArvioijaKorjausehdotus).to.eq(KORJAUSEHDOTUS)
    })

    cy.loginAsErikoistuva()
    cy.intercept('PUT', '**/erikoistuva-laakari/valmistumispyynto').as(
      'resubmitValmistumispyynto'
    )
    cy.visit('/valmistumispyynto')
    cy.contains(
      'Valmistumispyyntö on palautettu takaisin vastuuhenkilön toimesta, joka arvioi osaamisen.'
    ).should('be.visible')
    cy.contains(KORJAUSEHDOTUS).should('be.visible')

    cy.get('input[type="checkbox"]').each(($checkbox) => {
      cy.wrap($checkbox).check({ force: true })
    })
    cy.contains('button', 'Tee valmistumispyyntö').click()

    cy.contains('label', 'Matkapuhelinnumero')
      .parent()
      .find('input')
      .clear()
      .type('+358409876543')
      .should('have.value', '+358409876543')
    cy.contains('button', 'Lähetä pyyntö').click()
    cy.get('#confirm-send').find('button').contains('Lähetä pyyntö').click()

    cy.wait('@resubmitValmistumispyynto').then(({ response }) => {
      expect(response?.statusCode).to.eq(200)
      expect(response?.body?.tila).to.eq('ODOTTAA_VASTUUHENKILON_TARKASTUSTA')
    })
    cy.contains('Valmistumispyyntö lähetetty').should('be.visible')

    cy.apiRequest({
      method: 'GET',
      url: '/api/erikoistuva-laakari/valmistumispyynto',
    }).then(({ status, body }) => {
      expect(status).to.eq(200)
      expect(body.tila).to.eq('ODOTTAA_VASTUUHENKILON_TARKASTUSTA')
      expect(body.vastuuhenkiloOsaamisenArvioijaKorjausehdotus).to.be.null
    })
  })
})
