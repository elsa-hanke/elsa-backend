function setupMocks() {
  cy.server()
  cy.route('/api/kayttaja', 'fixture:kayttaja.json')
  cy.route('/api/erikoistuva-laakari', 'fixture:erikoistuva-laakari.json')
}

describe('Etusivu', () => {
  it('Navigoidaan etusivulle', () => {
    setupMocks()
    cy.visit('/')
    cy.contains('.navbar-brand', 'ELSA')
  })
})
