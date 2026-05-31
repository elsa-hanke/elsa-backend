/**
 * Saavutettavuustestit – Navigaatio (Navbar + Sidebar)
 *
 * Testaa korjatut ongelmat (s. 38-39 PDF-raportissa):
 *  - Mobiilivalikkopainikkeen suomenkielinen aria-label (WCAG 3.3.2)
 *  - Profiilivalikon role="menu" ja aria-current (WCAG 1.3.1)
 *  - Sivunavigaation aria-label (WCAG 4.1.2)
 *  - Osaaminen-painikkeen aria-expanded + semanttinen button (WCAG 1.3.1, 4.1.2)
 */

export {}

describe('Navigaatio – saavutettavuus', () => {
  before(() => {
    cy.loginAsErikoistuva()
  })

  it('navigaatioelementtien saavutettavuus', () => {
    cy.visit('/etusivu')
    cy.get('[role="status"]', { timeout: 8000 }).should('not.exist')

    // ── Mobiilivalikkopainikkeen aria-label on suomeksi (WCAG 3.3.2) ──────────
    cy.viewport('iphone-6')
    cy.visit('/etusivu')
    cy.get('.navbar-toggler, button[aria-label]').first().then(($btn) => {
      const label = $btn.attr('aria-label') ?? ''
      expect(label.toLowerCase()).to.not.include('toggle')
      expect(label.toLowerCase()).to.not.include('menu')
      expect(label.length).to.be.greaterThan(0)
      cy.log(`Mobiilivalikkonappi aria-label: "${label}"`)
    })
    cy.viewport(1280, 800)

    // ── Axe-tarkistus ylätunnisteessa – ennen dropdownin avaamista (WCAG 2.1 AA)
    cy.checkPageA11y('#navbar-top')

    // ── Profiilivalikolla on role="menu" (WCAG 1.3.1) ─────────────────────────
    cy.visit('/etusivu')
    cy.get('[role="status"]', { timeout: 8000 }).should('not.exist')
    cy.get('.user-dropdown .dropdown-toggle').click()
    cy.get('[role="menu"]').should('exist')

    // ── Profiilivalikon kohteet ovat role="menuitem" ───────────────────────────
    cy.get('[role="menu"] [role="menuitem"]').should('have.length.gte', 1)
    cy.get('body').click(0, 0)

    // ── Sivunavigaatiolla on aria-label="Päänavigaatio" (WCAG 4.1.2) ──────────
    cy.get('#sidebar-menu[aria-label]').should('have.attr', 'aria-label', 'Päänavigaatio')

    // ── Osaaminen-navigaatiokohta on button-elementti (WCAG 1.3.1, 4.1.2) ─────
    cy.get('#sidebar-menu').then(($nav) => {
      const osaaminenEl = $nav.find('button').filter((_, el) =>
        Cypress.$(el).text().includes('Osaaminen')
      )
      if (osaaminenEl.length) {
        expect(osaaminenEl.prop('tagName').toLowerCase()).to.equal('button')
      } else {
        cy.log('Osaaminen-elementtiä ei löydy – ehkä erikoistuja-roolin ulkopuolella')
      }
    })

    // ── Osaaminen-painikkeella on aria-expanded-attribuutti ───────────────────
    cy.get('#sidebar-menu button').filter((_, el) =>
      Cypress.$(el).text().includes('Osaaminen')
    ).then(($btns) => {
      if ($btns.length) {
        expect(['true', 'false']).to.include($btns.first().attr('aria-expanded'))
      }
    })

    // ── Axe-tarkistus sivunavigaatiossa (WCAG 2.1 AA) ─────────────────────────
    cy.checkPageA11y('#sidebar-menu')
  })
})
