import { E2E_ERIKOISTUVA_EMAIL } from '../../support/commands'
import { OpintoOikeus } from '../../plugins/db-tasks/opintooikeus'

export {}

describe('YEK työskentelyjakso', () => {
  const yekOpintooikeus: OpintoOikeus = {
    asetus_id: 5,
    erikoisala_id: 61,
    erikoistuva_laakari_id: 0,
    kaytossa: true,
    muokkausaika: '2021-01-04',
    muokkausoikeudet_virkailijoilla: true,
    myontamispaiva: '2021-01-04',
    opintoopas_id: 17,
    opiskelijatunnus: '',
    osaamisen_arvioinnin_oppaan_pvm: '2022-09-07',
    paattymispaiva: '2029-05-05',
    terveyskeskuskoulutusjakso_suoritettu: false,
    yliopisto_opintooikeus_id: 'e2e-yek-tyoskentelyjakso',
    tila: 'AKTIIVINEN',
    viimeinen_katselupaiva: '2029-11-05',
    yliopisto_id: 5,
    id: 610507
  }

  before(() => {
    Cypress.session.clearAllSavedSessions()
    cy.task('db:cleanupErikoistuva', { email: E2E_ERIKOISTUVA_EMAIL })
    cy.loginAsErikoistuva()
    cy.task('db:seedOpintooikeus', {
      email: E2E_ERIKOISTUVA_EMAIL,
      opintoOikeus: yekOpintooikeus,
      updateCurrent: true
    })
    cy.logout()
  })

  it('käyttäjä lisää YEK-työskentelyjakson ja poissaolon', () => {
    cy.visit('/kirjautuminen')
    cy.contains('Kirjaudu sisään (Suomi.fi)').click()

    cy.origin('https://testi.apro.tunnistus.fi', () => {
      cy.get('body').then(($body) => {
        const exists = $body.find('[name="_eventId_proceed"]').length > 0
        if (exists) {
          cy.get('[name="_eventId_proceed"]').click()
        } else {
          cy.get('#continue-button').click()
        }
      })
    })

    cy.location('origin', { timeout: 60000 }).should(
      'eq',
      new URL(Cypress.config('baseUrl') as string).origin
    )

    cy.request('/api/kayttaja').then(({ body }) => {
      if (body.activeAuthority === 'ROLE_YEK_KOULUTETTAVA') {
        return
      }

      cy.getCookie('XSRF-TOKEN').then((cookie) => {
        cy.request({
          method: 'POST',
          url: '/api/vaihda-rooli',
          form: true,
          body: { rooli: 'ROLE_YEK_KOULUTETTAVA' },
          headers: { 'X-XSRF-TOKEN': cookie?.value ?? '' }
        })
      })
    })

    cy.request('/api/kayttaja')
      .its('body.activeAuthority')
      .should('eq', 'ROLE_YEK_KOULUTETTAVA')

    cy.visit('/yektyoskentelyjaksot')
    cy.contains('h1', 'Työskentelyjaksot').should('be.visible')

    cy.visit('/yektyoskentelyjaksot/uusi')
    cy.get('.lisaa-tyoskentelyjakso').should('be.visible')
    cy.get('[role="status"]', { timeout: 10000 }).should('not.exist')

    cy.contains('label', 'Valviran laillistamispäivä')
      .parent()
      .find('input.date-input')
      .first()
      .clear()
      .type('01.01.2020')
      .blur()

    cy.get('input[type="file"]').first().selectFile('cypress/fixtures/test.pdf', { force: true })

    cy.get('input[type="radio"][name="laakarikoulutus"]').first().click({ force: true })

    cy.get('input[type="radio"][name="tyoskentelyjakso-tyyppi"]').first().click({ force: true })

    cy.contains('label', 'Työskentelypaikka')
      .parent()
      .find('input[type="text"]')
      .first()
      .clear()
      .type('E2E YEK Testisairaala')

    cy.contains('label', 'Kunta').parent().as('kuntaGroup')
    cy.selectFirstMultiselectOption(cy.get('@kuntaGroup'))

    cy.contains('label', 'Alkamispäivä')
      .parent()
      .find('input.date-input')
      .first()
      .clear()
      .type('01.02.2025')
      .blur()

    cy.contains('label', 'Päättymispäivä')
      .parent()
      .find('input.date-input')
      .first()
      .clear()
      .type('30.06.2027')
      .blur()

    cy.get('input[type="number"]').first().clear().type('100')

    cy.intercept('POST', '**/yek-koulutettava/tyoskentelyjaksot').as('yekTyoskentelyjaksoPost')
    cy.contains('button', 'Lisää').click()
    cy.wait('@yekTyoskentelyjaksoPost', { timeout: 15000 }).then(({ response }) => {
      expect(response?.statusCode).to.eq(201)
      expect(response?.body?.id).to.be.a('number')
      Cypress.env('yekTyoskentelyjaksoId', response?.body?.id)
    })

    cy.url().should('match', /\/yektyoskentelyjaksot\/\d+$/)
    cy.contains('E2E YEK Testisairaala').should('be.visible')

    cy.contains('Lisää poissaolo').click()
    cy.url().should('include', '/yektyoskentelyjaksot/poissaolot/uusi')
    cy.get('[role="status"]', { timeout: 10000 }).should('not.exist')

    cy.contains('label', 'Poissaolon syy').parent().as('poissaolonSyy')
    cy.selectFirstMultiselectOption(cy.get('@poissaolonSyy'))

    cy.contains('label', 'Alkamispäivä')
      .parent()
      .find('input.date-input')
      .first()
      .clear()
      .type('15.02.2025')
      .blur()

    cy.contains('label', 'Päättymispäivä')
      .parent()
      .find('input.date-input')
      .first()
      .clear()
      .type('28.02.2025')
      .blur()

    cy.intercept('POST', '**/yek-koulutettava/tyoskentelyjaksot/poissaolot').as('yekPoissaoloPost')
    cy.contains('button', 'Tallenna').click()
    cy.wait('@yekPoissaoloPost', { timeout: 15000 }).then(({ response }) => {
      expect(response?.statusCode).to.eq(201)
      expect(response?.body?.id).to.be.a('number')
    })

    cy.url().should('match', /\/yektyoskentelyjaksot\/poissaolot\/\d+$/)
    cy.contains(/15\.2\.2025|15\.02\.2025/i).should('be.visible')
    cy.contains(/28\.2\.2025|28\.02\.2025/i).should('be.visible')

    cy.visit('/yektyoskentelyjaksot')
    cy.contains('E2E YEK Testisairaala').should('be.visible')
  })
})

