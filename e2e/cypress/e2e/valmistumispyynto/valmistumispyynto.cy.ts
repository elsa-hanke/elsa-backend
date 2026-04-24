import { E2E_ERIKOISTUVA_EMAIL, KOULUTTAJA_EMAIL, VASTUUHENKILO_EMAIL, VIRKAILIJA_EMAIL } from '../../support/commands/credentials'

export {}

// Käyttötapaus 13.
// Valmistumispyynnön tekeminen
// Käyttäjä: Erikoistuja, Vastuuhenkilö, Virkailija
// Tavoite: Erikoistuja tekee valmistumispyynnön, vastuuhenkilö arvioi ja virkailija tarkistaa,
//          jonka jälkeen vastuuhenkilö hyväksyy valmistumisen
// Laukaisija: Erikoistuja on suorittanut erikoistumiskoulutuksen ja pyytää valmistumista
// Esiehto: Erikoistujalla on opinto-oikeus opintotietojärjestelmässä. Koejakso on hyväksytty.
// Käyttötapauksen kulku:
// 1. Erikoistuja siirtyy valmistumispyyntö-sivulle
// 2. Erikoistuja täyttää valmistumispyynnön tiedot ja lähettää pyynnön
// 3. Vastuuhenkilö arvioi erikoistujan osaamisen         (vaatii DevLoginResource)
// 4. Virkailija tarkistaa tiedot                         (vaatii DevLoginResource)
// 5. Vastuuhenkilö hyväksyy tai hylkää valmistumisen    (vaatii DevLoginResource)

const KOULUTTAJA_ETUNIMI     = 'E2E'
const KOULUTTAJA_SUKUNIMI    = 'Kouluttaja'
const VASTUUHENKILO_ETUNIMI  = 'E2E'
const VASTUUHENKILO_SUKUNIMI = 'Vastuuhenkilo'
const VIRKAILIJA_ETUNIMI     = 'E2E'
const VIRKAILIJA_SUKUNIMI    = 'Virkailija'

