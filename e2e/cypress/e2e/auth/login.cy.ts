describe('Authentication', () => {
  context('Suomi.fi login', () => {
    it('logs in successfully via Suomi.fi test identity provider', () => {
      cy.loginWithSuomifi('test@example.com')

      cy.url().should('not.include', '/kirjautuminen')

      cy.get('main[role="main"]').should('exist')
      cy.get('.etusivu').should('exist')

      cy.contains('Erikoistumisen edistyminen').should('be.visible')
    })
  })
})
