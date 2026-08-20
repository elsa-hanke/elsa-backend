import { KOULUTTAJA_EMAIL, VASTUUHENKILO_EMAIL } from '../../support/commands/credentials'

const KOULUTTAJA_ETUNIMI = 'Lassekalevi'
const KOULUTTAJA_SUKUNIMI = 'Hummaamistes'
const VASTUUHENKILO_ETUNIMI = 'Mia'
const VASTUUHENKILO_SUKUNIMI = 'Ålands'

describe('Kouluttajan seuranta', () => {
  before(() => {
    cy.prepareKoejaksoE2e({
      cleanupSupportUsers: true,
      storeTokens: true,
      kouluttaja: {
        email: KOULUTTAJA_EMAIL,
        etunimi: KOULUTTAJA_ETUNIMI,
        sukunimi: KOULUTTAJA_SUKUNIMI,
      },
      vastuuhenkilo: {
        email: VASTUUHENKILO_EMAIL,
        etunimi: VASTUUHENKILO_ETUNIMI,
        sukunimi: VASTUUHENKILO_SUKUNIMI,
      },
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
