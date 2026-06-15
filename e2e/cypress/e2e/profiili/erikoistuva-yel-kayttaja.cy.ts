import { E2E_ERIKOISTUVA_EMAIL } from '../../support/commands'
import { OpintoOikeus } from '../../plugins/db-tasks/opintooikeus'
import {SSN_ERIKOISTUVA} from "../../support/commands/credentials";


describe('Katseluoikeudet', () => {
  const opintoOikeus: OpintoOikeus = {
    asetus_id: 5,
    erikoisala_id: 50,
    erikoistuva_laakari_id: 0,
    kaytossa: false,
    muokkausaika: "2021-01-04",
    muokkausoikeudet_virkailijoilla: true,
    myontamispaiva: "2021-01-04",
    opintoopas_id: 17,
    opiskelijatunnus: "",
    osaamisen_arvioinnin_oppaan_pvm: "2022-09-07",
    paattymispaiva: "2026-05-05",
    terveyskeskuskoulutusjakso_suoritettu: false,
    yliopisto_opintooikeus_id: "1234",
    tila: "VALMISTUNUT",
    viimeinen_katselupaiva: "2026-11-05",
    yliopisto_id: 5,
    id: 506
  }

  const opintoOikeusAktiivinen: OpintoOikeus = {
    asetus_id: 5,
    erikoisala_id: 61,
    erikoistuva_laakari_id: 0,
    kaytossa: true,
    muokkausaika: "2021-01-04",
    muokkausoikeudet_virkailijoilla: true,
    myontamispaiva: "2021-01-04",
    opintoopas_id: 17,
    opiskelijatunnus: "",
    osaamisen_arvioinnin_oppaan_pvm: "2022-09-07",
    paattymispaiva: "2026-05-05",
    terveyskeskuskoulutusjakso_suoritettu: false,
    yliopisto_opintooikeus_id: "4321",
    tila: "AKTIIVINEN",
    viimeinen_katselupaiva: "2026-11-05",
    yliopisto_id: 5,
    id: 507
  }


  before(() => {
    cy.log('BEFORE!!!')
    Cypress.session.clearAllSavedSessions()
    cy.task('db:cleanupErikoistuva', { email: E2E_ERIKOISTUVA_EMAIL })
    cy.task('db:cleanupOpintooikeus', {email: E2E_ERIKOISTUVA_EMAIL})
  })

  context('Yel kayttaja, joka valmistunut erikoistuvana', () => {
    it('erikoistuva-yel-kayttaja', () => {
      cy.loginAsErikoistuva()
      cy.task('db:seedOpintooikeus', { email: E2E_ERIKOISTUVA_EMAIL, opintoOikeus })
      cy.task('db:seedOpintooikeus', { email: E2E_ERIKOISTUVA_EMAIL, opintoOikeus: opintoOikeusAktiivinen })

      cy.log('kirjaudutaan ulos')
      // Kirjaudutaan ulos, että käyttöoikeudet / roolit päivittyvät
      cy.getCookie('XSRF-TOKEN').then((cookie) => {
        cy.request({
          method: 'POST',
          url: '/api/logout',
          headers: { 'X-XSRF-TOKEN': cookie?.value ?? '' },
        })
      })


      cy.log('kirjaudutaan takaisin sisälle')
      // Kirjaudutaan takaiisn sisälle
      cy.visit('/')
      cy.visit('/kirjautuminen')

      cy.get(':nth-child(3) > .btn').click()

      // Suomi.fi muistaa kirjautumisen -> joten  mennään eteenpäin...
      cy.origin('https://testi.apro.tunnistus.fi', () => {

        cy.get('body').then(($body) => {
          const exists = $body.find('[name="_eventId_proceed"]').length > 0
          if (exists) {
            cy.get('[name="_eventId_proceed"]').click()
          }
          else {
            cy.get('#continue-button').click()
          }
        })
      })

      // Tarkistetaan valikosta, että siellä näkyy YEK ja erikoistuva roolit
      cy.get('#__BVID__26').click()
      cy.get(':nth-child(5) > :nth-child(3) > .dropdown-item > .d-flex > :nth-child(2)').should('exist')
      cy.get(':nth-child(5) > :nth-child(4) > .dropdown-item > .d-flex > :nth-child(2)').should('exist')
    })

  })
})
