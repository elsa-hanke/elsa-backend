import { E2E_ERIKOISTUVA_EMAIL, KOULUTTAJA_EMAIL, VASTUUHENKILO_EMAIL } from '../../support/commands/credentials'

export {}

// Käyttötapaus 6.
// Koejakson koulutussopimuksen täyttäminen ja lähettäminen, erikoistuja
// Käyttäjä: Erikoistuja
// Tavoite: Lähettää koulutussopimus kouluttajan hyväksyttäväksi
// Laukaisija: Erikoistuja aloittaa koejakson
// Esiehto: Erikoistujalla on opinto-oikeus opintotietojärjestelmässä.
//          Erikoistuja on lisännyt kouluttajalle katseluoikeudet.
// Käyttötapauksen kulku:
// 1. Erikoistuja siirtyy koejakso-sivulle
// 2. Erikoistuja avaa koulutussopimus-lomakkeen
// 3. Erikoistuja täyttää lomakkeen tiedot
// 4. Erikoistuja lähettää lomakkeen kouluttajan hyväksyttäväksi

const KOULUTTAJA_ETUNIMI     = 'E2E'
const KOULUTTAJA_SUKUNIMI    = 'Kouluttaja'
const VASTUUHENKILO_ETUNIMI  = 'E2E'
const VASTUUHENKILO_SUKUNIMI = 'Vastuuhenkilo'

describe('Koulutussopimus', () => {
  // ── Esialustetaan tietokanta testisarjaa varten ──────────────────────────────
  before(() => {
    Cypress.session.clearAllSavedSessions()

    // Siivotaan vanhat koejakso-lomakkeet ennen erikoistuvan poistoa (FK-turvallisuus)
    cy.task('db:cleanupKoejakso', { erikoistuvaEmail: E2E_ERIKOISTUVA_EMAIL })
    cy.task('db:cleanupErikoistuva', { email: E2E_ERIKOISTUVA_EMAIL })

    // Siemennetään tukikäyttäjät
    cy.task('db:seedKouluttaja', {
      email: KOULUTTAJA_EMAIL,
      etunimi: KOULUTTAJA_ETUNIMI,
      sukunimi: KOULUTTAJA_SUKUNIMI,
    })
    cy.task('db:seedVastuuhenkilo', {
      email: VASTUUHENKILO_EMAIL,
      etunimi: VASTUUHENKILO_ETUNIMI,
      sukunimi: VASTUUHENKILO_SUKUNIMI,
    })

    // Kirjautuminen luo erikoistuvan ja opinto-oikeuden (createWithoutOpintotietodata)
    cy.loginAsErikoistuva()

    // Luodaan kouluttajavaltuutus kirjautumisen jälkeen (opinto-oikeus täytyy olla olemassa)
    cy.task('db:seedKouluttajavaltuutus', {
      erikoistuvaEmail: E2E_ERIKOISTUVA_EMAIL,
      kouluttajaEmail: KOULUTTAJA_EMAIL,
    })
  })

  // ── Käyttötapaus 6: Erikoistuja lähettää koulutussopimuksen ─────────────────

  it('Käyttötapaus 6: Erikoistuja täyttää ja lähettää koulutussopimuksen', () => {
    // Siivotaan mahdollinen edellinen koulutussopimus jotta testi on idempotentti
    cy.task('db:cleanupKoejakso', { erikoistuvaEmail: E2E_ERIKOISTUVA_EMAIL })

    const today = new Date()
    const dd = String(today.getDate()).padStart(2, '0')
    const mm = String(today.getMonth() + 1).padStart(2, '0')
    const yyyy = today.getFullYear()
    const todayStr = `${dd}.${mm}.${yyyy}`

    // 1. Erikoistuja siirtyy koejakso-sivulle
    cy.visit('/koejakso')
    cy.contains('h1', 'Koejakso').should('be.visible')

    // 2. Erikoistuja avaa koulutussopimus-lomakkeen
    cy.visit('/koejakso/koulutussopimus')
    cy.get('[role="status"]', { timeout: 10000 }).should('not.exist')

    // 3. Erikoistuja täyttää lomakkeen – sähköposti
    cy.contains('label', 'Sähköpostiosoite')
      .parent()
      .find('input')
      .clear()
      .type(E2E_ERIKOISTUVA_EMAIL)

    // Matkapuhelinnumero
    cy.contains('label', 'Matkapuhelinnumero')
      .parent()
      .find('input')
      .clear()
      .type('+358401234567')

    // Koulutuspaikan nimi
    cy.contains('label', 'Toimipaikan nimi')
      .parent()
      .find('input[type="text"]')
      .first()
      .clear()
      .type('E2E Testisairaala')

    // Valitaan "Kyllä" koulutussopimus-radioryhmästä
    cy.contains('Toimipaikalla on koulutussopimus oman yliopiston kanssa')
      .should('be.visible')
    cy.contains('label', 'Kyllä').click()

    // Koejakson alkamispäivä
    cy.contains('label', 'Koejakson alkamispäivä')
      .parent()
      .find('input.date-input, input[type="text"]')
      .first()
      .clear()
      .type(todayStr)
      .blur()

    // Kouluttajan valinta – valitaan siemennetty kouluttaja
    cy.contains('label', 'Kouluttaja')
      .parent()
      .find('.multiselect')
      .click()
    cy.get('.multiselect--active .multiselect__option')
      .contains(`${KOULUTTAJA_ETUNIMI} ${KOULUTTAJA_SUKUNIMI}`)
      .click({ force: true })

    // 4. Erikoistuja lähettää lomakkeen
    cy.intercept('POST', '**/erikoistuva-laakari/koejakso/koulutussopimus').as('koulutussopimusPost')
    cy.intercept('GET', '**/erikoistuva-laakari/koejakso').as('koejaksoAfterSubmit')

    cy.contains('button', 'Hyväksy ja lähetä').click()
    cy.get('#confirm-send').find('button').contains('Hyväksy ja lähetä').click()

    cy.wait('@koulutussopimusPost').its('response.statusCode').should('eq', 201)
    cy.wait('@koejaksoAfterSubmit')

    // Vahvistetaan onnistuminen – lomake siirtyy odottamaan hyväksyntöjä readonly-tilaan
    cy.url().should('include', '/koejakso/koulutussopimus')
    cy.contains(
      'Koejakson koulutussopimus on lähetetty kouluttajan ja vastuuhenkilön hyväksyttäväksi.',
      { timeout: 15000 }
    ).should('be.visible')
    cy.contains('E2E Testisairaala').should('be.visible')
    cy.contains(`${KOULUTTAJA_ETUNIMI} ${KOULUTTAJA_SUKUNIMI}`).should('be.visible')
  })
})
