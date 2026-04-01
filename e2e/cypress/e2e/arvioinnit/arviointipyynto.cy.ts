import {E2E_ERIKOISTUVA_EMAIL} from "../../support/commands";

export {}

// Käyttötapaus 2.
// Arviointipyynnön tekeminen ja itsearviointi, erikoistuja
// Käyttäjä: Erikoistuja
// Tavoite: Lähettää arviointipyyntö ja tehdä itsearviointi
// Laukaisija: Erikoistuja on tekemässä arvioitavaa kokonaisuutta
// Esiehto: Erikoistuvalla on opinto-oikeus opintotietojärjestelmässä.
//          Erikoistuja on lisännyt kouluttajalle katseluoikeudet.
// Poikkeukset:
// - Kouluttajalla ei vielä ole katseluoikeutta. Uuden katseluoikeuden saa lisättyä
//   lomakkeelta kouluttajavalinnan kohdalta (Lisää kouluttaja)
// Käyttötapauksen kulku:
// 1. Erikoistuja siirtyy sivulle Osaaminen: Arvioinnit
// 2. Erikoistuja avaa uuden arviointipyynnön (Pyydä arviointia)
// 3. Erikoistuja täyttää arviointipyynnön tiedot ja valitsee arvioinnin antajan
// 4. Erikoistuja lähettää arviointipyynnön
// 5. Erikoistuja saa tiedon, että arviointipyyntö on lähetetty ja siirtyy tekemään itsearviointia
// 6. Erikoistuja täyttää itsearvioinnin tiedot ja tallentaa
//
// Käyttötapaus 3.
// Arvioinnin antaminen, kouluttaja
// Käyttäjä: Kouluttaja
// Tavoite: Antaa erikoistujalle arviointi
// Laukaisija: Erikoistuja on lähettänyt arviointipyynnön kouluttajalle
// Esiehto: Erikoistuja on lisännyt kouluttajalle katseluoikeudet ja arviointipyynnön.
// Poikkeukset:
// - Kouluttaja ei ole ennen kirjautunut ELSA-palveluun, jolloin hänen on ensimmäisellä
//   kerralla kirjauduttava kutsulinkistä.
// Käyttötapauksen kulku:
// 1. Kouluttaja saa sähköpostiinsa herätteen arviointipyynnön saapumisesta
// 2. Kouluttaja kirjautuu ELSA-palveluun Suomi.fi-tunnistautumisella
// 3. Kouluttaja näkee etusivullaan Avoimissa asioissa arviointipyynnön
// 4. Kouluttaja valitsee arviointityökalun ja kirjaa arviointinsa lomakkeelle
// 5. Kouluttaja lähettää arvioinnin

const KOULUTTAJA_EMAIL    = 'test-kouluttaja@test.elsa'
const KOULUTTAJA_ETUNIMI  = 'Testi'
const KOULUTTAJA_SUKUNIMI = 'Kouluttaja'

