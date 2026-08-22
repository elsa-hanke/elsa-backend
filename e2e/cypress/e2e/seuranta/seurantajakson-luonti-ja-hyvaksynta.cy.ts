import {
  KOULUTTAJA_EMAIL,
  VASTUUHENKILO_EMAIL,
} from '../../support/commands/credentials'

const KOULUTTAJA_NIMI = 'Lassekalevi Hummaamistes'

describe('Seurantajakson luonti', () => {
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
  })

  it('estää PDF-fontista puuttuvien merkkien tallentamisen', () => {
    cy.intercept(
      'GET',
      '**/erikoistuva-laakari/seurantakeskustelut/seurantajaksontiedot**'
    ).as('getInvalidSeurantajaksonTiedot')
    cy.intercept(
      'POST',
      '**/erikoistuva-laakari/seurantakeskustelut/seurantajakso'
    ).as('postInvalidSeurantajakso')

    cy.visit('/seurantakeskustelut/seurantajakso/uusi')
    cy.contains('label', 'Seurantajakso alkaa')
      .parent()
      .find('input.date-input')
      .clear()
      .type('01.01.2025')
      .blur()
    cy.contains('label', 'Seurantajakso päättyy')
      .parent()
      .find('input.date-input')
      .clear()
      .type('30.06.2025')
      .blur()
    cy.contains('button', 'Hae tiedot').click()
    cy.wait('@getInvalidSeurantajaksonTiedot')
      .its('response.statusCode')
      .should('eq', 200)

    cy.contains('label', 'Oma arviointi seurantajaksolta')
      .parent()
      .find('textarea')
      .type('Erikoistuminen etenee suunnitellusti ✓')
    cy.contains('label', 'Kouluttaja').parent().find('.multiselect').click()
    cy.get('.multiselect--active .multiselect__option')
      .contains(KOULUTTAJA_NIMI)
      .click({ force: true })

    cy.contains('button', 'Tallenna ja lähetä').click()
    cy.get('#confirm-modal').find('button').contains('Tallenna ja lähetä').click()

    cy.wait('@postInvalidSeurantajakso').then(({ response }) => {
      expect(response?.statusCode).to.eq(400)
      expect(response?.body).to.include({
        message: 'error.dataillegal.pdf-tiedostossa-tukemattomia-merkkeja',
        field: 'oma-arviointi-seurantajaksolta',
      })
      expect(response?.body?.unsupportedCharacters).to.deep.equal(['✓ (U+2713)'])
    })
    cy.contains(
      'Kenttä "Oma arviointi seurantajaksolta" sisältää merkkejä, joita ei voida ' +
        'lisätä arkistoitavaan PDF-tiedostoon: ✓ (U+2713). Poista tai korvaa merkit ' +
        'ja tallenna uudelleen.'
    ).should('be.visible')
    cy.location('pathname').should('eq', '/seurantakeskustelut/seurantajakso/uusi')
  })

  it('erikoistuja luo seurantajakson ja tiedot säilyvät sivun uudelleenlatauksessa', () => {
    cy.intercept(
      'GET',
      '**/erikoistuva-laakari/seurantakeskustelut/seurantajaksontiedot**'
    ).as('getSeurantajaksonTiedot')
    cy.intercept(
      'POST',
      '**/erikoistuva-laakari/seurantakeskustelut/seurantajakso'
    ).as('postSeurantajakso')

    cy.visit('/seurantakeskustelut/seurantajakso/uusi')
    cy.contains('h1', 'Lisää seurantajakso').should('be.visible')

    cy.contains('label', 'Seurantajakso alkaa')
      .parent()
      .find('input.date-input')
      .clear()
      .type('01.01.2025')
      .blur()
    cy.contains('label', 'Seurantajakso päättyy')
      .parent()
      .find('input.date-input')
      .clear()
      .type('30.06.2025')
      .blur()
    cy.contains('button', 'Hae tiedot').click()
    cy.wait('@getSeurantajaksonTiedot').its('response.statusCode').should('eq', 200)

    cy.contains('label', 'Oma arviointi seurantajaksolta')
      .parent()
      .find('textarea')
      .type('E2E oma arviointi seurantajaksolta.')
    cy.contains('label', 'Kouluttaja').parent().find('.multiselect').click()
    cy.get('.multiselect--active .multiselect__option')
      .contains(KOULUTTAJA_NIMI)
      .click({ force: true })

    cy.contains('button', 'Tallenna ja lähetä').click()
    cy.get('#confirm-modal').find('button').contains('Tallenna ja lähetä').click()

    cy.wait('@postSeurantajakso').then(({ request, response }) => {
      expect(request.body).to.include({
        alkamispaiva: '2025-01-01',
        paattymispaiva: '2025-06-30',
        omaArviointi: 'E2E oma arviointi seurantajaksolta.',
      })
      expect(Number(request.body.kouluttaja.id)).to.eq(Number(Cypress.env('kouluttajaId')))
      expect(response?.statusCode).to.eq(201)
      expect(response?.body?.id).to.be.a('number')
      Cypress.env('seurantajaksoId', response?.body?.id)
    })

    cy.url().should('match', /\/seurantakeskustelut\/seurantajakso\/\d+$/)
    cy.contains('E2E oma arviointi seurantajaksolta.').should('be.visible')
    cy.contains(KOULUTTAJA_NIMI).should('be.visible')

    cy.reload()
    cy.contains('E2E oma arviointi seurantajaksolta.').should('be.visible')
    cy.contains(KOULUTTAJA_NIMI).should('be.visible')

    cy.then(() => {
      const seurantajaksoId = Number(Cypress.env('seurantajaksoId'))

      cy.apiRequest({
        method: 'GET',
        url: `/api/erikoistuva-laakari/seurantakeskustelut/seurantajakso/${seurantajaksoId}`,
      }).then(({ status, body }) => {
        expect(status).to.eq(200)
        expect(body.tila).to.eq('ODOTTAA_ARVIOINTIA_JA_YHTEISIA_MERKINTOJA')
      })

      cy.loginAsKouluttaja(Cypress.env('kouluttajaToken'))
      cy.apiRequest({
        method: 'GET',
        url: `/api/kouluttaja/seurantakeskustelut/seurantajakso/${seurantajaksoId}`,
      }).then(({ body }) => {
        cy.apiRequest({
          method: 'PUT',
          url: `/api/kouluttaja/seurantakeskustelut/seurantajakso/${body.id}`,
          body: {
            ...body,
            edistyminenTavoitteidenMukaista: true,
            kouluttajanArvio: 'E2E kouluttajan arvio seurantajaksosta.',
          },
        }).its('status').should('eq', 200)
      })

      cy.loginAsErikoistuva()
      cy.apiRequest({
        method: 'GET',
        url: `/api/erikoistuva-laakari/seurantakeskustelut/seurantajakso/${seurantajaksoId}`,
      }).then(({ body }) => {
        expect(body.tila).to.eq('ODOTTAA_YHTEISIA_MERKINTOJA')
        cy.apiRequest({
          method: 'PUT',
          url: `/api/erikoistuva-laakari/seurantakeskustelut/seurantajakso/${body.id}`,
          body: {
            ...body,
            seurantakeskustelunYhteisetMerkinnat: 'E2E hyväksyttävät yhteiset merkinnät.',
          },
        }).its('status').should('eq', 200)
      })

      cy.loginAsKouluttaja(Cypress.env('kouluttajaToken'))
      cy.apiRequest({
        method: 'GET',
        url: `/api/kouluttaja/seurantakeskustelut/seurantajakso/${seurantajaksoId}`,
      }).then(({ body }) => {
        expect(body.tila).to.eq('ODOTTAA_HYVAKSYNTAA')
        cy.apiRequest({
          method: 'PUT',
          url: `/api/kouluttaja/seurantakeskustelut/seurantajakso/${body.id}`,
          body,
        }).then(({ status, body: approvedBody }) => {
          expect(status).to.eq(200)
          expect(approvedBody.hyvaksytty).to.eq(true)
        })
      })

      cy.visit(`/seurantakeskustelut/seurantajakso/${seurantajaksoId}`)
      cy.contains('Seurantajakso on arvioitu ja yhteiset merkinnät hyväksytty.').should(
        'be.visible'
      )
      cy.contains('Muokkaa arviointia').should('not.exist')
    })
  })
})
