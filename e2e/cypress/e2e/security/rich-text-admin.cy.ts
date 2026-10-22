import { SSN_VIRKAILIJA } from '../../support/commands/credentials'
import { assertNoExecution, assertSafeContent, assertSafeHtml, dirtyHtml, editorBody, observeXss, safeHtml } from '../../support/rich-text'

const api = '/api/tekninen-paakayttaja/ilmoitukset'

describe('Ilmoitusten API ja oikea TinyMCE – XSS-regressio', () => {
  let token: string
  let marker: string

  before(() => {
    cy.task('db:cleanupRichTextUser', 'admin')
    cy.task<{ token: string }>('db:seedRichTextUser', 'admin', { log: false }).then((fixture) => {
      token = fixture.token
    })
    cy.task<string>('db:richTextMarker').then((value) => { marker = value })
  })

  beforeEach(() => {
    cy.session(['rich-text-admin', token], () => {
      cy.loginWithSuomifi(SSN_VIRKAILIJA, undefined, token)
      cy.location('origin', { timeout: 60000 }).should('eq', new URL(Cypress.config('baseUrl') as string).origin)
      cy.location('pathname').should('not.eq', '/kirjautuminen')
      cy.request('/api/kayttaja').then(({ body }) => {
        expect(body.email).to.eq('e2e-rich-text-admin@test.elsa')
        expect(body.activeAuthority).to.eq('ROLE_TEKNINEN_PAAKAYTTAJA')
      })
    }, {
      validate: () => {
        cy.request('/api/kayttaja').its('body.activeAuthority').should('eq', 'ROLE_TEKNINEN_PAAKAYTTAJA')
      }
    })
  })

  afterEach(() => cy.task('db:cleanupRichTextAnnouncements'))
  after(() => {
    cy.task('db:cleanupRichTextAnnouncements')
    cy.task('db:cleanupRichTextUser', 'admin')
    Cypress.session.clearAllSavedSessions()
  })

  it('puhdistaa suoran POST- ja PUT-pyynnön ennen tallennusta ilman editoria', () => {
    const html = `${dirtyHtml}<p>${marker}</p>`
    cy.apiRequest({ method: 'POST', url: api, body: { teksti: html } }).then(({ status, body }) => {
      expect(status).to.eq(200)
      const id: number = body.id
      cy.task<string>('db:readRichTextAnnouncement', id).then((stored) => {
        assertSafeHtml(stored)
      })
      cy.apiRequest({ method: 'PUT', url: api, body: { id, teksti: `${html}<p>Päivitetty</p>` } })
        .its('status').should('eq', 200)
      cy.task<string>('db:readRichTextAnnouncement', id).then((stored) => {
        expect(stored).to.contain('Päivitetty')
        assertSafeHtml(stored)
      })
      cy.visit('/ilmoitukset', { onBeforeLoad: observeXss })
      cy.contains('.ilmoitukset h2', 'Hyvää & tärkeää – Åland').parent()
        .should('be.visible').should(($root) => assertSafeContent($root))
      assertNoExecution()
    })
  })

  it('avaa vanhan HTML:n turvallisesti TinyMCE:ssä ja säilyttää muotoilun tallennuksen yli', () => {
    cy.task<number>('db:createLegacyRichTextAnnouncement', `${dirtyHtml}<p>${marker}</p>`).then((id) => {
      let writes = 0
      cy.intercept({ method: '+(POST|PUT|PATCH|DELETE)', pathname: `${api}*` }, () => { writes++ }).as('write')
      // Return dirty data deliberately: the real backend would already clean the
      // GET, masking a regression in the editor's own input protection.
      cy.intercept('GET', `**/api/julkinen/ilmoitukset/${id}`, {
        id, teksti: `${dirtyHtml}<p>${marker}</p>`
      }).as('legacyEditorInput')
      cy.visit(`/ilmoitukset/${id}/muokkaa`, { onBeforeLoad: observeXss })
      cy.wait('@legacyEditorInput')
      editorBody().should(($body) => assertSafeContent($body, true))
      assertNoExecution()
      cy.then(() => expect(writes, 'opening the editor sends no writes').to.eq(0))
      cy.task('db:readRichTextAnnouncement', id).should('eq', `${dirtyHtml}<p>${marker}</p>`)

      // Exercise the real code dialog, Vue v-model, form and server persistence.
      cy.get('.tox button[aria-label="Source code"]').click()
      cy.get('.tox-dialog textarea').clear().type(
        `${safeHtml}<p>${marker}</p><p>Muokattu – Ää Öö Åå</p>`,
        { parseSpecialCharSequences: false }
      )
      cy.get('.tox-dialog').contains('button', 'Save').click()
      editorBody().should('contain.text', 'Muokattu – Ää Öö Åå')
      cy.get('.muokkaa-ilmotusta button[type="submit"]').click()
      cy.wait('@write').its('response.statusCode').should('eq', 200)
      cy.location('pathname').should('eq', '/ilmoitukset')
      cy.task<string>('db:readRichTextAnnouncement', id).then((stored) => {
        expect(stored).to.contain('Muokattu – Ää Öö Åå')
        expect(stored).to.contain('<strong>Ensimmäinen uutinen</strong>')
      })
      // Replace the dirty response stub with a passthrough to verify persisted data.
      cy.intercept('GET', `**/api/julkinen/ilmoitukset/${id}`, (request) => request.continue())
      cy.visit(`/ilmoitukset/${id}/muokkaa`, { onBeforeLoad: observeXss })
      editorBody().should(($body) => assertSafeContent($body, true))
        .should('contain.text', 'Muokattu – Ää Öö Åå')
      assertNoExecution()
      cy.then(() => expect(writes, 'only the explicit save writes to the API').to.eq(1))
    })
  })

  it('säilyttää kohdistimen suomenkielisen tekstin keskellä sekä undo- ja redo-toiminnot', () => {
    type Editor = {
      selection: { setCursorLocation: (node: Node, offset: number) => void }
      undoManager: { clear: () => void; add: () => void; undo: () => void; redo: () => void }
    }
    const withEditor = (action: (editor: Editor) => void) => cy.window().then((win) => {
      action((win as unknown as { tinymce: { activeEditor: Editor } }).tinymce.activeEditor)
    })
    const paragraph = () => editorBody().contains('p', /^Alku ä/)
    const assertCaret = (prefix: string) => paragraph().should(($p) => {
      const selection = $p[0].ownerDocument.getSelection()!
      expect(selection.isCollapsed).to.eq(true)
      expect(selection.rangeCount).to.eq(1)
      const caret = selection.getRangeAt(0)
      expect($p[0].contains(caret.startContainer)).to.eq(true)
      const before = $p[0].ownerDocument.createRange()
      before.selectNodeContents($p[0])
      before.setEnd(caret.startContainer, caret.startOffset)
      expect(before.toString()).to.eq(prefix)
    })

    cy.task<number>('db:createLegacyRichTextAnnouncement',
      `${safeHtml}<p>${marker}</p><p>Alku ä loppu</p>`).then((id) => {
      cy.visit(`/ilmoitukset/${id}/muokkaa`, { onBeforeLoad: observeXss })
      paragraph().click().then(($p) => {
        withEditor((editor) => {
          editor.selection.setCursorLocation($p[0].firstChild!, 'Alku ä'.length)
          editor.undoManager.clear()
          editor.undoManager.add()
        })
      })
      // Real contenteditable typing, not Source code or editor.setContent().
      paragraph().type('ö').should('have.text', 'Alku äö loppu')
      assertCaret('Alku äö')
      withEditor((editor) => editor.undoManager.add())
      // Do not reposition the selection between keystrokes.
      paragraph().type('å').should('have.text', 'Alku äöå loppu')
      assertCaret('Alku äöå')
      withEditor((editor) => { editor.undoManager.add(); editor.undoManager.undo() })
      paragraph().should('have.text', 'Alku äö loppu')
      withEditor((editor) => editor.undoManager.redo())
      paragraph().should('have.text', 'Alku äöå loppu')
      assertCaret('Alku äöå')
      cy.intercept('PUT', `**${api}`).as('saveTypedText')
      cy.get('.muokkaa-ilmotusta button[type="submit"]').click()
      cy.wait('@saveTypedText').its('response.statusCode').should('eq', 200)
      cy.location('pathname').should('eq', '/ilmoitukset')
      cy.visit(`/ilmoitukset/${id}/muokkaa`, { onBeforeLoad: observeXss })
      paragraph().should('have.text', 'Alku äöå loppu')
      assertNoExecution()
    })
  })
})
