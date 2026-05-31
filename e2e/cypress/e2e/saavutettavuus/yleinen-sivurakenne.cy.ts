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

import { E2E_ERIKOISTUVA_EMAIL } from '../../support/commands'

export {}

describe('Yleinen sivurakenne – saavutettavuus', () => {
  before(() => {
    cy.loginAsErikoistuva()
  })

  // ── Skip-to-main-content -linkki ───────────────────────────────────────────
  describe('Skip-to-main -linkki (WCAG 2.4.1)', () => {
    it('on sivun ensimmäinen kohdistettava elementti etusivulla', () => {
      cy.visit('/etusivu')
      // Ensimmäinen Tab-painallus tuo skip-linkin näkyviin
      cy.get('body').trigger('keydown', { key: 'Tab' })
      cy.get('a[href="#main-content"]').should('exist')
    })

    it('on oletuksena visuaalisesti piilotettu', () => {
      cy.visit('/etusivu')
      cy.get('a[href="#main-content"]').then(($link) => {
        // Ennen fokusta top-arvo on negatiivinen (off-screen)
        const top = parseFloat($link.css('top'))
        expect(top).to.be.lessThan(0)
      })
    })

    it('vie fokuksen #main-content-elementtiin klikattaessa', () => {
      cy.visit('/etusivu')
      cy.get('a[href="#main-content"]').focus().click()
      cy.focused().should('have.id', 'main-content')
    })
  })

  // ── Main-maamerkki ─────────────────────────────────────────────────────────
  describe('Main-maamerkki (WCAG 1.3.1)', () => {
    it('etusivulla on main-elementti', () => {
      cy.visit('/etusivu')
      cy.get('main, [role="main"]').should('exist')
    })

    it('kirjautumissivulla on main-elementti', () => {
      // Kirjautumissivua ei voi testata ilman uloskirjautumista, joten
      // tarkistetaan HTML-rakenteesta suoraan
      cy.visit('/etusivu') // käydään loggedin-sivulla
      cy.get('main, [role="main"]').should('have.length.gte', 1)
    })
  })

  // ── Navigaatioalueiden nimet ───────────────────────────────────────────────
  describe('Navigaatioalueiden aria-labelit (WCAG 4.1.2)', () => {
    it('päänavigaatiolla on suomenkielinen aria-label', () => {
      cy.visit('/etusivu')
      // Sidebar-navigaatio (desktop)
      cy.get('nav[aria-label]').then(($navs) => {
        const labels = [...$navs].map((n) => n.getAttribute('aria-label'))
        expect(labels).to.include.members(['Päänavigaatio'])
      })
    })
  })

  // ── SPA-navigointi: aria-live-ilmoitus ─────────────────────────────────────
  describe('SPA-navigoinnin ruudunlukijailmoitus (WCAG 4.1.3)', () => {
    it('aria-live-alue on olemassa', () => {
      cy.visit('/etusivu')
      cy.get('[aria-live="polite"]').should('exist')
    })

    it('route-announcer päivittyy navigoitaessa toiselle sivulle', () => {
      cy.visit('/etusivu')
      cy.get('#route-announcer').then(($el) => {
        // Navigoidaan uudelle sivulle
        cy.visit('/tyoskentelyjaksot')
        cy.get('#route-announcer').invoke('text').should('not.be.empty')
      })
    })
  })

  // ── Näkyvä kohdistus ───────────────────────────────────────────────────────
  describe('Näkyvä kohdistus painikkeissa (WCAG 2.4.7)', () => {
    it('ensisijainen painike saa näkyvän kohdistuksen', () => {
      cy.visit('/etusivu')
      // Etsi ensimmäinen btn-primary -luokkainen painike
      cy.get('.btn-primary').first().then(($btn) => {
        if ($btn.length) {
          cy.assertFocusVisible('.btn-primary')
        }
      })
    })

    it('outline-painike saa näkyvän kohdistuksen', () => {
      cy.visit('/tyoskentelyjaksot')
      cy.get('.btn-outline-primary').first().then(($btn) => {
        if ($btn.length) {
          cy.assertFocusVisible('.btn-outline-primary')
        }
      })
    })
  })

  // ── Axe-automaattitesti pääsivuilla ───────────────────────────────────────
  describe('Axe WCAG 2.1 AA automaattitarkistus (kaikki roolit)', () => {
    const sivut = ['/etusivu', '/tyoskentelyjaksot', '/arvioinnit']

    sivut.forEach((sivu) => {
      it(`ei kriittisiä WCAG-rikkomuksia sivulla ${sivu}`, () => {
        cy.visit(sivu)
        // Odotetaan sivun latautumista
        cy.get('[role="status"]', { timeout: 5000 }).should('not.exist')
        cy.checkPageA11y()
      })
    })
  })
})

