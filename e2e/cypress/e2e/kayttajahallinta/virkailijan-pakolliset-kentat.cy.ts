describe('Virkailijan pakolliset kentät', () => {
  before(() => {
    cy.prepareVirkailijaE2e()
    cy.then(() => cy.loginAsVirkailija(Cypress.env('virkailijaToken')))
  })

  it('tyhjää virkailijalomaketta ei lähetetä ja pakolliset kentät osoitetaan', () => {
    let postRequests = 0
    cy.intercept('POST', '**/virkailija/virkailijat', () => {
      postRequests += 1
    })

    cy.visit('/kayttajahallinta/kayttaja/uusi')
    cy.contains('label', 'Virkailija').click()
    cy.contains('button', 'Tallenna').click()

    cy.contains('label', 'Etunimi')
      .parent()
      .find('.invalid-feedback')
      .should('be.visible')
      .and('contain', 'Pakollinen tieto')
    cy.contains('label', 'Sukunimi')
      .parent()
      .find('.invalid-feedback')
      .should('be.visible')
      .and('contain', 'Pakollinen tieto')
    cy.contains('label', 'Sähköpostiosoite')
      .parent()
      .find('.invalid-feedback')
      .should('be.visible')
    cy.contains('label', 'Sähköpostiosoite uudelleen')
      .parent()
      .find('.invalid-feedback')
      .should('be.visible')
    cy.contains('label', 'Yliopiston käyttäjätunnus')
      .parent()
      .find('.invalid-feedback')
      .should('be.visible')

    cy.url().should('include', '/kayttajahallinta/kayttaja/uusi')
    cy.wait(250)
    cy.then(() => expect(postRequests).to.eq(0))
  })
})
