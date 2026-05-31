/**
 * Saavutettavuustestit – Haitarielementti (Accordion)
 *
 * Testaa korjatut ongelmat (s. 43 PDF-raportissa):
 *  - role="button" + aria-expanded (WCAG 1.3.1, 4.1.2)
 *  - Näkyvä kohdistus haitarin otsikolla (WCAG 2.4.7)
 *  - Tyhjät haitarit eivät saa turhaa fokusta (WCAG 2.4.3)
 *  - Axe-tarkistus haitarielementeillä
 */

import { E2E_ERIKOISTUVA_EMAIL } from '../../support/commands'

export {}

describe('Haitarielementti (Accordion) – saavutettavuus', () => {
  before(() => {
    cy.loginAsErikoistuva()
  })

  beforeEach(() => {
    cy.visit('/koulutussuunnitelma')
    cy.get('[role="status"]', { timeout: 10000 }).should('not.exist')
    cy.get('.card-header', { timeout: 8000 }).should('exist')
  })

  // ── role="button" ohjelmallinen rakenne (WCAG 1.3.1) ─────────────────────
  it('haitarin otsikolla on role="button", ei role="tab"', () => {
    cy.get('.accordian-header').first().should(($el) => {
      const role = $el.attr('role')
      expect(role).to.equal('button', 'Haitarin otsikolla pitäisi olla role="button"')
      expect(role).to.not.equal('tab', 'Haitarin otsikolla EI pitäisi olla role="tab"')
    })
  })

  // ── aria-expanded tila (WCAG 4.1.2) ──────────────────────────────────────
  it('haitarilla on aria-expanded-attribuutti', () => {
    cy.get('.accordian-header[tabindex="0"]').first().then(($header) => {
      const expanded = $header.attr('aria-expanded')
      expect(['true', 'false']).to.include(expanded, 'aria-expanded pitäisi olla "true" tai "false"')
    })
  })

  it('aria-expanded muuttuu haitaria avattaessa ja suljettaessa', () => {
    cy.get('.accordian-header[tabindex="0"]').first().as('header')

    // Tallennetaan alkutila
    cy.get('@header').invoke('attr', 'aria-expanded').then((initial) => {
      // Klikataan otsikkoa
      cy.get('@header').click()
      // Tila pitäisi muuttua
      cy.get('@header').invoke('attr', 'aria-expanded').should('not.equal', initial)
    })
  })

  // ── Näkyvä kohdistus (WCAG 2.4.7) ────────────────────────────────────────
  it('haitarin otsikko saa näkyvän kohdistuksen', () => {
    cy.get('.accordian-header[tabindex="0"]').first().focus().then(($el) => {
      const outline = $el.css('outline-style')
      const outlineWidth = parseInt($el.css('outline-width') || '0', 10)
      expect(outline !== 'none' && outlineWidth > 0).to.be.true
    })
  })

  // ── Näppäimistöaktivointi (WCAG 2.1.1) ───────────────────────────────────
  it('haitarin voi avata Enter-näppäimellä', () => {
    cy.get('.accordian-header[tabindex="0"]').first().as('header')
    cy.get('@header').invoke('attr', 'aria-expanded').then((before) => {
      cy.get('@header').focus().type('{enter}')
      cy.get('@header').invoke('attr', 'aria-expanded').should('not.equal', before)
    })
  })

  it('haitarin voi avata välilyönnillä', () => {
    cy.get('.accordian-header[tabindex="0"]').first().as('header')
    cy.get('@header').invoke('attr', 'aria-expanded').then((before) => {
      cy.get('@header').focus().trigger('keyup', { key: ' ', code: 'Space' })
      cy.get('@header').invoke('attr', 'aria-expanded').should('not.equal', before)
    })
  })

  // ── Axe-tarkistus (WCAG 2.1 AA) ──────────────────────────────────────────
  it('koulutussuunnitelmasivulla ei WCAG-rikkomuksia haitarialueella', () => {
    cy.checkPageA11y('.card')
  })
})

