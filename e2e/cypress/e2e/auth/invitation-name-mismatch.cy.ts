const ERROR_PAGE = '/kirjautuminen?virhe=VIRHEELLINEN_NIMI'
const DETAILS_API = '**/api/julkinen/kirjautuminen/nimivirhe'

describe('Kutsun ja tunnistautumisen nimien ero', () => {
  beforeEach(() => {
    cy.intercept('GET', '**/api/julkinen/seuraava-paivitys', { body: '' })
    cy.intercept('GET', '**/api/julkinen/ilmoitukset', { body: [] })
  })

  it('näyttää kutsun ja tunnistautumisen nimet oikeiden otsikoiden alla', () => {
    cy.intercept('GET', DETAILS_API, {
      body: {
        invitationName: 'Aleksi Räisänen',
        authenticatedName: 'Kari Räisänen'
      }
    }).as('nameMismatch')

    cy.visit(ERROR_PAGE)
    cy.wait('@nameMismatch')

    cy.contains('.alert-danger', 'Kutsuun liitetty nimi ei vastaa kirjautumisessa saatua nimeä.')
      .should('be.visible')
    cy.get('[data-cy="login-name-mismatch"]').within(() => {
      cy.get('[data-cy="invitation-name"]')
        .should('have.text', 'Aleksi Räisänen')
        .and('be.visible')
        .prev('dt').should('have.text', 'Kutsussa oleva nimi:')
      cy.get('[data-cy="authenticated-name"]')
        .should('have.text', 'Kari Räisänen')
        .and('be.visible')
        .prev('dt').should('have.text', 'Tunnistautumisessa saatu nimi:')
    })
    cy.get('.alert-danger a[href="mailto:julia.sillanpaa@tuni.fi"]').should('be.visible')
    cy.location('search').should('eq', '?virhe=VIRHEELLINEN_NIMI')
  })

  for (const statusCode of [204, 500]) {
    it(`näyttää yhteydenotto-ohjeen ilman nimitietoja, kun vastaus on ${statusCode}`, () => {
      cy.intercept('GET', DETAILS_API, { statusCode, body: '' }).as('nameMismatch')

      cy.visit(ERROR_PAGE)
      cy.wait('@nameMismatch')

      cy.get('[data-cy="login-name-mismatch"]').should('not.exist')
      cy.contains('.alert-danger', 'Nimen muokkaamiseksi ota yhteys tekniseen pääkäyttäjään')
        .should('be.visible')
    })
  }

  it('ei hae eikä näytä nimitietoja muissa kirjautumisvirheissä', () => {
    const getDetails = cy.stub().as('getDetails')
    cy.intercept('GET', DETAILS_API, getDetails)

    cy.visit('/kirjautuminen?virhe=EI_KAYTTO_OIKEUTTA')

    cy.contains('.alert-danger', 'Käyttöoikeutta palveluun ei löytynyt.').should('be.visible')
    cy.get('[data-cy="login-name-mismatch"]').should('not.exist')
    cy.get('@getDetails').should('not.have.been.called')
  })
})
