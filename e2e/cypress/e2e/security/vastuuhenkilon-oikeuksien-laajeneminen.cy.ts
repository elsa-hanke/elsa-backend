import {
  E2E_ERIKOISTUVA_EMAIL,
  VASTUUHENKILO_EMAIL,
} from '../../support/commands/credentials'

export {}

const VASTUUHENKILO_ETUNIMI = 'Mia'
const VASTUUHENKILO_SUKUNIMI = 'Ålands'
const VASTUUHENKILON_ERIKOISALA_ID = 1

describe('Vastuuhenkilön oikeuksien tahaton laajeneminen', () => {
  let alkuperaisetRajaimet: unknown
  let erikoistuvanOpintooikeusId: number
  let vastuuhenkiloToken: string

  before(() => {
    Cypress.session.clearAllSavedSessions()
    cy.task('db:cleanupErikoistuva', { email: E2E_ERIKOISTUVA_EMAIL })
    cy.task('db:cleanupVastuuhenkilo', { email: VASTUUHENKILO_EMAIL })

    cy.seedVastuuhenkiloUser(
      {
        email: VASTUUHENKILO_EMAIL,
        etunimi: VASTUUHENKILO_ETUNIMI,
        sukunimi: VASTUUHENKILO_SUKUNIMI,
        // Erikoistuvan oletuserikoisala on 46. Vastuuhenkilö kuuluu tarkoituksella
        // eri erikoisalalle, jotta hänen oikeuksiensa ei pidä laajentua kutsusta.
        erikoisalaId: VASTUUHENKILON_ERIKOISALA_ID,
      },
      'vastuuhenkiloToken'
    ).then((result) => {
      expect(result?.token, 'vastuuhenkilön kirjautumistunniste').to.be.a('string').and.not.be.empty
      vastuuhenkiloToken = result!.token!
      cy.loginAsVastuuhenkilo(vastuuhenkiloToken)
    })

    cy.apiRequest({
      method: 'GET',
      url: '/api/vastuuhenkilo/etusivu/erikoistujien-seuranta-rajaimet',
    }).then(({ status, body }) => {
      expect(status).to.eq(200)
      alkuperaisetRajaimet = body
    })

    cy.loginAsErikoistuva()
    cy.task('db:getActiveOpintooikeusId', { email: E2E_ERIKOISTUVA_EMAIL }).then((id) => {
      erikoistuvanOpintooikeusId = Number(id)
      expect(erikoistuvanOpintooikeusId).to.be.greaterThan(0)
    })

    // Sama operaatio, jonka arviointipyyntö-, keskustelu- ja katseluoikeuslomakkeet
    // tekevät, kun olemassa oleva vastuuhenkilö lisätään arvioijaksi.
    cy.apiRequest({
      method: 'POST',
      url: '/api/erikoistuva-laakari/lahikouluttajat',
      body: {
        etunimi: VASTUUHENKILO_ETUNIMI,
        sukunimi: VASTUUHENKILO_SUKUNIMI,
        sahkoposti: VASTUUHENKILO_EMAIL,
      },
    }).then(({ status }) => {
      expect(status).to.eq(200)
    })
  })

  after(() => {
    cy.task('db:cleanupVastuuhenkilo', { email: VASTUUHENKILO_EMAIL })
    cy.task('db:cleanupErikoistuva', { email: E2E_ERIKOISTUVA_EMAIL })
  })

  it('ei laajenna vastuuhenkilön erikoisaloja tai anna pääsyä erikoistujan tietoihin', () => {
    cy.loginAsVastuuhenkilo(vastuuhenkiloToken)

    let rajaimetKutsunJalkeen: unknown
    let impersonoinninStatus: number

    cy.apiRequest({
      method: 'GET',
      url: '/api/vastuuhenkilo/etusivu/erikoistujien-seuranta-rajaimet',
    }).then(({ status, body }) => {
      expect(status).to.eq(200)
      rajaimetKutsunJalkeen = body
    })

    cy.apiRequest({
      method: 'GET',
      url: `/api/login/impersonate?opintooikeusId=${erikoistuvanOpintooikeusId}`,
      failOnStatusCode: false,
      followRedirect: false,
    }).then(({ status }) => {
      impersonoinninStatus = status
    })

    cy.then(() => {
      expect(rajaimetKutsunJalkeen).to.deep.equal(alkuperaisetRajaimet)
      // Käyttäjä on kirjautunut sisään, mutta hänellä ei ole oikeutta
      // impersonoida eri erikoisalan erikoistujaa.
      expect(impersonoinninStatus).to.eq(403)
    })
  })
})
