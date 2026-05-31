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
    Cypress.session.clearAllSavedSessions()
    cy.task('db:cleanupErikoistuva', { email: E2E_ERIKOISTUVA_EMAIL })
    cy.task('db:seedKouluttaja', {
      email: KOULUTTAJA_EMAIL,
      etunimi: KOULUTTAJA_ETUNIMI,
      sukunimi: KOULUTTAJA_SUKUNIMI,
    })
    cy.loginAsErikoistuva()
    cy.task('db:seedTyoskentelyjakso', { email: E2E_ERIKOISTUVA_EMAIL })
  })

  beforeEach(() => {
    cy.visit('/arvioinnit/arviointipyynto')
    cy.get('.arviointipyynto', { timeout: 10000 }).should('be.visible')
    cy.get('[role="status"]', { timeout: 10000 }).should('not.exist')
  })

  // ── Axe-tarkistus koko sivulla ────────────────────────────────────────────
  it('sivulla ei WCAG 2.1 AA -rikkomuksia (axe)', () => {
    cy.checkPageA11y()
  })

  // ── Datepicker: kalenteripainike fokusoitavissa ───────────────────────────
  it('tapahtuman ajankohta -kentän kalenteripainike ei ole tabindex=-1', () => {
    cy.get('button[id^="datepicker-"]').each(($btn) => {
      expect($btn.attr('tabindex')).to.not.equal('-1')
    })
  })

  // ── Datepicker: kalenteripainikkeen aria-label ────────────────────────────
  it('kalenteripainikkeella on suomenkielinen aria-label tai sr-only-teksti', () => {
    cy.get('button[id^="datepicker-"]').first().then(($btn) => {
      const ariaLabel = $btn.attr('aria-label') ?? ''
      const srText = $btn.find('.sr-only').text() ?? ''
      const combined = ariaLabel + srText
      expect(combined.toLowerCase()).to.match(/kalenteri/)
    })
  })

  // ── Popover: lisätietopainikkeiden nimet ──────────────────────────────────
  it('lisätietopainikkeet eivät ole nimettömiä', () => {
    cy.get('button[id^="elsa-popover-"]').each(($btn) => {
      const label = $btn.attr('aria-label')
      expect(label, `Painikkeella ${$btn.attr('id')} pitäisi olla aria-label`).to.not.be.empty
      expect(label).to.not.equal('')
    })
  })

  // ── Multiselect: tyhjennyspainikkeen nimi ─────────────────────────────────
  it('multiselect-tyhjennyspainikkeen aria-label ei ole tyhjä', () => {
    // Valitaan ensin arvo aktivoidaksemme clear-napin
    cy.contains('label', 'Työskentelyjakso').parent().find('.multiselect').click()
    cy.get('.multiselect--active .multiselect__option:not(.multiselect__option--group):not(.multiselect__option--disabled)')
      .first().click()

    // Tyhjennyspainike ilmestyy
    cy.get('.clear-button[aria-label]').should('exist')
    cy.get('.clear-button').first().then(($btn) => {
      const label = $btn.attr('aria-label')
      expect(label).to.not.be.empty
    })
  })

  // ── Liitetiedosto: painike fokusoitavissa ─────────────────────────────────
  it('liitetiedoston lisäyspainike on fokusoitavissa (tabindex="0")', () => {
    cy.get('label[tabindex="0"][role="button"]').should('have.length.gte', 1)
  })

  // ── Lomakevirhe: role="alert" ─────────────────────────────────────────────
  it('lomakkeen virheilmoituksella on role="alert"', () => {
    // Lähetetään lomake tyhjänä
    cy.contains('button', 'Lähetä').click()
    cy.get('[role="alert"]', { timeout: 5000 }).should('exist')
  })
})

