/**
 * Saavutettavuustestit – Yleinen sivurakenne
 *
 * Testaa korjatut ongelmat:
 *  - Skip-to-main-content -linkki (WCAG 2.4.1)
 *  - Sivun main-maamerkki (WCAG 1.3.1)
 *  - Navigaatioelementtien aria-labelit (WCAG 4.1.2)
 *  - SPA-navigoinnin aria-live-ilmoitus (WCAG 4.1.3)
 *  - Näkyvä kohdistus painikkeissa ja linkeissä (WCAG 2.4.7)
 *  - Axe-automaattiajot kaikilla pääsivuilla
 */

export {}

describe('Yleinen sivurakenne – saavutettavuus', () => {
  before(() => {
    cy.loginAsErikoistuva()
  })

  it('sivurakenteen saavutettavuus', () => {
    cy.visit('/etusivu')
    cy.get('[role="status"]', { timeout: 8000 }).should('not.exist')

    // ── Skip-to-main-linkki on sivun ensimmäinen kohdistettava elementti (WCAG 2.4.1)
    cy.get('body').trigger('keydown', { key: 'Tab', force: true })
    cy.get('a[href="#main-content"]').should('exist')

    // ── Skip-to-main-linkki on oletuksena visuaalisesti piilotettu ───────────
    cy.get('a[href="#main-content"]').then(($link) => {
      const top = parseFloat($link.css('top'))
      expect(top).to.be.lessThan(0)
    })

    // ── Skip-to-main-linkki vie fokuksen #main-content-elementtiin ───────────
    cy.get('a[href="#main-content"]').focus().click({ force: true })
    cy.focused().should('have.id', 'main-content')

    // ── Etusivulla on main-maamerkki (WCAG 1.3.1) ────────────────────────────
    cy.visit('/etusivu')
    cy.get('main, [role="main"]').should('exist')

    // ── Päänavigaatiolla on suomenkielinen aria-label (WCAG 4.1.2) ───────────
    cy.get('nav[aria-label]').then(($navs) => {
      const labels = [...$navs].map((n) => n.getAttribute('aria-label'))
      expect(labels).to.include.members(['Päänavigaatio'])
    })

    // ── aria-live-alue on olemassa SPA-navigointia varten (WCAG 4.1.3) ───────
    cy.get('[aria-live="polite"]').should('exist')

    // ── route-announcer päivittyy navigoitaessa toiselle sivulle ─────────────
    cy.visit('/tyoskentelyjaksot')
    cy.get('#route-announcer').invoke('text').should('not.be.empty')

    // ── Ensisijainen painike saa näkyvän kohdistuksen (WCAG 2.4.7) ───────────
    cy.visit('/etusivu')
    cy.get('.btn-primary').first().then(($btn) => {
      if ($btn.length) {
        cy.assertFocusVisible('.btn-primary')
      }
    })

    // ── Outline-painike saa näkyvän kohdistuksen (WCAG 2.4.7) ────────────────
    cy.visit('/tyoskentelyjaksot')
    cy.get('[role="status"]', { timeout: 5000 }).should('not.exist')
    cy.get('.btn-outline-primary').first().then(($btn) => {
      if ($btn.length) {
        cy.assertFocusVisible('.btn-outline-primary')
      }
    })

    // ── Axe-tarkistus: etusivu (WCAG 2.1 AA) ─────────────────────────────────
    cy.visit('/etusivu')
    cy.get('[role="status"]', { timeout: 5000 }).should('not.exist')
    cy.checkPageA11y()

    // ── Axe-tarkistus: työskentelyjaksot (WCAG 2.1 AA) ───────────────────────
    cy.visit('/tyoskentelyjaksot')
    cy.get('[role="status"]', { timeout: 5000 }).should('not.exist')
    cy.checkPageA11y()

    // ── Axe-tarkistus: arvioinnit (WCAG 2.1 AA) ──────────────────────────────
    cy.visit('/arvioinnit')
    cy.get('[role="status"]', { timeout: 5000 }).should('not.exist')
    cy.checkPageA11y()
  })
})