describe('Arviointipyyntö', () => {
  // Esialustetaan tietokanta koko testisarjaa varten
  before(() => {
    Cypress.session.clearAllSavedSessions()
    cy.task('db:cleanupErikoistuva', { email: E2E_ERIKOISTUVA_EMAIL })
    cy.task('db:seedKouluttaja', {
      email: KOULUTTAJA_EMAIL,
      etunimi: KOULUTTAJA_ETUNIMI,
      sukunimi: KOULUTTAJA_SUKUNIMI,
    })
    // Kirjautuminen luo erikoistuvan ja opinto-oikeuden tietokantaan (createWithoutOpintotietodata)
    cy.loginAsErikoistuva()
    // Luodaan työskentelyjakso opinto-oikeuden luomisen jälkeen, jotta arviointipyyntö-lomakkeen
    // Työskentelyjakso-valikossa on valittavia vaihtoehtoja
    cy.task('db:seedTyoskentelyjakso', { email: E2E_ERIKOISTUVA_EMAIL })
  })

  // Käyttötapaus 2 – Erikoistuja
  it('Käyttötapaus 2: Arviointipyynnön tekeminen ja itsearviointi (erikoistuja)', () => {
    // 1. Erikoistuja siirtyy sivulle Osaaminen: Arvioinnit
    cy.visit('/arvioinnit')
    cy.contains('h1', 'Arvioinnit').should('be.visible')

    // 2. Erikoistuja avaa uuden arviointipyynnön
    cy.visit('/arvioinnit/arviointipyynto')
    cy.contains('h1', 'Pyydä arviointia').should('be.visible')

    // 3. Erikoistuja täyttää arviointipyynnön tiedot ja valitsee arvioinnin antajan
    // Odotetaan lomakkeen latautumista
    cy.get('.arviointipyynto').should('be.visible')
    cy.get('[role="status"]', { timeout: 10000 }).should('not.exist')

    // Työskentelyjakso – valitaan ensimmäinen vaihtoehto
    cy.contains('label', 'Työskentelyjakso').parent().find('.multiselect').click()
    cy.get('.multiselect--active .multiselect__option:not(.multiselect__option--group):not(.multiselect__option--disabled)')
      .first()
      .click()

    // Arvioitavat kokonaisuudet – valitaan ensimmäinen ei-ryhmäotsikko-vaihtoehto
    cy.contains('label', 'Arvioitavat kokonaisuudet').parent().find('.multiselect').click()
    cy.get('.multiselect--active .multiselect__option:not(.multiselect__option--group):not(.multiselect__option--disabled)')
      .first()
      .click()

    // Kouluttaja / Vastuuhenkilö – valitaan esialustettu kouluttaja
    cy.contains('label', 'Kouluttaja').parent().find('.multiselect').click()
    cy.get('.multiselect--active .multiselect__option')
      .contains(`${KOULUTTAJA_ETUNIMI} ${KOULUTTAJA_SUKUNIMI}`)
      .click()

    // Arvioitava tapahtuma – kirjataan tapahtuman nimi (näkyy korttinäkymässä)
    cy.contains('label', 'Arvioitava tapahtuma')
      .parent()
      .find('input[type="text"]')
      .clear()
      .type('E2E Testitapahtuma')

    // Tapahtuman ajankohta – päivämäärä työskentelyjakson aikavälillä (2020-01-01 – avoin)
    cy.contains('label', 'Tapahtuman ajankohta')
      .parent()
      .find('input.date-input')
      .clear()
      .type('01.01.2025')
      .blur()

    // 4. Erikoistuja lähettää arviointipyynnön
    cy.contains('button', 'Lähetä').click()

    // 5. Erikoistuja saa tiedon, että arviointipyyntö on lähetetty
    cy.url().should('include', 'arviointipyynto-lahetetty')
    cy.contains('Arviointipyyntö lähetetty').should('be.visible')

    // 6. Erikoistuja siirtyy tekemään itsearviointia
    cy.visit('/arvioinnit')
    // Siirrytään Arviointipyynnöt-välilehdelle, jossa odottavat pyynnöt näkyvät
    cy.contains('.nav-link', 'Arviointipyynnöt').click()
    // Varmistetaan, että luotu arviointipyyntö näkyy tapahtuman nimen perusteella
    cy.contains('E2E Testitapahtuma').should('be.visible')
    // Klikataan Tee itsearviointi -painiketta
    cy.contains('Tee itsearviointi').click({ force: true })

    cy.url().should('include', '/itsearviointi')
    cy.contains('h1', 'Tee itsearviointi').should('be.visible')

    // Itsearvioinnin täyttäminen ja tallentaminen
    cy.get('textarea').first().type('E2E itsearviointi vastaus.')
    cy.contains('button', 'Tallenna').click()

    // Tallennettu – palataan arvioinnit-listalle
    cy.url().should('include', '/arvioinnit')
  })

  // Käyttötapaus 3 – Kouluttaja
  // Kouluttajan kirjautuminen vaatii kutsulinkki-prosessin (verification-token).
  // TODO: toteuta, kun toinen Suomi.fi-testitunnus on vahvistettu testiympäristössä
  //       ja token haetaan tietokantatehtävällä.
  context('Käyttötapaus 3: Arvioinnin antaminen (kouluttaja)', () => {
    it.skip('kouluttaja näkee avoimen arviointipyynnön etusivullaan', () => {
      // Odottaa toteutusta: kutsulinkki-kirjautumisprosessia ei ole vielä automatisoitu
      cy.loginAsKouluttaja()

      // 3. Kouluttaja näkee etusivullaan Avoimissa asioissa arviointipyynnön
      cy.visit('/etusivu')
      cy.contains('Avoimet asiat').should('be.visible')
      cy.contains('E2E Testitapahtuma').should('be.visible')
    })

    it.skip('kouluttaja täyttää ja lähettää arvioinnin', () => {
      cy.loginAsKouluttaja()

      // 4. Kouluttaja valitsee arviointityökalun ja kirjaa arviointinsa lomakkeelle
      cy.visit('/arvioinnit')
      cy.contains('E2E Testitapahtuma')
        .closest('tr, .list-item, [class*="row"]')
        .contains('Arvioi')
        .click({ force: true })

      cy.selectFirstMultiselectOption(
        cy.contains('label', 'Arviointityökalu').parent()
      )

      // 5. Kouluttaja lähettää arvioinnin
      cy.contains('button', 'Lähetä').click()

      cy.url().should('include', '/arvioinnit')
    })
  })
})
