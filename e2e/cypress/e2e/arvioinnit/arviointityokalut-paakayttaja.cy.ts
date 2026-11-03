import { SSN_VIRKAILIJA } from '../../support/commands/credentials'

const api = '**/api/tekninen-paakayttaja/arviointityokalu'
const originalText = 'Arvioi osaamista: äiti, työ ja Åland.'
const updatedText = 'Päivitetty ohje: hyvä arviointi säilyy tallennuksen jälkeen.'

function instructionEditor() {
  return cy.get<HTMLIFrameElement>('iframe.tox-edit-area__iframe')
    .should('have.length', 1)
    .should(($frame) => {
      expect($frame[0].contentDocument?.body.isContentEditable).to.eq(true)
    })
    .then(($frame) => cy.wrap(Cypress.$($frame[0].contentDocument!.body)))
}

// ELSAINSI-85: real UI POST/PATCH requests must persist ownerless assessment tools.
// Saving an existing tool creates a new version even when no fields are changed.
describe('Arviointityökalut – tekninen pääkäyttäjä', () => {
  let toolName: string

  before(() => {
    cy.task('db:cleanupRichTextUser', 'admin')
    cy.task<string>('db:assessmentToolName').then((name) => { toolName = name })
    cy.task('db:cleanupAssessmentTool')
    cy.task<{ token: string }>('db:seedRichTextUser', 'admin', { log: false }).then(({ token }) => {
      cy.loginWithSuomifi(SSN_VIRKAILIJA, undefined, token)
      cy.location('origin', { timeout: 60000 })
        .should('eq', new URL(Cypress.config('baseUrl') as string).origin)
      cy.location('pathname').should('not.eq', '/kirjautuminen')
      cy.request('/api/kayttaja').its('body.activeAuthority').should('eq', 'ROLE_TEKNINEN_PAAKAYTTAJA')
    })
  })

  after(() => {
    cy.task('db:cleanupAssessmentTool')
    cy.task('db:cleanupRichTextUser', 'admin')
    Cypress.session.clearAllSavedSessions()
  })

  function openForEditing(text: string) {
    // Use links: these routes pass the tool ID in Vue router params, not the URL.
    cy.contains('.arviointityokalut-table a', toolName).click()
    cy.contains('a', 'Muokkaa arviointityökalua').click()
    cy.contains('h1', 'Muokkaa arviointityökalua').should('be.visible')
    cy.contains('label', 'Arviotyökalun nimi').parent().find('input').should('have.value', toolName)
    instructionEditor().should('contain.text', text)
  }

  function expectSaved(alias: `@${string}`, text: string): Cypress.Chainable<number> {
    return cy.wait(alias).then(({ response }) => {
      expect(response?.statusCode).to.eq(200)
      expect(response?.body.nimi).to.eq(toolName)
      expect(response?.body.tila).to.eq('JULKAISTU')
      expect(response?.body.ohjeteksti).to.include(text)
      expect(response?.body.id).to.be.a('number')
      return response!.body.id as number
    })
  }

  it('luo työkalun, tallentaa ilman muutoksia ja säilyttää muokatun ohjetekstin', () => {
    cy.intercept('POST', api).as('createTool')
    cy.intercept('PATCH', api).as('updateTool')

    cy.visit('/arviointityokalut/arviointityokalu/uusi')
    cy.contains('label', 'Arviotyökalun nimi').parent().find('input').type(toolName)
    instructionEditor().click().type(originalText)
    cy.contains('button', /^Tallenna$/).click()
    expectSaved('@createTool', originalText).then((originalId) => {
      cy.location('pathname').should('eq', '/arviointityokalut')
      openForEditing(originalText)

      // Reproduce saving an existing tool without changing anything.
      cy.contains('button', /^Tallenna$/).click()
      expectSaved('@updateTool', originalText).then((unchangedVersionId) => {
        expect(unchangedVersionId).not.to.eq(originalId)
        cy.location('pathname').should('eq', '/arviointityokalut')
        openForEditing(originalText)

        instructionEditor().clear().type(updatedText)
        cy.contains('button', /^Tallenna$/).click()
        expectSaved('@updateTool', updatedText).then((editedVersionId) => {
          expect(editedVersionId).not.to.eq(unchangedVersionId)
        })
        cy.location('pathname').should('eq', '/arviointityokalut')
        // Reload the list and reopen the latest version from the real API.
        cy.reload()
        openForEditing(updatedText)
      })
    })
  })
})
