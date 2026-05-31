/**
 * Saavutettavuustestit – Datepicker (kalenterivalitsin)
 *
 * Testaa korjatut ongelmat (s. 32 PDF-raportissa):
 *  - Kalenteria voi avata näppäimistöllä (WCAG 2.1.1) – tabindex=-1 poistettu
 *  - Kalenteripainikkeella on kuvaava nimi (WCAG 2.4.6) – aria-label lisätty
 *  - Axe-tarkistus datepicker-komponentissa
 */

export {}

describe('Datepicker (kalenterivalitsin) – saavutettavuus', () => {
  before(() => {
    cy.loginAsErikoistuva()
  })

  it('kalenterivalitsimen saavutettavuus', () => {
    cy.visit('/tyoskentelyjaksot/uusi')
    cy.get('[role="status"]', { timeout: 10000 }).should('not.exist')

    // ── Kalenteripainike on fokusoitavissa (ei tabindex=-1) (WCAG 2.1.1) ─────
    cy.get('button[id^="datepicker-"]').first().then(($btn) => {
      expect($btn.attr('tabindex')).to.not.equal('-1')
    })

    // ── Kalenteripainikkeella on suomenkielinen aria-label (WCAG 2.4.6) ──────
    cy.get('button[id^="datepicker-"]').first().then(($btn) => {
      const label = $btn.attr('aria-label') ?? $btn.find('.sr-only').text()
      expect(label).to.match(/kalenteri/i, 'Painikkeen nimi ei sisällä sanaa "kalenteri"')
    })

    // ── Axe-tarkistus datepicker-alueella (WCAG 2.1 AA) ──────────────────────
    cy.checkPageA11y('.date-input-group')
  })
})
