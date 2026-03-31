import {E2E_ERIKOISTUVA_EMAIL} from "../../support/commands";

export {}

/**
 * Käyttötapaus 1 – Katseluoikeuden lisääminen kouluttajalle
 *
 * Käyttäjät: Erikoistuja, kouluttaja
 * Tavoite: Lisätä katseluoikeus kouluttajalle, jotta hän pääsee ELSA-palveluun
 * Laukaisija: Erikoistuja on saanut opinto-oikeuden ja hänelle on nimetty kouluttaja
 * Esiehto: Erikoistuvalla on opinto-oikeus opintotietojärjestelmässä. Kouluttaja on kirjautumassa palveluun ensimmäistä kertaa, hänellä ei vielä ole käyttöoikeutta.
 * Poikkeukset:
 *  - Kouluttaja löytyy jo alasvetovalikosta (joku on jo aikaisemmin lisännyt hänet kouluttajakseen)
 *  - Erikoistuja lähettää katseluoikeudet väärällä nimellä tai väärään sähköpostiin
 *  - Kouluttaja ei muista käyttää kutsulinkkiä ensimmäiseen kirjautumiseen. Muu kirjautumistapa ei toimi tunnusten aktivointiin.
 * Käyttötapauksen kulku:
 *  1. Erikoistuja kirjautuu järjestelmään Suomi.fi-tunnistautumisella
 *  2. Erikoistuja siirtyy yläkulmasta omaan profiiliinsa ja Katseluoikeudet-välilehdelle
 *  3. Erikoistuja tarkistaa löytyykö kouluttaja listasta. Jos ei löydy, lisää uuden kouluttajan.
 *  4. Erikoistuja kirjaa kouluttajan nimen ja sähköpostin Lisää kouluttaja -kohtaan ja vahvistaa lisäyksen
 *  5. Kouluttaja näkyy nyt erikoistujalla Katseluoikeudet-sivulla
 *  6. Kouluttaja saa sähköpostiinsa kutsun ELSA-palvelun käyttäjäksi
 *  7. Kouluttaja kirjautuu kutsuviestissä olevan linkin kautta, ja hänen käyttöoikeutensa aktivoituu
 */

const KOULUTTAJA_EMAIL   = 'test-kouluttaja@test.elsa'
const KOULUTTAJA_ETUNIMI = 'Testi'
const KOULUTTAJA_SUKUNIMI = 'Kouluttaja'

describe('Katseluoikeudet', () => {
  before(() => {
    Cypress.session.clearAllSavedSessions()
    cy.task('db:cleanupErikoistuva', { email: E2E_ERIKOISTUVA_EMAIL })
    // Seed the kouluttaja before logging in as erikoistuva.
    // Idempotent — safe to call on repeated runs.
    // cy.task('db:seedKouluttaja', {
    //   email: KOULUTTAJA_EMAIL,
    //   etunimi: KOULUTTAJA_ETUNIMI,
    //   sukunimi: KOULUTTAJA_SUKUNIMI,
    // })

    cy.loginAsErikoistuva()
  })

  after(() => {
    // Clean up so the test is fully idempotent across CI runs.
    // Remove view-access grant first, then the seeded kouluttaja.
    cy.task('db:cleanupKouluttaja', { email: KOULUTTAJA_EMAIL })
  })

  context('Katseluoikeuden lisääminen kouluttajalle (case 1)', () => {
    it('navigates, selects, grants, and verifies katseluoikeus for kouluttaja', () => {
      // Erikoistuja siirtyy profiiliin ja avaa Katseluoikeudet-välilehden
      cy.visit('/profiili#katseluoikeudet')
      cy.contains('h1', 'Oma profiili').should('be.visible')
      cy.contains('.nav-link', 'Katseluoikeudet').should('be.visible')

      // Näytetään esisiemennetty kouluttaja alasvetovalikossa
      cy.contains('.nav-link', 'Katseluoikeudet').click()
      cy.contains('Katseluoikeudet').should('be.visible')
      cy.contains('label', 'Kouluttaja')
        .parent()
        .as('kouluttajaGroup')
      cy.get('@kouluttajaGroup')
        .find('.multiselect')
        .click()
      cy.get('.multiselect__content .multiselect__option')
        .contains(`${KOULUTTAJA_ETUNIMI} ${KOULUTTAJA_SUKUNIMI}`)
        .should('be.visible')
        .click()

      // Myönnetään katseluoikeus kouluttajalle
      cy.contains('button', 'Myönnä oikeus').click()
      cy.contains(`${KOULUTTAJA_ETUNIMI} ${KOULUTTAJA_SUKUNIMI}`).should('be.visible')
      cy.contains(KOULUTTAJA_EMAIL).should('be.visible')

      // Kouluttaja näkyy erikoistujan Katseluoikeudet-sivulla myös sivun päivityksen jälkeen
      cy.visit('/profiili#katseluoikeudet')
      cy.contains('.nav-link', 'Katseluoikeudet').click()
      cy.contains(`${KOULUTTAJA_ETUNIMI} ${KOULUTTAJA_SUKUNIMI}`).should('be.visible')
    })
  })
})
