import { SSN_VIRKAILIJA } from '../../support/commands/credentials'

const MAIN = 'main[role="main"]'
const SEARCH = 'input[placeholder="Hae erikoistujaa / kouluttajaa"]'
type Fixture = {
  token: string | null
  ownId: number
  foreignId: number
  ownEmail: string
  foreignEmail: string
}

describe('Käyttäjätilien yhdistämisen haun yliopistorajaus', () => {
  let fixture: Fixture

  beforeEach(() => cy.task('db:cleanupAccountMergeSearch'))

  const loginAs = (admin: boolean) =>
    cy.task<Fixture>('db:seedAccountMergeSearch', { admin }).then((result) => {
      fixture = result
      cy.loginWithSuomifi(SSN_VIRKAILIJA, undefined, result.token ?? undefined)
      cy.location('origin', { timeout: 60000 }).should(
        'eq', new URL(Cypress.config('baseUrl') as string).origin
      )
      cy.location('pathname').should('not.eq', '/kirjautuminen')
      cy.get(MAIN).should('exist')
    })

  afterEach(() => cy.task('db:cleanupAccountMergeSearch'))

  const observeSearch = (role: string) => {
    cy.intercept('GET', `**/api/${role}/erikoistujat-ja-kouluttajat*`, (request) => {
      if (request.query['nimi.contains'] === 'MergeSearch') request.alias = 'commonSearch'
      if (request.query['nimi.contains'] === 'MergeSearchForeign') request.alias = 'foreignSearch'
    })
    cy.visit('/kayttajahallinta/yhdista-kayttajatileja')
    cy.get(MAIN).find(SEARCH).type('MergeSearch')
  }

  it('virkailija löytää vain oman yliopistonsa erikoistujan', () => {
    loginAs(false).then(() => {
      observeSearch('virkailija')
      cy.wait('@commonSearch').then(({ response }) => {
        expect(response?.statusCode).to.eq(200)
        expect(response?.body.content.map((item: { kayttajaId: number }) => item.kayttajaId))
          .to.deep.eq([fixture.ownId])
      })
      cy.get(MAIN).contains('tr', fixture.ownEmail).should('be.visible')
      cy.get(MAIN).should('not.contain.text', fixture.foreignEmail)
      cy.get(MAIN).find(SEARCH).clear().type('MergeSearchForeign')
      cy.wait('@foreignSearch').then(({ response }) => {
        expect(response?.statusCode).to.eq(200)
        expect(response?.body.content).to.deep.eq([])
        expect(response?.body.page.totalElements).to.eq(0)
      })
      cy.get(MAIN).contains('Ei hakutuloksia').should('be.visible')
      cy.get(MAIN).should('not.contain.text', fixture.foreignEmail)
    })
  })

  it('pääkäyttäjä löytää molempien yliopistojen erikoistujat', () => {
    loginAs(true).then(() => {
      observeSearch('tekninen-paakayttaja')
      cy.wait('@commonSearch').then(({ response }) => {
        expect(response?.statusCode).to.eq(200)
        expect(response?.body.content.map((item: { kayttajaId: number }) => item.kayttajaId))
          .to.have.members([fixture.ownId, fixture.foreignId])
      })
      cy.get(MAIN).contains('tr', fixture.ownEmail).should('be.visible')
      cy.get(MAIN).contains('tr', fixture.foreignEmail).should('be.visible')
    })
  })
})
