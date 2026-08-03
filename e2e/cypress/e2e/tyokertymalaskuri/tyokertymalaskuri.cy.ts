export {}

describe('Työkertymälaskuri', () => {
  it('Käyttäjä lisää työskentelyjakson julkiseen työkertymälaskuriin ja muokkaa sitä', () => {
    cy.intercept('GET', '**/julkinen/poissaolon-syyt').as('poissaolonSyyt')

    cy.visit('/tyokertymalaskuri', {
      onBeforeLoad(win) {
        win.localStorage.removeItem('laskuri-tyoskentelyjaksot')
      },
    })

    cy.contains('h1', 'Työkertymälaskuri').should('be.visible')
    cy.contains('h2', 'Laskennan yhteenveto').should('be.visible')
    cy.contains('span', 'Työkertymä yhteensä')
      .parent()
      .find('.duration-text')
      .invoke('text')
      .invoke('trim')
      .should('eq', '0 vrk')

    cy.contains('button', 'Lisää työskentelyjakso').click()
    cy.wait('@poissaolonSyyt', { timeout: 15000 }).its('response.statusCode').should('eq', 200)
    cy.get('.modal-content').within(() => {
      cy.contains('h5', 'Lisää työskentelyjakso').should('be.visible')
      cy.contains('label', 'Työskentelypaikka')
        .parent()
        .find('input[type="text"]')
        .clear()
        .type('E2E Laskurisairaala')
      cy.get('input[type="radio"][name="kaytannon-koulutus-tyyppi"]').first().check({ force: true })
      cy.contains('label', 'Alkamispäivä')
        .parent()
        .find('input.date-input, input[type="text"]')
        .first()
        .clear()
        .type('01.01.2025')
        .blur()
      cy.contains('label', 'Päättymispäivä')
        .parent()
        .find('input.date-input, input[type="text"]')
        .first()
        .clear()
        .type('31.03.2025')
        .blur()
      cy.contains('label', 'Työaika (50–100 %)')
        .parent()
        .find('input[type="number"]')
        .clear()
        .type('100')
      cy.contains('button', 'Tallenna jakso').click()
    })

    cy.get('.modal-content').should('not.exist')
    cy.contains('.tyoskentelyjaksot-table', 'E2E Laskurisairaala').should('be.visible')
    cy.contains('.tyoskentelyjaksot-table', /1\.1\.2025.*31\.3\.2025/).should('be.visible')
    cy.contains('span', 'Työkertymä yhteensä')
      .parent()
      .find('.duration-text')
      .invoke('text')
      .invoke('trim')
      .should('not.eq', '0 vrk')

    cy.contains('.tyoskentelyjaksot-table button', 'E2E Laskurisairaala').click()
    cy.get('.modal-content').within(() => {
      cy.contains('h5', 'Muokkaa työskentelyjaksoa').should('be.visible')
      cy.contains('label', 'Työskentelypaikka')
        .parent()
        .find('input[type="text"]')
        .clear()
        .type('E2E Muokattu laskurisairaala')
      cy.contains('button', 'Tallenna').click()
    })

    cy.get('.modal-content').should('not.exist')
    cy.contains('.tyoskentelyjaksot-table', 'E2E Muokattu laskurisairaala').should('be.visible')

    // Summary card assertions
    cy.contains('span', 'Työkertymä yhteensä')
      .parent()
      .find('.duration-text')
      .invoke('text')
      .invoke('trim')
      .should('not.eq', '0 vrk')

    // Table row assertions
    cy.get('.tyoskentelyjaksot-table').within(() => {
      cy.contains('E2E Muokattu laskurisairaala').should('be.visible')
      cy.contains(/1\.1\.2025.*31\.3\.2025/).should('be.visible')
      cy.contains('100 %').should('be.visible')
      cy.contains('Ei poissaoloja').should('be.visible')
    })
  })
})
