/**
 * Saavutettavuustestit – Datepicker (kalenterivalitsin)
 *
 * Testaa korjatut ongelmat (s. 32 PDF-raportissa):
 *  - Kalenteria voi avata näppäimistöllä (WCAG 2.1.1) – tabindex=-1 poistettu
 *  - Kalenteripainikkeella on kuvaava nimi (WCAG 2.4.6) – aria-label lisätty
 *  - Kalenterin sisäisillä painikkeilla on näkyvä kohdistus (WCAG 2.4.7)
 *  - Axe-tarkistus datepicker-komponentissa
 */

import { E2E_ERIKOISTUVA_EMAIL } from '../../support/commands'

export {}

describe('Datepicker (kalenterivalitsin) – saavutettavuus', () => {
  before(() => {
    cy.loginAsErikoistuva()
  })

  // Työskentelyjakson lisäämislomake sisältää päivämääräkenttiä
  beforeEach(() => {
    cy.visit('/tyoskentelyjaksot/uusi')
    cy.get('[role="status"]', { timeout: 10000 }).should('not.exist')
  })

  // ── Kalenteripainikkeen fokusoitavuus (WCAG 2.1.1) ────────────────────────
  it('kalenteripainike on fokusoitavissa näppäimistöllä (ei tabindex=-1)', () => {
    // b-form-datepicker renderöi painikkeen id="datepicker-{id}"
    cy.get('button[id^="datepicker-"]').first().then(($btn) => {
      expect($btn.attr('tabindex')).to.not.equal('-1')
    })
  })

  it('kalenteripainike aktivoituu Enter-näppäimellä', () => {
    cy.get('button[id^="datepicker-"]').first().focus().type('{enter}')
    // Kalenterin pitäisi avautua – b-calendar tulee näkyviin
    cy.get('.b-calendar, [role="dialog"]').should('be.visible')
  })

  // ── Kalenteripainikkeen saavutettava nimi (WCAG 2.4.6) ────────────────────
  it('kalenteripainikkeella on suomenkielinen aria-label', () => {
    cy.get('button[id^="datepicker-"]').first().then(($btn) => {
      const label = $btn.attr('aria-label') ?? $btn.find('.sr-only').text()
      expect(label).to.match(/kalenteri/i, 'Painikkeen nimi ei sisällä sanaa "kalenteri"')
    })
  })

  // ── Kalenterin sisäinen näkyvä kohdistus (WCAG 2.4.7) ────────────────────
  it('kalenterin navigointipainikkeet saavat näkyvän kohdistuksen', () => {
    // Avataan kalenteri
    cy.get('button[id^="datepicker-"]').first().click()
    cy.get('.b-calendar').should('be.visible')
    // Tarkistetaan seuraava/edellinen kuukausi -painikkeiden focustyylit
    cy.get('.b-calendar button').first().then(($btn) => {
      cy.assertFocusVisible('.b-calendar button')
    })
  })

  // ── Axe-tarkistus sivulla (WCAG 2.1 AA) ──────────────────────────────────
  it('työskentelyjakson lisäämissivulla ei WCAG-rikkomuksia datepicker-alueella', () => {
    cy.checkPageA11y('.date-input-group')
  })
})

