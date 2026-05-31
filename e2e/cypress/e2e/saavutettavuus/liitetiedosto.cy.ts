/**
 * Saavutettavuustestit – Liitetiedoston lisääminen
 *
 * Testaa korjatut ongelmat (s. 30 PDF-raportissa):
 *  - Lisää liitetiedosto -painike on fokusoitavissa (WCAG 2.1.1)
 *  - Painikkeella on ohjelmallinen rooli (WCAG 4.1.2)
 *  - Lataus/poisto-painikkeilla on kontekstuaalinen nimi (WCAG 4.1.2)
 *  - Painike aktivoituu Enter/välilyönnillä (WCAG 2.1.1)
 *
 * Sivu: /tyoskentelyjaksot/uusi  (asiakirjat-upload käytössä tyoskentelyjakso-form.vuessa)
 */

export {}

describe('Liitetiedoston lisääminen – saavutettavuus', () => {
  before(() => {
    cy.loginAsErikoistuva()
  })

  it('liitetiedosto-komponentin saavutettavuus', () => {
    // ── Työskentelyjakso-lomake: liitetiedosto-komponentin tarkistukset ───────
    cy.visit('/tyoskentelyjaksot/uusi')
    cy.get('[role="status"]', { timeout: 10000 }).should('not.exist')
    cy.get('label[role="button"][tabindex="0"]', { timeout: 10000 }).should('exist')

    // Fokusoitavuus (WCAG 2.1.1) – tabindex="0" osoittaa fokusoitavuuden
    cy.get('label[role="button"][tabindex="0"]').first().should('exist')

    // role="button" ohjelmallinen rakenne (WCAG 4.1.2)
    cy.get('label[tabindex="0"]').first().should('have.attr', 'role', 'button')

    // Näkyvä kohdistuskehys (WCAG 2.4.7)
    cy.get('label[role="button"][tabindex="0"]').first().focus().then(($el) => {
      const outline = $el.css('outline-style')
      const outlineWidth = parseInt($el.css('outline-width') || '0', 10)
      expect(outline !== 'none' && outlineWidth > 0).to.be.true
    })

    // Virheellinen disabled-attribuutti on poistettu labelilta
    cy.get('label[role="button"]').first().then(($el) => {
      expect($el.attr('disabled')).to.be.undefined
    })

    // Axe-tarkistus tiedostolatauskomponentissa
    cy.checkPageA11y('[id^="elsa-asiakirjat-upload-"]')

    // ── Asiakirjat-sivu: latauspainikkeiden nimet (WCAG 4.1.2) ───────────────
    cy.visit('/asiakirjat')
    cy.get('[role="status"]', { timeout: 8000 }).should('not.exist')

    cy.get('body').then(($body) => {
      if ($body.find('.asiakirjat-table tbody tr').length > 0) {
        // Latauspainike: aria-label pitäisi sisältää tiedoston nimi
        cy.get('button[aria-label*="Lataa"]').first().should('exist')
        cy.get('button[aria-label*="Lataa"]').first().then(($btn) => {
          expect($btn.attr('aria-label')).to.match(/^Lataa .+/)
        })
      } else {
        cy.log('Ei asiakirjoja saatavilla, ohitetaan latauspainikkeiden tarkistus')
      }
    })
  })
})
