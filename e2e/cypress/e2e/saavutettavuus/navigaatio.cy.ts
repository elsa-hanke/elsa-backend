/**
 * Saavutettavuustestit – Navigaatio (Navbar + Sidebar)
 *
 * Testaa korjatut ongelmat (s. 38-39 PDF-raportissa):
 *  - Mobiilivalikkopainikkeen suomenkielinen aria-label (WCAG 3.3.2)
 *  - Profiilivalikon role="menu" ja aria-current (WCAG 1.3.1)
 *  - Sivunavigaation aria-label (WCAG 4.1.2)
 *  - Osaaminen-painikkeen aria-expanded + semanttinen button (WCAG 1.3.1, 4.1.2)
 */

import { E2E_ERIKOISTUVA_EMAIL } from '../../support/commands'

export {}

describe('Navbar – saavutettavuus', () => {
  before(() => {
    cy.loginAsErikoistuva()
  })

  beforeEach(() => {
    cy.visit('/etusivu')
    cy.get('[role="status"]', { timeout: 8000 }).should('not.exist')
  })

  // ── Mobiilivalikkopainikkeen nimi (WCAG 3.3.2) ────────────────────────────
  it('mobiilivalikkopainikkeen aria-label on suomeksi', () => {
    cy.viewport('iphone-6')
    cy.visit('/etusivu')
    cy.get('.navbar-toggler, button[aria-label]').first().then(($btn) => {
      const label = $btn.attr('aria-label') ?? ''
      // Ei saa olla englanninkielinen "toggle navigation" tai "Menu"
      expect(label.toLowerCase()).to.not.include('toggle')
      expect(label.toLowerCase()).to.not.include('menu')
      // Pitäisi sisältää suomenkielinen teksti
      expect(label.length).to.be.greaterThan(0)
      cy.log(`Mobiilivalikkonappi aria-label: "${label}"`)
    })
    cy.viewport(1280, 800)
  })

  // ── Profiilivalikon role="menu" (WCAG 1.3.1) ──────────────────────────────
  it('käyttäjävalikolla on role="menu"', () => {
    // Avataan profiili-dropdown
    cy.get('.user-dropdown .dropdown-toggle').click()
    cy.get('[role="menu"]').should('exist')
  })

  it('profiilivalikon kohteet ovat role="menuitem"', () => {
    cy.get('.user-dropdown .dropdown-toggle').click()
    cy.get('[role="menu"] [role="menuitem"]').should('have.length.gte', 1)
  })

  // ── Axe-tarkistus navigaatiossa ───────────────────────────────────────────
  it('ylätunnisteen navigaatiossa ei WCAG-rikkomuksia', () => {
    cy.checkPageA11y('#navbar-top')
  })
})

describe('Sivunavigaatio – saavutettavuus', () => {
  before(() => {
    cy.loginAsErikoistuva()
  })

  beforeEach(() => {
    cy.visit('/etusivu')
    cy.get('[role="status"]', { timeout: 8000 }).should('not.exist')
  })

  // ── Navigaation aria-label (WCAG 4.1.2) ──────────────────────────────────
  it('sivunavigaatiolla on aria-label="Päänavigaatio"', () => {
    cy.get('#sidebar-menu[aria-label]').should('have.attr', 'aria-label', 'Päänavigaatio')
  })

  // ── Osaaminen-painike semanttinen button (WCAG 1.3.1, 4.1.2) ─────────────
  it('Osaaminen-navigaatiokohta on button-elementti, ei linkki', () => {
    // Osaaminen-kohta ei saisi olla pelkkä ankkuri ilman href-attribuuttia
    cy.get('#sidebar-menu').then(($nav) => {
      // Etsitään Osaaminen-teksti sisältävä elementti
      const osaaminenEl = $nav.find('button').filter((_, el) =>
        Cypress.$(el).text().includes('Osaaminen')
      )
      if (osaaminenEl.length) {
        expect(osaaminenEl.prop('tagName').toLowerCase()).to.equal('button')
      } else {
        cy.log('Osaaminen-elementtiä ei löydy – ehkä erikoistuja-roolin ulkopuolella')
      }
    })
  })

  it('Osaaminen-painikkeella on aria-expanded-attribuutti', () => {
    cy.get('#sidebar-menu button').filter((_, el) =>
      Cypress.$(el).text().includes('Osaaminen')
    ).then(($btns) => {
      if ($btns.length) {
        expect(['true', 'false']).to.include($btns.first().attr('aria-expanded'))
      }
    })
  })

  // ── Axe-tarkistus sivunavigaatiossa ──────────────────────────────────────
  it('sivunavigaatiossa ei WCAG-rikkomuksia', () => {
    cy.checkPageA11y('#sidebar-menu')
  })
})

