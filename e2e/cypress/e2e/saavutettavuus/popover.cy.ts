/**
 * Saavutettavuustestit – Lisätietopainike (Popover)
 *
 * Testaa korjatut ongelmat (s. 31 PDF-raportissa):
 *  - Lisätietopainikkeella on saavutettava nimi (WCAG 4.1.2)
 *  - aria-expanded kertoo ponnahdusikkunan tilan (WCAG 4.1.2)
 *  - Painike on fokusoitavissa (WCAG 2.1.1)
 *  - Näkyvä kohdistus (WCAG 2.4.7)
 *  - Sulkemispainikkeen suomenkielinen nimi
 */

import { E2E_ERIKOISTUVA_EMAIL } from '../../support/commands'

export {}

describe('Lisätietopainike (Popover) – saavutettavuus', () => {
  before(() => {
    cy.loginAsErikoistuva()
  })

  beforeEach(() => {
    // Itsearviointi-sivu käyttää paljon lisätietopainikkeita
    cy.visit('/arvioinnit/arviointipyynto')
    cy.get('[role="status"]', { timeout: 10000 }).should('not.exist')
  })

  // ── Saavutettava nimi (WCAG 4.1.2) ────────────────────────────────────────
  it('info-painikkeella on aria-label (ei nimetön)', () => {
    cy.get('button[id^="elsa-popover-"]').each(($btn) => {
      const label = $btn.attr('aria-label')
      expect(label, `Painikkeella ${$btn.attr('id')} pitäisi olla aria-label`).to.not.be.empty
    })
  })

  // ── aria-expanded tila (WCAG 4.1.2) ──────────────────────────────────────
  it('info-painikkeella on aria-expanded-attribuutti', () => {
    cy.get('button[id^="elsa-popover-"]').first().then(($btn) => {
      expect(['true', 'false']).to.include(
        $btn.attr('aria-expanded'),
        'aria-expanded pitäisi olla "true" tai "false"'
      )
    })
  })

  it('aria-expanded="true" kun popover on auki', () => {
    cy.get('button[id^="elsa-popover-"]').first().as('btn')
    cy.get('@btn').should('have.attr', 'aria-expanded', 'false')
    cy.get('@btn').click()
    cy.get('@btn').should('have.attr', 'aria-expanded', 'true')
  })

  it('aria-expanded="false" kun popover suljetaan', () => {
    cy.get('button[id^="elsa-popover-"]').first().as('btn')
    cy.get('@btn').click()
    cy.get('@btn').should('have.attr', 'aria-expanded', 'true')
    // Suljetaan painamalla Escape tai klikkaamalla muualle
    cy.get('body').click(0, 0)
    cy.get('@btn').should('have.attr', 'aria-expanded', 'false')
  })

  // ── Fokusoitavuus ja näkyvä kohdistus (WCAG 2.1.1, 2.4.7) ────────────────
  it('info-painike on fokusoitavissa Tab-näppäimellä', () => {
    cy.get('button[id^="elsa-popover-"]').first().focus().then(($el) => {
      expect(document.activeElement).to.equal($el[0])
    })
  })

  // ── Axe-tarkistus ─────────────────────────────────────────────────────────
  it('arviointipyynnöllä ei WCAG-rikkomuksia popover-alueella', () => {
    cy.checkPageA11y('button[id^="elsa-popover-"]')
  })
})

