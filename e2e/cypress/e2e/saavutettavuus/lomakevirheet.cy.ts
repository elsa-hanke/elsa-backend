/**
 * Saavutettavuustestit – Lomakevirheet ja form-group
 *
 * Testaa korjatut ongelmat (s. 16, 27 PDF-raportissa):
 *  - form-error:lla role="alert" + aria-live (WCAG 4.1.3)
 *  - Pakollisen kentän sr-only-teksti "pakollinen tieto" (WCAG 3.3.2)
 *  - Axe-tarkistus lomakesivuilla
 */

export {}

describe('Lomakevirheet ja form-group – saavutettavuus', () => {
  before(() => {
    cy.loginAsErikoistuva()
  })

  it('lomakevirheiden ja pakollisten kenttien saavutettavuus', () => {
    cy.visit('/tyoskentelyjaksot/uusi')
    cy.get('[role="status"]', { timeout: 10000 }).should('not.exist')

    // ── Pakollisten kenttien labelissa on sr-only "pakollinen tieto" (WCAG 3.3.2)
    cy.contains('.sr-only', /pakollinen/i).should('exist')

    // ── Pakollinen form-group sisältää sr-only-elementin ruudunlukijoille ─────
    cy.get('.form-group').find('.sr-only').should('exist')

    // ── role="alert" ja aria-live="assertive" lomakevirheelle (WCAG 4.1.3) ───
    // Lähetetään lomake tyhjänä käynnistääksemme virheen
    cy.contains('button', /Tallenna|Lisää|Lähetä/).first().click()
    cy.get('[role="alert"]', { timeout: 5000 }).should('exist').then(($alerts) => {
      $alerts.each((_, el) => {
        const live = el.getAttribute('aria-live')
        if (live) {
          expect(live).to.equal('assertive')
        }
      })
    })

    // ── Axe-tarkistus työskentelyjakson lisäämislomakkeella (WCAG 2.1 AA) ────
    cy.checkPageA11y()
  })
})
