import { E2E_ERIKOISTUVA_EMAIL, KOULUTTAJA_EMAIL, VASTUUHENKILO_EMAIL, VIRKAILIJA_EMAIL } from '../../support/commands/credentials'

export {}

// Käyttötapaus 9.
// Aloituskeskustelun täyttäminen ja lähettäminen, erikoistuja ja kouluttaja
// Käyttäjä: Erikoistuja (9a) ja Kouluttaja (9b – vaatii DevLoginResource)
// Tavoite: Täyttää aloituskeskustelu-lomake ja saada kouluttajan hyväksyntä
// Laukaisija: Koejakson koulutussopimus on hyväksytty
// Esiehto: Koulutussopimus on hyväksytty vastuuhenkilön puolesta.
//
// Käyttötapaus 10.
// Väliarvioinnin täyttäminen ja hyväksyminen
// Käyttäjä: Erikoistuja (10a) ja Kouluttaja (10b – vaatii DevLoginResource)
// Laukaisija: Aloituskeskustelu on hyväksytty
//
// Käyttötapaus 11.
// Loppukeskustelun täyttäminen ja hyväksyminen
// Käyttäjä: Erikoistuja (11a) ja Kouluttaja (11b – vaatii DevLoginResource)
// Laukaisija: Väliarviointi on hyväksytty
//
const KOULUTTAJA_ETUNIMI     = 'E2E'
const KOULUTTAJA_SUKUNIMI    = 'Kouluttaja'
const VASTUUHENKILO_ETUNIMI  = 'E2E'
const VASTUUHENKILO_SUKUNIMI = 'Vastuuhenkilo'
const VIRKAILIJA_ETUNIMI     = 'E2E'
const VIRKAILIJA_SUKUNIMI    = 'Virkailija'

