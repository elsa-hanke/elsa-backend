import { announcementContent, assertNoExecution, assertSafeContent, assertSafeHtml, dirtyHtml, observeXss } from '../../support/rich-text'

describe('Julkisen ilmoituksen tallennettu XSS', () => {
  afterEach(() => cy.task('db:cleanupRichTextAnnouncements'))

  it('puhdistaa vanhan tietueen julkisessa API:ssa ja selaimessa muuttamatta tietokantaa', () => {
    cy.task<number>('db:createLegacyRichTextAnnouncement', dirtyHtml).then((id) => {
      cy.intercept('GET', '**/api/julkinen/ilmoitukset').as('announcements')
      cy.visit('/kirjautuminen', { onBeforeLoad: observeXss })
      cy.wait('@announcements').then(({ response }) => {
        expect(response?.statusCode).to.eq(200)
        const html = response?.body.find((item: { id: number }) => item.id === id)?.teksti
        expect(html).to.be.a('string')
        // Assert the real server response, independently of DOMPurify in the page.
        assertSafeHtml(html)
      })
      announcementContent().should('be.visible').should(($root) => assertSafeContent($root))
      assertNoExecution()
      cy.request(`/api/julkinen/ilmoitukset/${id}`).its('body.teksti').should('contain', '<h2>')
      cy.task('db:readRichTextAnnouncement', id).should('eq', dirtyHtml)
    })
  })

  it('torjuu myös puhdistamattoman API-vastauksen frontendissä', () => {
    cy.intercept('GET', '**/api/julkinen/ilmoitukset', [{ id: 987654321, teksti: dirtyHtml }]).as('dirtyResponse')
    cy.visit('/kirjautuminen', { onBeforeLoad: observeXss })
    cy.wait('@dirtyResponse').its('response.body.0.teksti').should('eq', dirtyHtml)
    announcementContent().should('be.visible').should(($root) => assertSafeContent($root))
    // Clicking the stripped encoded javascript: link must also be harmless.
    announcementContent().contains('a', 'safe').click()
    assertNoExecution()
    cy.location('pathname').should('eq', '/kirjautuminen')
  })
})
