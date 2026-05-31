/**
 * Saavutettavuustestit – Lomakevirheet ja form-group
 *
 * Testaa korjatut ongelmat (s. 16, 27 PDF-raportissa):
 *  - form-error:lla role="alert" + aria-live (WCAG 4.1.3)
 *  - Pakollisen kentän sr-only-teksti "pakollinen tieto" (WCAG 3.3.2)
 *  - Axe-tarkistus lomakesivuilla
 */

import { E2E_ERIKOISTUVA_EMAIL } from '../../support/commands'

export {}

describe('Lomakevirheet – saavutettavuus', () => {
  before(() => {
    cy.loginAsErikoistuva()
  })

  beforeEach(() => {
    cy.visit('/tyoskentelyjaksot/uusi')
    cy.get('[role="status"]', { timeout: 10000 }).should('not.exist')
  })

  // ── role="alert" lomakevirheille (WCAG 4.1.3) ─────────────────────────────
  it('lomakevirheellä on role="alert" ja aria-live="assertive"', () => {
    // Lähetetään lomake tyhjänä käynnistääksemme virheen
    cy.contains('button', /Tallenna|Lisää|Lähetä/).first().click()

    // Odotetaan virhealueen ilmestymistä
    cy.get('[role="alert"]', { timeout: 5000 }).should('exist').then(($alerts) => {
      $alerts.each((_, el) => {
        // role="alert" implisiittisesti on aria-live="assertive"
        const live = el.getAttribute('aria-live')
        if (live) {
          expect(live).to.equal('assertive')
        }
      })
    })
  })

  // ── Pakollisen kentän sr-only-teksti (WCAG 3.3.2) ─────────────────────────
  it('pakollisten kenttien labelissa on näkymätön "pakollinen tieto" -teksti', () => {
    // Haetaan pakollisuusmerkin (*) yhteydessä oleva sr-only span
    cy.get('.sr-only').then(($spans) => {
      const texts = [...$spans].map((el) => el.textContent?.trim())
      expect(texts.some((t) => t && t.toLowerCase().includes('pakollinen'))).to.be.true
    })
  })

  // ── Axe-tarkistus lomakkeella ─────────────────────────────────────────────
  it('työskentelyjakson lisäämislomakkeella ei WCAG-rikkomuksia', () => {
    cy.checkPageA11y()
  })
})

describe('Form-group – pakollinen tieto -merkintä', () => {
  before(() => {
    cy.loginAsErikoistuva()
  })

  it('pakollinen kenttä sisältää sr-only-elementin ruudunlukijoille', () => {
    cy.visit('/tyoskentelyjaksot/uusi')
    cy.get('[role="status"]', { timeout: 10000 }).should('not.exist')

    // Tarkistetaan, että pakollisissa form-groupeissa on sr-only-elementti
    cy.get('.form-group').then(($groups) => {
      let found = false
      $groups.each((_, group) => {
        const srOnly = Cypress.$(group).find('.sr-only')
        if (srOnly.length > 0) {
          const text = srOnly.text().trim().toLowerCase()
          if (text.includes('pakollinen')) {
            found = true
          }
        }
      })
      expect(found).to.be.true
    })
  })
})

