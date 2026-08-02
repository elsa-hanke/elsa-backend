import { E2E_ERIKOISTUVA_EMAIL } from '../../support/commands'
import { KOULUTTAJA_EMAIL, VASTUUHENKILO_EMAIL, VIRKAILIJA_EMAIL } from '../../support/commands/credentials'

const KOULUTTAJA_ETUNIMI = 'Lassekalevi'
const KOULUTTAJA_SUKUNIMI = 'Hummaamistes'
const VASTUUHENKILO_ETUNIMI = 'Mia'
const VASTUUHENKILO_SUKUNIMI = 'Ålands'

describe('Kouluttajan seuranta', () => {
  before(() => {
    Cypress.session.clearAllSavedSessions()
    cy.task('db:cleanupKoejakso', { erikoistuvaEmail: E2E_ERIKOISTUVA_EMAIL })
    cy.task('db:cleanupErikoistuva', { email: E2E_ERIKOISTUVA_EMAIL })
    cy.task('db:cleanupKouluttaja', { email: KOULUTTAJA_EMAIL })
    cy.task('db:cleanupVastuuhenkilo', { email: VASTUUHENKILO_EMAIL })
    cy.task('db:cleanupVirkailija', { email: VIRKAILIJA_EMAIL })

    cy.loginAsErikoistuva()

    cy.task('db:seedKouluttaja', {
      email: KOULUTTAJA_EMAIL,
      etunimi: KOULUTTAJA_ETUNIMI,
      sukunimi: KOULUTTAJA_SUKUNIMI,
    }).then((result: any) => {
      Cypress.env('kouluttajaToken', result?.token)
    })

    cy.task('db:seedVastuuhenkilo', {
      email: VASTUUHENKILO_EMAIL,
      etunimi: VASTUUHENKILO_ETUNIMI,
      sukunimi: VASTUUHENKILO_SUKUNIMI,
    })

    cy.task('db:seedKouluttajavaltuutus', {
      erikoistuvaEmail: E2E_ERIKOISTUVA_EMAIL,
      kouluttajaEmail: KOULUTTAJA_EMAIL,
    })
  })

  it('Kouluttaja tarkistaa seurantasivun', () => {
    cy.loginAsKouluttaja(Cypress.env('kouluttajaToken'))
    cy.visit('/etusivu')
    cy.get('.mt-5').should('be.visible')
    cy.get('.btn > div').should('be.visible').click()

    // Tarkistetaan, että palaa omaan profiiliin -painike on näkyvissä.
    cy.get('.text-white > .btn').should('be.visible').click()
  })
})
