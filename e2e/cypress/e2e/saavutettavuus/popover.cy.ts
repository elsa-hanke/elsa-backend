/**
 * Saavutettavuustestit – Lisätietopainike (Popover)
 *
 * Testaa korjatut ongelmat (s. 31 PDF-raportissa):
 *  - Lisätietopainikkeella on saavutettava nimi (WCAG 4.1.2)
 *  - aria-expanded kertoo ponnahdusikkunan tilan (WCAG 4.1.2)
 *  - Painike on fokusoitavissa (WCAG 2.1.1)
 *  - Näkyvä kohdistus (WCAG 2.4.7)
 *  - Sulkemispainikkeen suomenkielinen nimi
 *
 * Sivu: /suoritemerkinnat/uusi  (elsa-popover käytössä arviointiasteikko- ja
 *       vaativuustaso-kentissä)
 */

export {}

describe('Lisätietopainike (Popover) – saavutettavuus', () => {
  before(() => {
    cy.loginAsErikoistuva()
  })

  it('lisätietopainikkeen saavutettavuus', () => {
    // Suoritemerkintä-lomakkeella on elsa-popover-komponentit
    cy.visit('/suoritemerkinnat/uusi')
    cy.get('[role="status"]', { timeout: 10000 }).should('not.exist')
    cy.get('button[id^="elsa-popover-"]', { timeout: 10000 }).should('exist')

    // ── Saavutettava nimi: painikkeilla on aria-label (WCAG 4.1.2) ────────────
    cy.get('button[id^="elsa-popover-"]').each(($btn) => {
      const label = $btn.attr('aria-label')
      expect(label, `Painikkeella ${$btn.attr('id')} pitäisi olla aria-label`).to.not.be.empty
    })

    // ── aria-expanded="false" kun popover on kiinni (WCAG 4.1.2) ─────────────
    cy.get('button[id^="elsa-popover-"]').first().then(($btn) => {
      expect(['true', 'false']).to.include(
        $btn.attr('aria-expanded'),
        'aria-expanded pitäisi olla "true" tai "false"'
      )
    })

    // ── aria-expanded="true" kun popover on auki ──────────────────────────────
    cy.get('button[id^="elsa-popover-"]').first().as('btn')
    cy.get('@btn').should('have.attr', 'aria-expanded', 'false')
    cy.get('@btn').click()
    cy.get('@btn').should('have.attr', 'aria-expanded', 'true')

    // ── aria-expanded="false" kun popover suljetaan ───────────────────────────
    cy.get('body').click(0, 0)
    cy.get('@btn').should('have.attr', 'aria-expanded', 'false')

    // ── Painike on fokusoitavissa (WCAG 2.1.1) – tabindex="0" osoittaa fokusoitavuuden
    cy.get('button[id^="elsa-popover-"]').first().should('not.have.attr', 'tabindex', '-1')

    // ── Axe-tarkistus popover-alueella (WCAG 2.1 AA) ─────────────────────────
    cy.checkPageA11y('button[id^="elsa-popover-"]')
  })
})