describe('Koejakson arvioinnit', () => {
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
    cy.task('db:seedVirkailija', {
      email: VIRKAILIJA_EMAIL,
      etunimi: VIRKAILIJA_ETUNIMI,
      sukunimi: VIRKAILIJA_SUKUNIMI,
    })

    // Kirjautuminen luo erikoistuvan ja opinto-oikeuden (createWithoutOpintotietodata)
    cy.loginAsErikoistuva()

    // Luodaan kouluttajavaltuutus kirjautumisen jälkeen
    cy.task('db:seedKouluttajavaltuutus', {
      erikoistuvaEmail: E2E_ERIKOISTUVA_EMAIL,
      kouluttajaEmail: KOULUTTAJA_EMAIL,
    })
  })

  // ── Käyttötapaukset 9a, 10a, 11a: kaikki erikoistuja-toimet yhdessä testissä ─
  // Kouluttajan ja esihenkilön hyväksynnät (9b/10b/11b) simuloidaan tietokantatehtävillä,
  // jotta erikoistujan voi jatkaa seuraavaan lomakkeeseen ilman erillistä kirjautumista.

  it('Käyttötapaukset 9a, 10a, 11a: Erikoistuja täyttää ja lähettää koejakson arviointilomakkeet', () => {
    // Siivotaan mahdolliset vanhat lomakkeet ja siemennetään hyväksytty koulutussopimus
    cy.task('db:cleanupKoejakso', { erikoistuvaEmail: E2E_ERIKOISTUVA_EMAIL })
    cy.task('db:ensureKoulutussopimusHyvaksytty', {
      erikoistuvaEmail: E2E_ERIKOISTUVA_EMAIL,
      kouluttajaEmail: KOULUTTAJA_EMAIL,
    })

    // ── Käyttötapaus 9a: Erikoistuja täyttää ja lähettää aloituskeskustelun ──

    cy.visit('/koejakso/aloituskeskustelu')
    cy.get('[role="status"]', { timeout: 10000 }).should('not.exist')

    // Sähköpostiosoite
    cy.contains('label', 'Sähköpostiosoite')
      .parent().find('input').first().clear().type(E2E_ERIKOISTUVA_EMAIL)

    // Koejakson suorituspaikka
    cy.contains('label', 'Koejakson suorituspaikka')
      .parent().find('input').clear().type('E2E Testisairaala')

    // Koejakson alkamispäivä – käytetään tämän päivän päivämäärää, jotta se ei ole ennen opinto-oikeuden myöntämispäivää
    const today = new Date()
    const dd = String(today.getDate()).padStart(2, '0')
    const mm = String(today.getMonth() + 1).padStart(2, '0')
    const yyyy = today.getFullYear()
    const todayStr = `${dd}.${mm}.${yyyy}`
    const endStr = `${dd}.${mm}.${yyyy + 1}`

    cy.contains('label', 'Koejakson alkamispäivä')
      .parent()
      .find('input.date-input, input[type="text"]')
      .first().clear().type(todayStr).blur()

    // Koejakson päättymispäivä
    cy.contains('label', 'Koejakson päättymispäivä')
      .parent()
      .find('input.date-input, input[type="text"]')
      .first().clear().type(endStr).blur()

    // Koejakso suoritetaan kokoaikatyössä – valitaan "Kyllä"
    cy.contains('Kyllä').click()

    // Kouluttaja – valitaan ensimmäinen vaihtoehto multiselect-alasvetovalikosta
    cy.selectFirstMultiselectOption(
      cy.contains('label', 'Kouluttaja').parent()
    )

    // Lähiesimies – valitaan ensimmäinen vaihtoehto multiselect-alasvetovalikosta
    cy.selectFirstMultiselectOption(
      cy.contains('label', /Lähiesihenkilo|Lähiesimies/).parent()
    )

    // Koejakson osaamistavoitteet
    cy.contains('label', /osaamistavoitteet/i)
      .parent()
      .find('textarea, input[type="text"]')
      .first().clear().type('E2E-testiosaamistavoitteet')

    cy.contains('button', 'Lähetä').click()
    // Vahvistetaan lähetys-modaalin "Lähetä"-painikkeella (confirm-send -modaali)
    cy.get('#confirm-send').find('button').contains('Lähetä').click()
    // Lomake lähetetty – sivu pysyy samalla URL:lla mutta näyttää vain luku -tilan
    cy.contains('lähetetty kouluttajan', { timeout: 15000 }).should('be.visible')

    // Simuloidaan kouluttajan ja esihenkilön hyväksyntä tietokannassa
    // (korvaa käyttötapauksen 9b, joka vaatii dev-kirjautumispisteen)
    cy.task('db:ensureAloituskeskusteluHyvaksytty', {
      erikoistuvaEmail: E2E_ERIKOISTUVA_EMAIL,
      kouluttajaEmail: KOULUTTAJA_EMAIL,
    })

    // ── Käyttötapaus 10a: Erikoistuja täyttää ja lähettää väliarvioinnin ──

    // Poistetaan mahdolliset vanhat väliarviointi- ja loppukeskustelu-rivit
    cy.task('db:cleanupValiarviointi', { erikoistuvaEmail: E2E_ERIKOISTUVA_EMAIL })

    cy.visit('/koejakso/valiarviointi')
    cy.get('[role="status"]', { timeout: 10000 }).should('not.exist')


    // Edistyminen tavoitteiden mukaisesti – valitaan "Kyllä"
    cy.get('body').then(($body) => {
      if ($body.find('label:contains("Kyllä")').length > 0) {
        cy.contains('label', 'Kyllä').first().click()
      }
    })

    // Kouluttaja – valitaan ensimmäinen vaihtoehto multiselect-alasvetovalikosta
    cy.selectFirstMultiselectOption(
      cy.contains('label', 'Kouluttaja').parent()
    )

    // Lähiesimies – valitaan ensimmäinen vaihtoehto multiselect-alasvetovalikosta
    cy.selectFirstMultiselectOption(
      cy.contains('label', /Lähiesihenkilo|Lähiesimies/).parent()
    )

    // Odotetaan onnistunutta pyyntöä ennen etenemistä
    cy.intercept('POST', '**/koejakso/valiarviointi').as('valiarviointiPost')
    cy.contains('button', 'Lähetä').click()
    // Vahvistetaan lähetys-modaalin "Lähetä"-painikkeella (confirm-send -modaali)
    cy.get('#confirm-send').find('button').contains('Lähetä').click()
    cy.wait('@valiarviointiPost', { timeout: 15000 })

    // Simuloidaan kouluttajan hyväksyntä tietokannassa (korvaa käyttötapauksen 10b)
    cy.task('db:ensureValiarviointiHyvaksytty', {
      erikoistuvaEmail: E2E_ERIKOISTUVA_EMAIL,
      kouluttajaEmail: KOULUTTAJA_EMAIL,
    })

    // ── Käyttötapaus 11a: Erikoistuja täyttää ja lähettää loppukeskustelun ──

    // Poistetaan mahdollinen vanha loppukeskustelu-rivi ennen lomakkeen täyttämistä
    cy.task('db:cleanupLoppukeskustelu', { erikoistuvaEmail: E2E_ERIKOISTUVA_EMAIL })

    cy.visit('/koejakso/loppukeskustelu')
    cy.get('[role="status"]', { timeout: 10000 }).should('not.exist')


    // Kouluttaja – valitaan ensimmäinen vaihtoehto multiselect-alasvetovalikosta
    cy.selectFirstMultiselectOption(
      cy.contains('label', 'Kouluttaja').parent()
    )

    // Lähiesimies – valitaan ensimmäinen vaihtoehto multiselect-alasvetovalikosta
    cy.selectFirstMultiselectOption(
      cy.contains('label', /Lähiesihenkilo|Lähiesimies/).parent()
    )

    cy.contains('label', 'Koejakson päättymispäivä')
      .parent()
      .find('input.date-input, input[type="text"]')
      .first().clear().type(endStr).blur()

    // Odotetaan onnistunutta pyyntöä ennen etenemistä
    cy.intercept('POST', '**/koejakso/loppukeskustelu').as('loppukeskusteluPost')
    cy.contains('button', 'Lähetä').click()
    // Vahvistetaan lähetys-modaalin "Lähetä"-painikkeella (confirm-send -modaali)
    cy.get('#confirm-send').find('button').contains('Lähetä').click()
    cy.wait('@loppukeskusteluPost', { timeout: 15000 })
    cy.contains('Koejakso').should('be.visible')
  })
})

