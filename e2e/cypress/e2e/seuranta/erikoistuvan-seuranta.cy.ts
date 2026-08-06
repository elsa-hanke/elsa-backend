import {KOULUTTAJA_EMAIL, VASTUUHENKILO_EMAIL, VIRKAILIJA_EMAIL} from "../../support/commands/credentials";

const KOULUTTAJA_ETUNIMI = 'Lassekalevi'
const KOULUTTAJA_SUKUNIMI = 'Hummaamistes'
const VASTUUHENKILO_ETUNIMI = 'Mia'
const VASTUUHENKILO_SUKUNIMI = 'Ålands'
const VIRKAILIJA_ETUNIMI = 'Daniel'
const VIRKAILIJA_SUKUNIMI = 'Siekkinen'


describe('Erikoistuvan laakarin seuranta', () => {

  before(() => {
    cy.prepareKoejaksoE2e({
      cleanupSupportUsers: true,
      seedVirkailija: true,
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
      virkailija: {
        email: VIRKAILIJA_EMAIL,
        etunimi: VIRKAILIJA_ETUNIMI,
        sukunimi: VIRKAILIJA_SUKUNIMI,
      },
    })

  })

  it('Erikoistuvan laakarin seuranta', () => {
    cy.loginAsErikoistuva()
    cy.luoTyoskentelyjakso()

  })

})

