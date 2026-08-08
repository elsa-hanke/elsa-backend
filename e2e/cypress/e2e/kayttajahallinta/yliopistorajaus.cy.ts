describe('Käyttäjähallinnan yliopistorajaus', () => {
  before(() => {
    cy.prepareVirkailijaE2e()
    cy.then(() => cy.loginAsVirkailija(Cypress.env('virkailijaToken')))
  })

  it('virkailija ei voi luoda virkailijaa oman yliopistonsa ulkopuolelle', () => {
    const rejectedEmail = 'e2e-rajattu-virkailija@test.elsa'

    cy.apiRequest({
      method: 'POST',
      url: '/api/virkailija/virkailijat',
      failOnStatusCode: false,
      body: {
        etunimi: 'E2E',
        sukunimi: 'Rajattu virkailija',
        sahkoposti: rejectedEmail,
        sahkopostiUudelleen: rejectedEmail,
        eppn: 'e2e-rajattu-virkailija',
        yliopisto: { id: 1000 },
      },
    }).then(({ status }) => {
      expect(status).to.eq(400)
    })

    cy.apiRequest({
      method: 'GET',
      url: '/api/virkailija/virkailijat',
      qs: {
        sort: 'id,asc',
        'nimi.contains': 'Rajattu virkailija',
      },
    }).then(({ status, body }) => {
      expect(status).to.eq(200)
      expect(
        body.content.some((kayttaja: any) => kayttaja.sahkoposti === rejectedEmail)
      ).to.eq(false)
    })
  })
})
