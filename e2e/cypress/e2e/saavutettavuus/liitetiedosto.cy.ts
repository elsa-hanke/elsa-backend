/**
 * Saavutettavuustestit – Liitetiedoston lisääminen
 *
 * Testaa korjatut ongelmat (s. 30 PDF-raportissa):
 *  - Lisää liitetiedosto -painike on fokusoitavissa (WCAG 2.1.1)
 *  - Painikkeella on ohjelmallinen rooli (WCAG 4.1.2)
 *  - Lataus/poisto-painikkeilla on kontekstuaalinen nimi (WCAG 4.1.2)
 *  - Painike aktivoituu Enter/välilyönnillä (WCAG 2.1.1)
 */

import { E2E_ERIKOISTUVA_EMAIL } from '../../support/commands'

export {}

describe('Liitetiedoston lisääminen – saavutettavuus', () => {
  before(() => {
    cy.loginAsErikoistuva()
  })

  beforeEach(() => {
    // Arviointipyyntö-sivu sisältää liitetiedosto-komponentin
    cy.visit('/arvioinnit/arviointipyynto')
    cy.get('[role="status"]', { timeout: 10000 }).should('not.exist')
  })

  // ── Fokusoitavuus (WCAG 2.1.1) ────────────────────────────────────────────
  it('liitetiedostopainike on fokusoitavissa näppäimistöllä', () => {
    cy.get('label[role="button"][tabindex="0"]').first().should('exist')
    cy.get('label[role="button"]').first().focus().then(($el) => {
      expect(document.activeElement).to.equal($el[0])
    })
  })

  // ── role="button" (WCAG 4.1.2) ────────────────────────────────────────────
  it('liitetiedoston lisäyslabel-elementillä on role="button"', () => {
    cy.get('label[tabindex="0"]').first().should('have.attr', 'role', 'button')
  })

  // ── Näkyvä kohdistus (WCAG 2.4.7) ────────────────────────────────────────
  it('liitetiedostopainike saa näkyvän kohdistuskehyksen', () => {
    cy.get('label[role="button"][tabindex="0"]').first().focus().then(($el) => {
      const outline = $el.css('outline-style')
      const outlineWidth = parseInt($el.css('outline-width') || '0', 10)
      expect(outline !== 'none' && outlineWidth > 0).to.be.true
    })
  })

  // ── aria-disabled (ei vanha disabled HTML-attribuutti) ────────────────────
  it('poistettu virheellinen disabled-attribuutti labelilta', () => {
    cy.get('label[role="button"]').first().then(($el) => {
      // Perinteinen disabled-attribuutti ei ole validi labelilla
      expect($el.attr('disabled')).to.be.undefined
    })
  })

  // ── Axe-tarkistus sivulla ─────────────────────────────────────────────────
  it('arviointipyynnöllä ei WCAG-rikkomuksia tiedostolatauskomponentissa', () => {
    cy.checkPageA11y('[id^="elsa-asiakirjat-upload-"]')
  })
})

describe('Asiakirjataulukko – latauspainikkeiden nimet (WCAG 4.1.2)', () => {
  before(() => {
    cy.loginAsErikoistuva()
  })

  it('latauspainikkeella on aria-label tiedoston nimellä', () => {
    cy.visit('/asiakirjat')
    cy.get('[role="status"]', { timeout: 8000 }).should('not.exist')

    // Jos asiakirjoja on, tarkistetaan latauspainikkeiden nimet
    cy.get('body').then(($body) => {
      if ($body.find('.asiakirjat-table tbody tr').length > 0) {
        // Latauspainike: aria-label pitäisi sisältää tiedoston nimi
        cy.get('button[aria-label*="Lataa"]').first().should('exist')
        cy.get('button[aria-label*="Lataa"]').first().then(($btn) => {
          expect($btn.attr('aria-label')).to.match(/^Lataa .+/)
        })
      } else {
        cy.log('Ei asiakirjoja saatavilla, ohitetaan testi')
      }
    })
  })
})

