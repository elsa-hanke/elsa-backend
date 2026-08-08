const UUSI_VIRKAILIJA_EMAIL = 'e2e-uusi-virkailija@test.elsa'

describe('Virkailijan luonti', () => {
  before(() => {
    cy.prepareVirkailijaE2e({ cleanupEmails: [UUSI_VIRKAILIJA_EMAIL] })
    cy.then(() => cy.loginAsVirkailija(Cypress.env('virkailijaToken')))
  })

  after(() => {
    cy.task('db:cleanupVirkailija', { email: UUSI_VIRKAILIJA_EMAIL })
  })

  it('virkailija luo oman yliopistonsa virkailijan ja tiedot säilyvät uudelleenlatauksessa', () => {
    cy.intercept('GET', '**/virkailija/yliopistot').as('getYliopistot')
    cy.intercept('POST', '**/virkailija/virkailijat').as('postVirkailija')

    cy.visit('/kayttajahallinta/kayttaja/uusi')
    cy.contains('h1', 'Lisää uusi käyttäjä').should('be.visible')
    cy.contains('label', 'Virkailija').click()
    cy.wait('@getYliopistot').its('response.statusCode').should('eq', 200)

    cy.contains('label', 'Etunimi').parent().find('input').type('E2E')
    cy.contains('label', 'Sukunimi').parent().find('input').type('Uusi virkailija')
    cy.contains('label', 'Sähköpostiosoite').parent().find('input').type(UUSI_VIRKAILIJA_EMAIL)
    cy.contains('label', 'Sähköpostiosoite uudelleen')
      .parent()
      .find('input')
      .type(UUSI_VIRKAILIJA_EMAIL)
    cy.contains('label', 'Yliopiston käyttäjätunnus')
      .parent()
      .find('input')
      .type('e2e-uusi-virkailija')

    cy.contains('button', 'Tallenna').click()
    cy.wait('@postVirkailija').then(({ request, response }) => {
      expect(request.body).to.include({
        etunimi: 'E2E',
        sukunimi: 'Uusi virkailija',
        sahkoposti: UUSI_VIRKAILIJA_EMAIL,
        eppn: 'e2e-uusi-virkailija',
      })
      expect(request.body.yliopisto.id).to.be.a('number')
      expect(response?.statusCode).to.eq(201)
      expect(response?.body?.kayttaja?.id).to.be.a('number')
    })

    cy.url().should('match', /\/kayttajahallinta\/virkailijat\/\d+$/)
    cy.contains('E2E').should('be.visible')
    cy.contains('Uusi virkailija').should('be.visible')
    cy.contains(UUSI_VIRKAILIJA_EMAIL).should('be.visible')
    cy.contains('e2e-uusi-virkailija').should('be.visible')

    cy.reload()
    cy.contains('Uusi virkailija').should('be.visible')
    cy.contains(UUSI_VIRKAILIJA_EMAIL).should('be.visible')
    cy.contains('e2e-uusi-virkailija').should('be.visible')
  })
})