describe('Valmistumispyyntö', () => {
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

    // Luodaan kouluttajavaltuutus
    cy.task('db:seedKouluttajavaltuutus', {
      erikoistuvaEmail: E2E_ERIKOISTUVA_EMAIL,
      kouluttajaEmail: KOULUTTAJA_EMAIL,
    })
  })

  // ── Käyttötapaus 13a: Erikoistuja tekee valmistumispyynnön ──────────────────

  it('Käyttötapaus 13a: Erikoistuja tekee valmistumispyynnön', () => {
    // Varmistetaan esiehto tietokannassa: loppukeskustelu hyväksytty
    cy.task('db:ensureLoppukeskusteluHyvaksytty', {
      erikoistuvaEmail: E2E_ERIKOISTUVA_EMAIL,
      kouluttajaEmail: KOULUTTAJA_EMAIL,
    })

    const today = new Date()
    const dd = String(today.getDate()).padStart(2, '0')
    const mm = String(today.getMonth() + 1).padStart(2, '0')
    const yyyy = today.getFullYear()
    const todayStr = `${dd}.${mm}.${yyyy}`

    // 1. Erikoistuja siirtyy valmistumispyyntö-sivulle
    cy.intercept('GET', '**/erikoistuva-laakari/valmistumispyynto').as('getValmistumispyynto')
    cy.intercept('GET', '**/erikoistuva-laakari/valmistumispyynto-suoritusten-tila').as('getSuoritustenTila')
    cy.intercept('POST', '**/erikoistuva-laakari/valmistumispyynto').as('postValmistumispyynto')

    cy.visit('/valmistumispyynto')
    cy.wait('@getValmistumispyynto').its('response.statusCode').should('eq', 200)
    cy.get('[role="status"]', { timeout: 10000 }).should('not.exist')
    cy.wait('@getSuoritustenTila').its('response.statusCode').should('eq', 200)

    // 2. Erikoistuja vahvistaa esitietovaatimukset ja avaa lomakkeen
    cy.get('input[type="checkbox"]').each(($checkbox) => {
      cy.wrap($checkbox).check({ force: true })
    })
    cy.contains('button', 'Tee valmistumispyyntö').click()

    // 3. Täytetään valmistumispyyntö-lomake
    cy.contains('label', 'Sähköpostiosoite')
      .parent()
      .find('input')
      .clear()
      .type(E2E_ERIKOISTUVA_EMAIL)

    cy.contains('label', 'Matkapuhelinnumero')
      .parent()
      .find('input')
      .clear()
      .type('+358401234567')

    cy.get('body').then(($body) => {
      if ($body.find('label:contains("Valviran laillistamispäivä")').length > 0) {
        cy.contains('label', 'Valviran laillistamispäivä')
          .parent()
          .find('input.date-input, input[type="text"]')
          .first()
          .clear()
          .type(todayStr)
          .blur()
      }
    })

    cy.get('body').then(($body) => {
      if ($body.find('input[type="file"]').length > 0) {
        cy.get('input[type="file"]').first().selectFile(
          'cypress/fixtures/test.pdf',
          { force: true }
        )
      }
    })

    cy.get('body').then(($body) => {
      if ($body.find('textarea').length > 0) {
        cy.get('textarea').first().clear().type('E2E-selvitys vanhentuneista suorituksista.')
      }
    })

    // 4. Lähetetään valmistumispyyntö
    cy.contains('button', 'Lähetä pyyntö').click()
    cy.get('#confirm-send').find('button').contains('Lähetä pyyntö').click()

    cy.wait('@postValmistumispyynto')
      .its('response.statusCode')
      .should('eq', 201)

    cy.contains('Valmistumispyyntö lähetetty', { timeout: 15000 }).should('be.visible')
    cy.contains('Valmistumispyyntö odottaa vastuuhenkilön suorittamaa osaamisten tarkastusta', {
      timeout: 15000,
    }).should('be.visible')
  })

  // ── Käyttötapaukset 13b–13d: Vastuuhenkilö ja virkailija ────────────────────
  // Vaativat dev-kirjautumispisteen. TODO: Ota käyttöön DevLoginResourcen jälkeen.

  // it.skip('Käyttötapaus 13b: Vastuuhenkilö arvioi erikoistujan osaamisen', () => {
  //   cy.loginAsVastuuhenkilo()
  //   cy.visit('/valmistumispyynnot')
  //   cy.get('[role="status"]', { timeout: 10000 }).should('not.exist')
  //   cy.get('table tbody tr, .list-group-item').first().within(() => {
  //     cy.contains('a', 'Osaamisen arviointi').click()
  //   })
  //   cy.url().should('include', '/valmistumispyynnon-arviointi/')
  //   cy.contains('button', 'Lähetä').click()
  //   cy.url().should('not.include', '/valmistumispyynnon-arviointi/')
  // })
  //
  // it.skip('Käyttötapaus 13c: Virkailija tarkistaa valmistumispyynnön tiedot', () => {
  //   cy.loginAsVirkailija()
  //   cy.visit('/valmistumispyynnot')
  //   cy.get('[role="status"]', { timeout: 10000 }).should('not.exist')
  //   cy.get('table tbody tr, .list-group-item').first().within(() => {
  //     cy.contains('a', 'Tarkista').click()
  //   })
  //   cy.url().should('include', '/valmistumispyynnon-tarkistus/')
  //   cy.contains('button', 'Merkitse tarkistetuksi').click()
  //   cy.url().should('not.include', '/valmistumispyynnon-tarkistus/')
  // })
  //
  // it.skip('Käyttötapaus 13d: Vastuuhenkilö hyväksyy valmistumisen', () => {
  //   cy.loginAsVastuuhenkilo()
  //   cy.visit('/valmistumispyynnot')
  //   cy.get('[role="status"]', { timeout: 10000 }).should('not.exist')
  //   cy.get('table tbody tr, .list-group-item').first().within(() => {
  //     cy.contains('a', 'Valmistumisen hyväksyminen').click()
  //   })
  //   cy.url().should('include', '/valmistumispyynnon-hyvaksynta/')
  //   cy.contains('button', 'Hyväksy').click()
  //   cy.url().should('not.include', '/valmistumispyynnon-hyvaksynta/')
  // })
})
