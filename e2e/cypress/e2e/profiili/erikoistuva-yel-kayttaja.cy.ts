import { E2E_ERIKOISTUVA_EMAIL } from '../../support/commands'
import { OpintoOikeus } from '../../plugins/db-tasks/opintooikeus'


describe('Katseluoikeudet', () => {
  const opintoOikeus: OpintoOikeus = {
    asetus_id: 5,
    erikoisala_id: 50,
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
    yliopisto_opintooikeus_id: "1234",
    tila: "AKTIIVINEN",
    viimeinen_katselupaiva: "2026-11-05",
    yliopisto_id: 5,
    id: 507
  }

  before(() => {
    Cypress.session.clearAllSavedSessions()
    cy.task('db:cleanupErikoistuva', { email: E2E_ERIKOISTUVA_EMAIL })
    cy.task('db:cleanupOpintooikeus', {email: E2E_ERIKOISTUVA_EMAIL})
  })

  context('Yel kayttaja, joka valmistunut erikoistuvana', () => {
    it('erikoistuva-yel-kayttaja', () => {
      cy.loginAsErikoistuva()
      cy.task('db:seedOpintooikeus', { email: E2E_ERIKOISTUVA_EMAIL, opintoOikeus })
      cy.task('db:seedOpintooikeus', { email: E2E_ERIKOISTUVA_EMAIL, opintoOikeus: opintoOikeusAktiivinen })
      cy.visit('/profiili')
    })
  })
})
