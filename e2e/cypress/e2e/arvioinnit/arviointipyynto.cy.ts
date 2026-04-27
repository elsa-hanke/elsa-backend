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
    // Suljetaan monivalintavalikko painamalla Escape ennen seuraavan avaamista.
    // Monivalintavalikko (multiple=true) jää muuten auki ja seuraava klikkaus
    // sulkee avoinna olevan valikon eikä avaa kouluttajavalikkoa.
    // Käytetään ehdollista sulkemista, koska valikko saattaa jo sulkeutua automaattisesti.
    cy.get('body').then(($body) => {
      if ($body.find('.multiselect--active').length > 0) {
        cy.get('.multiselect--active').type('{esc}')
      }
    })
    cy.get('.multiselect--active').should('not.exist')

    // Kouluttaja / Vastuuhenkilö – valitaan esialustettu kouluttaja
    cy.contains('label', 'Kouluttaja').parent().find('.multiselect').as('kouluttajaSelect').click()
    cy.get('@kouluttajaSelect').should('have.class', 'multiselect--active')
    // cy.contains(selector, text) palauttaa .multiselect__option-elementin itsensä (ei sisällä
    // olevan divin), jolloin klikkaus kohdistuu oikeaan event-kohteeseen.
    // force:true tarvitaan, koska Vue Multiselectin scroll-konttainer voi peittää optionin.
    cy.contains('.multiselect--active .multiselect__option', `${KOULUTTAJA_ETUNIMI} ${KOULUTTAJA_SUKUNIMI}`)
      .click({ force: true })

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
    // Odotetaan, että välilehti on aktiivinen ennen kuin haetaan sen sisältöä.
    // Bootstrap Vue lisää .active-luokan (ei välttämättä .show-luokkaa ilman fade-animaatiota).
    cy.get('.tab-pane.active').should('be.visible')
    // Varmistetaan, että luotu arviointipyyntö näkyy tapahtuman nimen perusteella aktiivisessa välilehdessä
    cy.get('.tab-pane.active').contains('E2E Testitapahtuma').should('be.visible')
    // Klikataan Tee itsearviointi -painiketta
    cy.get('.tab-pane.active').contains('Tee itsearviointi').click()

    cy.url().should('include', '/itsearviointi')
    cy.contains('h1', 'Tee itsearviointi').should('be.visible')

    // Itsearvioinnin täyttäminen ja tallentaminen
    cy.get('textarea').first().type('E2E itsearviointi vastaus.')
    cy.contains('button', 'Tallenna').click()

    // Tallennettu – palataan arvioinnit-listalle
    cy.url().should('include', '/arvioinnit')
  })
})
