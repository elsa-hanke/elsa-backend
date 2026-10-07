/**
 * Työskentelyjakson poistaminen (onnistunut polku)
 *
 * Kattaa:
 *  - Työskentelyjakson luominen liitetiedostoineen (PDF)
 *  - Työskentelyjakson poistaminen yksityiskohtasivulta vahvistusdialogin kautta
 *  - Poistetun jakson katoaminen työskentelyjaksojen listalta
 *  - Liitetiedoston säilyminen Asiakirjat-sivulla (vain viittaus jaksoon poistuu)
 *
 * Backendin käyttöoikeus- ja IDOR-tarkistukset on katettu Spring-integraatiotesteillä
 * (ErikoistuvaLaakariTyoskentelyjaksoResourceIT ja TyoskentelyjaksoServiceDeleteIT).
 */

describe('Työskentelyjakson poisto', () => {
  const TYOSKENTELYPAIKKA = 'E2E Poistettava jakso'
  const LIITETIEDOSTO = 'test.pdf'

  beforeEach(() => {
    cy.resetErikoistuvaE2eState()
    cy.loginAsErikoistuva()
  })

  const luoTyoskentelyjaksoLiitteella = () => {
    cy.visit('/tyoskentelyjaksot/uusi')
    cy.get('.lisaa-tyoskentelyjakso').should('be.visible')
    cy.get('[data-testid="loading"]', { timeout: 10000 }).should('not.exist')
    cy.get('input[type="radio"][name="tyoskentelyjakso-tyyppi"]').first().click({ force: true })
    cy.contains('label', 'Työskentelypaikka')
      .parent()
      .find('input[type="text"]')
      .first()
      .clear()
      .type(TYOSKENTELYPAIKKA)
    cy.contains('label', 'Kunta').parent().as('kuntaGroup')
    cy.selectFirstMultiselectOption(cy.get('@kuntaGroup'))
    cy.contains('label', 'Alkamispäivä')
      .parent()
      .find('input.date-input')
      .first()
      .clear()
      .type('01.01.2025')
      .blur()
    cy.contains('label', 'Päättymispäivä')
      .parent()
      .find('input.date-input')
      .first()
      .clear()
      .type('30.06.2027')
      .blur()
    cy.get('input[type="number"]').first().clear().type('100')
    cy.get('input[type="radio"][name="kaytannon-koulutus-tyyppi"]').first().click({ force: true })

    cy.get('input[type="file"]').first().selectFile(`cypress/fixtures/${LIITETIEDOSTO}`, {
      force: true
    })

    cy.intercept('POST', '**/erikoistuva-laakari/tyoskentelyjaksot').as('tyoskentelyjaksoPost')
    cy.contains('button', 'Lisää').click()
    cy.wait('@tyoskentelyjaksoPost', { timeout: 30000 })
      .its('response.statusCode')
      .should('be.oneOf', [200, 201])
    cy.url().should('not.include', '/uusi')
  }

  it('poistaa työskentelyjakson, mutta säilyttää liitetiedoston Asiakirjat-sivulla', () => {
    // --- Vaihe 1: Luodaan jakso ja liitetiedosto ---
    luoTyoskentelyjaksoLiitteella()

    // --- Vaihe 2: Avataan jakson yksityiskohtasivu ja varmistetaan liitetiedosto ---
    cy.visit('/tyoskentelyjaksot')
    cy.contains('h1', 'Työskentelyjaksot').should('be.visible')
    cy.contains(TYOSKENTELYPAIKKA).click()
    cy.url().should('match', /\/tyoskentelyjaksot\/\d+$/)
    cy.contains('.asiakirjat-table', LIITETIEDOSTO).should('be.visible')

    // --- Vaihe 3: Poistetaan jakso vahvistusdialogin kautta ---
    cy.intercept('DELETE', '**/erikoistuva-laakari/tyoskentelyjaksot/*').as('tyoskentelyjaksoDelete')
    cy.contains('button', 'Poista jakso').should('not.be.disabled').click()
    cy.get('.modal-content').should('be.visible').contains('button', 'Poista').click()

    cy.wait('@tyoskentelyjaksoDelete', { timeout: 15000 })
      .its('response.statusCode')
      .should('eq', 204)
    cy.contains('.toast-body', 'Työskentelyjakso poistettu onnistuneesti').should('be.visible')

    // --- Vaihe 4: Jakso on poistunut listalta ---
    cy.url().should('match', /\/tyoskentelyjaksot$/)
    cy.contains('h1', 'Työskentelyjaksot').should('be.visible')
    cy.contains(TYOSKENTELYPAIKKA).should('not.exist')

    // --- Vaihe 5: Liitetiedosto säilyy, vain viittaus jaksoon on poistunut ---
    cy.visit('/asiakirjat')
    cy.contains('h1', 'Asiakirjat').should('be.visible')
    cy.contains('.asiakirjat-table', LIITETIEDOSTO).should('be.visible')
  })
})
