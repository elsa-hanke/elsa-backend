/**
 * Saavutettavuustestit – Haitarielementti (Accordion)
 *
 * Testaa korjatut ongelmat (s. 43 PDF-raportissa):
 *  - role="button" + aria-expanded (WCAG 1.3.1, 4.1.2)
 *  - Näkyvä kohdistus haitarin otsikolla (WCAG 2.4.7)
 *  - Näppäimistöaktivointi (WCAG 2.1.1)
 *  - Tyhjät haitarit saavat tabindex="-1" (WCAG 2.4.3)
 *  - Axe-tarkistus haitarielementeillä
 */

export {}

describe('Haitarielementti (Accordion) – saavutettavuus', () => {
  before(() => {
    cy.loginAsErikoistuva()
  })

  it('haitarielementin saavutettavuus', () => {
    cy.visit('/koulutussuunnitelma')
    cy.get('[role="status"]', { timeout: 10000 }).should('not.exist')
    cy.get('.card-header', { timeout: 8000 }).should('exist')

    // ── role="button" ohjelmallinen rakenne (WCAG 1.3.1) ─────────────────────
    // Kaikilla haitareilla on role="button" riippumatta sisällöstä
    cy.get('.accordian-header').first().should('have.attr', 'role', 'button')

    // ── Tyhjät haitarit eivät saa turhaa fokusta (WCAG 2.4.3) ────────────────
    // Tyhjillä (empty) haitareilla on tabindex="-1" jotta Tab ei vie niihin
    cy.get('.accordian-header.empty').first().should('have.attr', 'tabindex', '-1')

    // ── Näkyvä kohdistus (WCAG 2.4.7) ────────────────────────────────────────
    // Haitarin otsikko voidaan fokusoida ohjelmallisesti (myös tabindex="-1")
    cy.get('.accordian-header').first().focus().then(($el) => {
      const outline = $el.css('outline-style')
      const outlineWidth = parseInt($el.css('outline-width') || '0', 10)
      expect(outline !== 'none' && outlineWidth > 0).to.be.true
    })

    // ── aria-expanded ja vuorovaikutustestit (WCAG 4.1.2, 2.1.1) ─────────────
    // Testataan vain jos sivulla on sisältöä sisältäviä haitareita
    cy.get('body').then(($body) => {
      const nonEmpty = $body.find('.accordian-header:not(.empty)')
      if (nonEmpty.length === 0) {
        cy.log('Ei sisältöä sisältäviä haitareita – interaktiotarkistukset ohitetaan')
        return
      }

      // aria-expanded-attribuutti on olemassa ei-tyhjällä haitarilla
      cy.get('.accordian-header:not(.empty)').first().then(($header) => {
        const expanded = $header.attr('aria-expanded')
        expect(['true', 'false']).to.include(expanded, 'aria-expanded pitäisi olla "true" tai "false"')
      })

      // aria-expanded muuttuu haitaria avattaessa
      cy.get('.accordian-header:not(.empty)').first().as('header')
      cy.get('@header').invoke('attr', 'aria-expanded').then((initial) => {
        cy.get('@header').click()
        cy.get('@header').invoke('attr', 'aria-expanded').should('not.equal', initial)
      })

      // Haitarin avaaminen Enter-näppäimellä
      cy.get('.accordian-header:not(.empty)').first().as('enterHeader')
      cy.get('@enterHeader').invoke('attr', 'aria-expanded').then((before) => {
        cy.get('@enterHeader').focus().type('{enter}')
        cy.get('@enterHeader').invoke('attr', 'aria-expanded').should('not.equal', before)
      })

      // Haitarin avaaminen välilyönnillä
      cy.get('.accordian-header:not(.empty)').first().as('spaceHeader')
      cy.get('@spaceHeader').invoke('attr', 'aria-expanded').then((before) => {
        cy.get('@spaceHeader').focus().trigger('keyup', { key: ' ', code: 'Space' })
        cy.get('@spaceHeader').invoke('attr', 'aria-expanded').should('not.equal', before)
      })
    })

    // ── Axe-tarkistus haitarialueella (WCAG 2.1 AA) ──────────────────────────
    cy.checkPageA11y('.card')
  })
})
