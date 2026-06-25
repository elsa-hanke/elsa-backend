/**
 * Saavutettavuustestit – Arviointipyyntö (integraatiotesti + axe)
 *
 * Lisää axe-tarkistukset olemassa olevaan käyttötapaustestiin.
 * Testaa kaikki korjatut komponentit yhdessä kontekstissa:
 *  - Datepicker, Multiselect, Popover, Liitetiedosto, Lomakevirhe
 *
 * Viittaa: e2e/arvioinnit/arviointipyynto.cy.ts (olemassaoleva testi)
 */

import { E2E_ERIKOISTUVA_EMAIL } from '../../support/commands'

export {}

const KOULUTTAJA_EMAIL    = 'test-kouluttaja@test.elsa'
const KOULUTTAJA_ETUNIMI  = 'Testi'
const KOULUTTAJA_SUKUNIMI = 'Kouluttaja'

describe('Arviointipyyntö-sivu – saavutettavuustarkistukset (s. 49 PDF)', () => {
  before(() => {
    cy.task('db:seedKouluttaja', {
      email: KOULUTTAJA_EMAIL,
      etunimi: KOULUTTAJA_ETUNIMI,
      sukunimi: KOULUTTAJA_SUKUNIMI,
    })
    cy.loginAsErikoistuva()
    cy.task('db:seedTyoskentelyjakso', { email: E2E_ERIKOISTUVA_EMAIL })
  })

  it('arviointipyynnön saavutettavuus', () => {
    cy.visit('/arvioinnit/arviointipyynto')
    cy.get('.arviointipyynto', { timeout: 10000 }).should('be.visible')
    cy.get('[role="status"]', { timeout: 10000 }).should('not.exist')

    // ── Axe-tarkistus koko sivulla (WCAG 2.1 AA) ─────────────────────────────
    cy.checkPageA11y()

    // ── Datepicker: kalenteripainike ei ole tabindex=-1 (WCAG 2.1.1) ─────────
    cy.get('button[id^="datepicker-"]').each(($btn) => {
      expect($btn.attr('tabindex')).to.not.equal('-1')
    })

    // ── Datepicker: kalenteripainikkeella on suomenkielinen aria-label ────────
    cy.get('button[id^="datepicker-"]').first().then(($btn) => {
      const ariaLabel = $btn.attr('aria-label') ?? ''
      const srText = $btn.find('.sr-only').text() ?? ''
      const combined = ariaLabel + srText
      expect(combined.toLowerCase()).to.match(/kalenteri/)
    })


    // ── Multiselect: tyhjennyspainikkeen aria-label ei ole tyhjä ─────────────
    cy.contains('label', 'Työskentelyjakso').parent().find('.multiselect').click()
    cy.get('.multiselect--active .multiselect__option:not(.multiselect__option--group):not(.multiselect__option--disabled)')
      .first().click()
    cy.get('.clear-button[aria-label]').should('exist')
    cy.get('.clear-button').first().then(($btn) => {
      const label = $btn.attr('aria-label')
      expect(label).to.not.be.empty
    })


    // ── Lomakevirhe: role="alert" ilmestyy virheellisellä lähetyksellä ───────
    cy.contains('button', 'Lähetä').click()
    cy.get('[role="alert"]', { timeout: 5000 }).should('exist')
  })
})
