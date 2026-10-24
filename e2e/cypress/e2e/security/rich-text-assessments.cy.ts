import { SSN_KOULUTTAJA } from '../../support/commands/credentials'
import { assertNoExecution, assertSafeContent, dirtyHtml, observeXss } from '../../support/rich-text'

describe('Arviointien rich text – selaimen XSS-suojaus', () => {
  let token: string
  before(() => {
    cy.task('db:cleanupRichTextUser', 'trainer')
    cy.task<{ token: string }>('db:seedRichTextUser', 'trainer', { log: false }).then((fixture) => {
      token = fixture.token
    })
  })

  beforeEach(() => {
    cy.session(['rich-text-trainer', token], () => {
      cy.loginWithSuomifi(SSN_KOULUTTAJA, undefined, token)
      cy.location('origin', { timeout: 60000 }).should('eq', new URL(Cypress.config('baseUrl') as string).origin)
      cy.location('pathname').should('not.eq', '/kirjautuminen')
      cy.request('/api/kayttaja').its('body.email').should('eq', 'e2e-rich-text-trainer@test.elsa')
    }, {
      validate: () => {
        cy.request('/api/kayttaja').its('body.activeAuthority').should('eq', 'ROLE_KOULUTTAJA')
      }
    })
  })

  after(() => {
    cy.task('db:cleanupRichTextUser', 'trainer')
    Cypress.session.clearAllSavedSessions()
  })

  it('puhdistaa kuvaus- ja kuvausSv-kentät myös kieltä vaihdettaessa', () => {
    cy.intercept('GET', '**/api/kouluttaja/erikoisalat', [{ id: 46, nimi: 'E2E erikoisala' }])
    cy.intercept({ method: 'GET', pathname: '/api/kouluttaja/arvioitavatkokonaisuudet' }, {
      content: [{ id: 987654321, nimi: 'E2E kokonaisuus FI', nimiSv: 'E2E helhet SV',
        kuvaus: `${dirtyHtml}<p>Suomenkielinen kuvaus</p>`,
        kuvausSv: `${dirtyHtml}<p>Svensk beskrivning</p>` }],
      page: { size: 20, number: 0, totalElements: 1, totalPages: 1 }
    }).as('descriptions')
    cy.visit('/arvioitavat-kokonaisuudet', { onBeforeLoad: observeXss })
    cy.wait('@descriptions')
    cy.contains('.accordian-header', 'E2E kokonaisuus FI').click()
    cy.contains('.arvioitavat-kokonaisuudet h2', 'Hyvää & tärkeää – Åland').parent()
      .should('be.visible').should(($root) => assertSafeContent($root))
      .should('contain.text', 'Suomenkielinen kuvaus')
    assertNoExecution()

    // The locale selector is hidden in the pilot. Change the existing i18n
    // instance through Vue's mounted root, without adding production test hooks.
    cy.get('#app').then(($app) => {
      const root = $app[0] as HTMLElement & { __vue__?: { $i18n: { locale: string } } }
      expect(root.__vue__, 'mounted Vue root').to.exist
      root.__vue__!.$i18n.locale = 'sv'
    })
    cy.contains('.accordian-header', 'E2E helhet SV').should('be.visible')
    cy.contains('.arvioitavat-kokonaisuudet h2', 'Hyvää & tärkeää – Åland').parent()
      .should('be.visible').should(($root) => assertSafeContent($root))
      .should('contain.text', 'Svensk beskrivning').and('not.contain.text', 'Suomenkielinen kuvaus')
    assertNoExecution()
  })

  it('puhdistaa arviointityökalun ohjetekstin oikeassa esittelynäkymässä', () => {
    cy.intercept('GET', '**/api/kouluttaja/arviointityokalut/kategoriat', [])
    cy.intercept('GET', '**/api/kouluttaja/arviointityokalut', [{
      id: 987654321, nimi: 'E2E turvallinen työkalu', ohjeteksti: dirtyHtml,
      kategoria: null, kysymykset: [], liite: null
    }]).as('instructions')
    cy.visit('/arviointityokalut/esittely', { onBeforeLoad: observeXss })
    cy.wait('@instructions')
    cy.contains('.accordian-header', 'E2E turvallinen työkalu').click()
    cy.contains('.esittely h2', 'Hyvää & tärkeää – Åland').parent()
      .should('be.visible').should(($root) => assertSafeContent($root))
    assertNoExecution()
  })
})
