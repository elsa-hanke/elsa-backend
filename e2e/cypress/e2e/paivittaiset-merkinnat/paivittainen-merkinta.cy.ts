import { E2E_ERIKOISTUVA_EMAIL } from '../../support/commands'

export {}

describe('Päivittäinen merkintä', () => {
  before(() => {
    cy.resetErikoistuvaE2eState()
  })

  beforeEach(() => {
    cy.loginAsErikoistuva()
  })

  it('Erikoistuja lisää päivittäisen merkinnän ja näkee sen listalla', () => {
    cy.visit('/paivittaiset-merkinnat')
    cy.contains('h1', 'Päivittäiset merkinnät').should('be.visible')

    cy.visit('/paivittaiset-merkinnat/uusi')
    cy.contains('h1', 'Lisää merkintä').should('be.visible')
    cy.get('[role="status"]', { timeout: 10000 }).should('not.exist')

    cy.contains('label', 'Päivämäärä')
      .parent()
      .find('input.date-input, input[type="text"]')
      .first()
      .clear()
      .type('15.05.2025')
      .blur()

    cy.get('input[name="paivakirja-merkinta-aihe"]').eq(1).check({ force: true })

    cy.contains('label', 'Oppimistapahtuma')
      .parent()
      .find('input[type="text"]')
      .clear()
      .type('E2E ohjauskeskustelu')

    cy.contains('label', 'Ajatuksia opitusta ja sen soveltamisesta')
      .parent()
      .find('textarea')
      .clear()
      .type('E2E reflektio päivittäisestä merkinnästä.')

    cy.intercept('POST', '**/erikoistuva-laakari/paivakirjamerkinnat').as('paivakirjamerkintaPost')
    cy.contains('button', 'Tallenna merkintä').click()

    cy.wait('@paivakirjamerkintaPost', { timeout: 15000 }).then(({ response }) => {
      expect(response?.statusCode).to.eq(201)
      expect(response?.body?.id).to.be.a('number')
    })

    cy.url().should('match', /\/paivittaiset-merkinnat\/\d+$/)
    cy.contains('h1', 'E2E ohjauskeskustelu').should('be.visible')
    cy.contains('E2E reflektio päivittäisestä merkinnästä.').should('be.visible')

    cy.visit('/paivittaiset-merkinnat')
    cy.contains('E2E ohjauskeskustelu').should('be.visible')
    cy.contains('E2E reflektio päivittäisestä merkinnästä.').should('be.visible')
  })

  it('Erikoistuja saa tallennettaessa tarkan virheen PDF-fontista puuttuvasta merkistä', () => {
    cy.visit('/paivittaiset-merkinnat/uusi')
    cy.contains('h1', 'Lisää merkintä').should('be.visible')
    cy.get('[role="status"]', { timeout: 10000 }).should('not.exist')

    cy.contains('label', 'Päivämäärä')
      .parent()
      .find('input.date-input, input[type="text"]')
      .first()
      .clear()
      .type('16.05.2025')
      .blur()

    cy.get('input[name="paivakirja-merkinta-aihe"]').eq(1).check({ force: true })

    cy.contains('label', 'Oppimistapahtuma')
      .parent()
      .find('input[type="text"]')
      .clear()
      .type('E2E PDF-merkkien tarkistus')

    cy.contains('label', 'Ajatuksia opitusta ja sen soveltamisesta')
      .parent()
      .find('textarea')
      .clear()
      .type('Pohdinta sisältää tarkistusmerkin ✓')

    cy.intercept('POST', '**/erikoistuva-laakari/paivakirjamerkinnat').as(
      'pdfMerkkiPaivakirjamerkintaPost'
    )
    cy.contains('button', 'Tallenna merkintä').click()

    cy.wait('@pdfMerkkiPaivakirjamerkintaPost').then(({ request, response }) => {
      expect(request.body.reflektio).to.eq('Pohdinta sisältää tarkistusmerkin ✓')
      expect(response?.statusCode).to.eq(400)
      expect(response?.body).to.include({
        message: 'error.dataillegal.pdf-tiedostossa-tukemattomia-merkkeja',
        field: 'ajatuksia-opitusta-ja-sen-soveltamisesta',
        pdfSource: 'paivakirjamerkinta',
        sourceDate: '2025-05-16',
      })
      expect(response?.body?.unsupportedCharacters).to.deep.eq(['✓ (U+2713)'])
    })

    cy.contains(
      'Kenttä "Ajatuksia opitusta ja sen soveltamisesta" sisältää merkkejä, joita ei ' +
        'voida lisätä arkistoitavaan PDF-tiedostoon: ✓ (U+2713). Poista tai korvaa ' +
        'merkit ja tallenna uudelleen.'
    ).should('be.visible')

    cy.contains('label', 'Ajatuksia opitusta ja sen soveltamisesta')
      .parent()
      .find('textarea')
      .should('have.value', 'Pohdinta sisältää tarkistusmerkin ✓')
      .clear()
      .type('Pohdinta sisältää vain tuettuja merkkejä.')

    cy.contains('button', 'Tallenna merkintä').click()
    cy.wait('@pdfMerkkiPaivakirjamerkintaPost').then(({ response }) => {
      expect(response?.statusCode).to.eq(201)
      expect(response?.body?.id).to.be.a('number')
    })

    cy.url().should('match', /\/paivittaiset-merkinnat\/\d+$/)
    cy.contains('Pohdinta sisältää vain tuettuja merkkejä.').should('be.visible')
  })
})
